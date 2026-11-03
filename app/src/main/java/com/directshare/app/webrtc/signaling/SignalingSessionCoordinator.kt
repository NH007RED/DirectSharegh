package com.directshare.app.webrtc.signaling

import com.directshare.app.domain.model.IceServerConfig
import com.directshare.app.domain.model.SessionStatus
import com.directshare.app.domain.model.SignalingSession
import com.directshare.app.domain.repository.SignalingRepository
import com.directshare.app.utils.AppLogger
import com.directshare.app.webrtc.peerconnection.WebRtcClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalingSessionCoordinator @Inject constructor(
    private val signalingRepository: SignalingRepository,
    private val webRtcClient: WebRtcClient
) {
    private var signalingJobs = mutableListOf<Job>()
    private var offerPublishedForSession: String? = null
    private var answerPublishedForSession: String? = null
    private var remoteOfferAppliedForSession: String? = null
    private var remoteAnswerAppliedForSession: String? = null

    fun startWatchingAuthorizedSession(
        scope: CoroutineScope,
        session: SignalingSession,
        isHost: Boolean,
        iceServers: List<IceServerConfig>,
        onWebRtcNegotiationStarted: () -> Unit,
        onNegotiationError: (String) -> Unit
    ) {
        stopWatching()

        val job = scope.launch {
            val initResult = webRtcClient.initializePeerConnection(
                iceServerConfigs = iceServers,
                isOffererHost = isHost
            )
            if (initResult.isFailure) {
                onNegotiationError(
                    initResult.exceptionOrNull()?.message ?: "Unable to initialize direct connection"
                )
                return@launch
            }

            onWebRtcNegotiationStarted()

            val remoteUid = if (isHost) session.guestUid else session.hostUid
            if (!remoteUid.isNullOrBlank()) {
                val iceJob = scope.launch {
                    signalingRepository.observeRemoteIceCandidates(session.sessionId, remoteUid)
                        .collect { candidate ->
                            webRtcClient.addRemoteIceCandidate(
                                sdpMid = candidate.sdpMid,
                                sdpMLineIndex = candidate.sdpMLineIndex,
                                sdp = candidate.sdp
                            )
                        }
                }
                signalingJobs.add(iceJob)
            }

            if (isHost) {
                if (offerPublishedForSession != session.sessionId) {
                    offerPublishedForSession = session.sessionId
                    val offerResult = webRtcClient.createOffer()
                    offerResult.fold(
                        onSuccess = { offerSdp ->
                            val writeRes = signalingRepository.sendOffer(session.sessionId, offerSdp)
                            if (writeRes.isFailure) {
                                onNegotiationError("Unable to send connection invite to the other device")
                            }
                        },
                        onFailure = { err ->
                            onNegotiationError(err.message ?: "Unable to prepare connection request")
                        }
                    )
                }

                val answerJob = scope.launch {
                    signalingRepository.observeAnswer(session.sessionId)
                        .filterNotNull()
                        .collect { answerPayload ->
                            if (remoteAnswerAppliedForSession != session.sessionId) {
                                remoteAnswerAppliedForSession = session.sessionId
                                val res = webRtcClient.setRemoteAnswer(answerPayload.sdp)
                                if (res.isFailure) {
                                    onNegotiationError("Unable to complete connection handshake")
                                } else {
                                    signalingRepository.updateSessionStatus(
                                        session.sessionId,
                                        SessionStatus.CONNECTED
                                    )
                                }
                            }
                        }
                }
                signalingJobs.add(answerJob)
            } else {
                val offerJob = scope.launch {
                    signalingRepository.observeOffer(session.sessionId)
                        .filterNotNull()
                        .collect { offerPayload ->
                            if (remoteOfferAppliedForSession != session.sessionId) {
                                remoteOfferAppliedForSession = session.sessionId
                                val answerResult = webRtcClient.setRemoteOfferAndCreateAnswer(offerPayload.sdp)
                                answerResult.fold(
                                    onSuccess = { answerSdp ->
                                        if (answerPublishedForSession != session.sessionId) {
                                            answerPublishedForSession = session.sessionId
                                            val writeRes = signalingRepository.sendAnswer(session.sessionId, answerSdp)
                                            if (writeRes.isFailure) {
                                                onNegotiationError("Unable to send connection response")
                                            }
                                        }
                                    },
                                    onFailure = { err ->
                                        onNegotiationError(err.message ?: "Unable to accept connection request")
                                    }
                                )
                            }
                        }
                }
                signalingJobs.add(offerJob)
            }
        }
        signalingJobs.add(job)
    }

    fun stopWatching() {
        signalingJobs.forEach { it.cancel() }
        signalingJobs.clear()
        offerPublishedForSession = null
        answerPublishedForSession = null
        remoteOfferAppliedForSession = null
        remoteAnswerAppliedForSession = null
        AppLogger.d("SignalingSessionCoordinator", "Stopped active session watchers")
    }
}
