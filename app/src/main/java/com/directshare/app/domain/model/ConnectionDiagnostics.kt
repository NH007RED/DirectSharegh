package com.directshare.app.domain.model

data class ConnectionDiagnostics(
    val firebaseAuthenticated: Boolean = false,
    val firebaseUid: String? = null,
    val signalingConnected: Boolean = false,
    val activeSessionId: String? = null,
    val iceConnectionState: String = "NEW",
    val peerConnectionState: String = "NEW",
    val dataChannelState: String = "CLOSED",
    val localCandidatesCount: Int = 0,
    val remoteCandidatesCount: Int = 0,
    val selectedCandidatePairType: String = "N/A",
    val turnConfigured: Boolean = false,
    val lastError: String? = null
)
