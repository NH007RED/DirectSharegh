package com.directshare.app.domain.model

import android.net.Uri

data class FileTransferItem(
    val fileId: String,
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256Checksum: String? = null
)

data class IncomingTransferOffer(
    val batchId: String,
    val senderUid: String,
    val senderDeviceName: String,
    val files: List<IncomingFileDescriptor>,
    val totalBytes: Long,
    val timestamp: Long = System.currentTimeMillis()
)

data class IncomingFileDescriptor(
    val fileId: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256Checksum: String
)

data class CompletedReceivedFile(
    val fileId: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val savedUri: Uri,
    val sha256Verified: Boolean,
    val sha256Digest: String
)
