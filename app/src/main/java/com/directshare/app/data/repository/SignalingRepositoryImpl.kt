package com.directshare.app.data.repository

import com.directshare.app.data.firebase.FirebaseAuthManager
import com.directshare.app.data.firebase.FirebaseSignalingService
import com.directshare.app.domain.model.PeerDevice
import com.directshare.app.domain.model.SessionIceCandidate
import com.directshare.app.domain.model.SessionSdpPayload
import com.directshare.app.domain.model.SessionStatus
import com.directshare.app.domain.model.SignalingSession
import com.directshare.app.domain.repository.SignalingRepository
import kotlinx.coroutines.flow.Flow

class SignalingRepositoryImpl(
    private val signalingService: FirebaseSignalingService,
    private val authManager: FirebaseAuthManager
) : SignalingRepository {

    override suspend fun ensureAuthenticated(): Result<String> = authManager.ensureSignedIn()

    override fun getCurrentUid(): String? = authManager.getCurrentUid()

    override fun observeFirebaseConnectionState(): Flow<Boolean> =
        signalingService.observeConnectionState()

    override suspend fun publishOnlinePresence(
        deviceName: String,
        activeSessionId: String?
    ): Result<Unit> = signalingService.publishOnlinePresence(deviceName, activeSessionId)

    override suspend fun removeOnlinePresence(): Result<Unit> =
        signalingService.removeOnlinePresence()

    override fun observeOnlinePeers(): Flow<List<PeerDevice>> =
        signalingService.observeOnlinePeers()

    override suspend fun createHostingSession(deviceName: String): Result<SignalingSession> =
        signalingService.createHostingSession(deviceName)

    override suspend fun joinSessionByPairingCode(
        pairingCode: String,
        guestDeviceName: String
    ): Result<SignalingSession> =
        signalingService.joinSessionByPairingCode(pairingCode, guestDeviceName)

    override suspend fun joinSessionById(
        sessionId: String,
        guestDeviceName: String
    ): Result<SignalingSession> =
        signalingService.joinSessionById(sessionId, guestDeviceName)

    override suspend fun authorizeGuest(sessionId: String, authorized: Boolean): Result<Unit> =
        signalingService.authorizeGuest(sessionId, authorized)

    override suspend fun updateSessionStatus(
        sessionId: String,
        status: SessionStatus
    ): Result<Unit> = signalingService.updateSessionStatus(sessionId, status)

    override fun observeSession(sessionId: String): Flow<SignalingSession?> =
        signalingService.observeSession(sessionId)

    override suspend fun sendOffer(sessionId: String, sdp: String): Result<Unit> =
        signalingService.sendOffer(sessionId, sdp)

    override suspend fun sendAnswer(sessionId: String, sdp: String): Result<Unit> =
        signalingService.sendAnswer(sessionId, sdp)

    override fun observeOffer(sessionId: String): Flow<SessionSdpPayload?> =
        signalingService.observeOffer(sessionId)

    override fun observeAnswer(sessionId: String): Flow<SessionSdpPayload?> =
        signalingService.observeAnswer(sessionId)

    override suspend fun sendIceCandidate(
        sessionId: String,
        sdpMid: String,
        sdpMLineIndex: Int,
        sdp: String
    ): Result<Unit> = signalingService.sendIceCandidate(sessionId, sdpMid, sdpMLineIndex, sdp)

    override fun observeRemoteIceCandidates(
        sessionId: String,
        remoteUid: String
    ): Flow<SessionIceCandidate> =
        signalingService.observeRemoteIceCandidates(sessionId, remoteUid)

    override suspend fun cleanupSession(sessionId: String, pairingCode: String?): Result<Unit> =
        signalingService.cleanupSession(sessionId, pairingCode)

    override suspend fun disconnectAllSessions(): Result<Unit> =
        signalingService.disconnectAllSessions()
}
