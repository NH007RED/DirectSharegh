package com.directshare.app.domain.model

data class PeerDevice(
    val uid: String,
    val deviceName: String,
    val status: PeerPresenceStatus,
    val lastSeenTimestamp: Long,
    val activeSessionId: String? = null
)

enum class PeerPresenceStatus {
    ONLINE_AVAILABLE,
    IN_SESSION,
    OFFLINE
}
