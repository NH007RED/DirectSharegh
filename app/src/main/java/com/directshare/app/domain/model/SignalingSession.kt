package com.directshare.app.domain.model

data class SignalingSession(
    val sessionId: String,
    val pairingCode: String,
    val hostUid: String,
    val hostDeviceName: String,
    val guestUid: String? = null,
    val guestDeviceName: String? = null,
    val isAuthorized: Boolean = false,
    val status: SessionStatus = SessionStatus.WAITING_FOR_PEER,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + SESSION_TTL_MS
) {
    fun isExpired(nowMs: Long = System.currentTimeMillis()): Boolean = nowMs >= expiresAt

    companion object {
        const val SESSION_TTL_MS: Long = 10 * 60 * 1000L // 10 minutes max session lifetime
    }
}

enum class SessionStatus {
    WAITING_FOR_PEER,
    AWAITING_AUTHORIZATION,
    AUTHORIZED,
    NEGOTIATING,
    CONNECTED,
    REJECTED,
    CLOSED,
    EXPIRED
}

data class SessionSdpPayload(
    val type: String, // "offer" or "answer"
    val sdp: String,
    val senderUid: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SessionIceCandidate(
    val candidateId: String,
    val sdpMid: String,
    val sdpMLineIndex: Int,
    val sdp: String,
    val senderUid: String,
    val timestamp: Long = System.currentTimeMillis()
)
