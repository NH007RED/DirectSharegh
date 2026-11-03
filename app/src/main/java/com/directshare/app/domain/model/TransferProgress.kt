package com.directshare.app.domain.model

data class TransferProgress(
    val batchId: String,
    val currentFileIndex: Int,
    val totalFiles: Int,
    val currentFileName: String,
    val currentFileBytesTransferred: Long,
    val currentFileTotalBytes: Long,
    val overallBytesTransferred: Long,
    val overallTotalBytes: Long,
    val bytesPerSecond: Long,
    val estimatedRemainingSeconds: Long?,
    val bufferedAmountBytes: Long = 0L,
    val isPausedForBackpressure: Boolean = false
) {
    val overallFraction: Float
        get() = if (overallTotalBytes <= 0L) 1f else (overallBytesTransferred.toFloat() / overallTotalBytes.toFloat()).coerceIn(0f, 1f)

    val currentFileFraction: Float
        get() = if (currentFileTotalBytes <= 0L) 1f else (currentFileBytesTransferred.toFloat() / currentFileTotalBytes.toFloat()).coerceIn(0f, 1f)

    val overallPercentage: Int
        get() = (overallFraction * 100f).toInt().coerceIn(0, 100)
}
