package com.directshare.app.domain.model

data class TransferHistoryRecord(
    val id: Long = 0L,
    val fileName: String,
    val fileSizeBytes: Long,
    val mimeType: String,
    val direction: TransferDirection,
    val timestamp: Long,
    val status: TransferRecordStatus,
    val peerDisplayName: String,
    val sha256Checksum: String?,
    val savedFileUri: String? = null
)

enum class TransferDirection {
    SENT,
    RECEIVED
}

enum class TransferRecordStatus {
    COMPLETED,
    FAILED,
    CANCELLED,
    REJECTED
}

data class TransferStatisticsSummary(
    val totalSentCount: Int = 0,
    val totalReceivedCount: Int = 0,
    val totalBytesSent: Long = 0L,
    val totalBytesReceived: Long = 0L,
    val successfulTransfersCount: Int = 0,
    val failedTransfersCount: Int = 0
)
