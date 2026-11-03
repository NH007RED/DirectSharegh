package com.directshare.app.domain.model

sealed interface TransferState {
    data object Idle : TransferState

    data class FindingDevices(
        val onlinePeers: List<PeerDevice>,
        val hostedSession: SignalingSession? = null
    ) : TransferState

    data class Connecting(
        val sessionId: String,
        val peerDeviceName: String,
        val stepDescription: String
    ) : TransferState

    data class WaitingForAuthorization(
        val sessionId: String,
        val guestUid: String,
        val guestDeviceName: String,
        val isHost: Boolean
    ) : TransferState

    data class PeerConnectedReady(
        val sessionId: String,
        val peerUid: String,
        val peerDeviceName: String
    ) : TransferState

    data class WaitingForReceiverApproval(
        val batchId: String,
        val peerDeviceName: String,
        val fileCount: Int,
        val totalBytes: Long
    ) : TransferState

    data class IncomingTransferPrompt(
        val offer: IncomingTransferOffer
    ) : TransferState

    data class Sending(
        val peerDeviceName: String,
        val progress: TransferProgress
    ) : TransferState

    data class Receiving(
        val peerDeviceName: String,
        val progress: TransferProgress
    ) : TransferState

    data class Completed(
        val isSender: Boolean,
        val peerDeviceName: String,
        val totalFiles: Int,
        val totalBytes: Long,
        val durationMs: Long,
        val averageSpeedBytesPerSec: Long,
        val receivedFiles: List<CompletedReceivedFile> = emptyList()
    ) : TransferState

    data class Failed(
        val reason: TransferFailureReason,
        val detailedMessage: String,
        val peerDeviceName: String? = null
    ) : TransferState

    data class Cancelled(
        val cancelledByLocalUser: Boolean,
        val peerDeviceName: String? = null
    ) : TransferState
}

enum class TransferFailureReason(val displayTitle: String) {
    FIREBASE_DISCONNECTED("Network Disconnected"),
    AUTHENTICATION_FAILED("Connection Service Unavailable"),
    INVALID_PAIRING_CODE("Invalid or Expired Pairing Code"),
    UNAUTHORIZED_PEER("Connection Declined"),
    WEBRTC_NEGOTIATION_FAILED("Direct Link Setup Failed"),
    STUN_TURN_CONNECTIVITY_FAILED("Network Restriction Detected"),
    RECEIVER_REJECTED("Receiver Declined Transfer"),
    RECEIVER_UNAVAILABLE("Receiver Disconnected"),
    FILE_PERMISSION_ERROR("File Access Permission Denied"),
    INSUFFICIENT_STORAGE("Insufficient Device Storage"),
    CHECKSUM_MISMATCH("File Verification Failed"),
    CONNECTION_LOST("Connection Interrupted"),
    UNKNOWN_ERROR("Unexpected Transfer Error")
}
