package com.directshare.app.domain.repository

import com.directshare.app.domain.model.PeerDevice
import com.directshare.app.domain.model.SessionIceCandidate
import com.directshare.app.domain.model.SessionSdpPayload
import com.directshare.app.domain.model.SessionStatus
import com.directshare.app.domain.model.SignalingSession
import kotlinx.coroutines.flow.Flow

interface SignalingRepository {
    suspend fun ensureAuthenticated(): Result<String>
    fun getCurrentUid(): String?
    fun observeFirebaseConnectionState(): Flow<Boolean>

    suspend fun publishOnlinePresence(deviceName: String, activeSessionId: String? = null): Result<Unit>
    suspend fun removeOnlinePresence(): Result<Unit>
    fun observeOnlinePeers(): Flow<List<PeerDevice>>

    suspend fun createHostingSession(deviceName: String): Result<SignalingSession>
    suspend fun joinSessionByPairingCode(pairingCode: String, guestDeviceName: String): Result<SignalingSession>
    suspend fun joinSessionById(sessionId: String, guestDeviceName: String): Result<SignalingSession>

    suspend fun authorizeGuest(sessionId: String, authorized: Boolean): Result<Unit>
    suspend fun updateSessionStatus(sessionId: String, status: SessionStatus): Result<Unit>
    fun observeSession(sessionId: String): Flow<SignalingSession?>

    suspend fun sendOffer(sessionId: String, sdp: String): Result<Unit>
    suspend fun sendAnswer(sessionId: String, sdp: String): Result<Unit>
    fun observeOffer(sessionId: String): Flow<SessionSdpPayload?>
    fun observeAnswer(sessionId: String): Flow<SessionSdpPayload?>

    suspend fun sendIceCandidate(
        sessionId: String,
        sdpMid: String,
        sdpMLineIndex: Int,
        sdp: String
    ): Result<Unit>

    fun observeRemoteIceCandidates(
        sessionId: String,
        remoteUid: String
    ): Flow<SessionIceCandidate>

    suspend fun cleanupSession(sessionId: String, pairingCode: String? = null): Result<Unit>
    suspend fun disconnectAllSessions(): Result<Unit>
}
