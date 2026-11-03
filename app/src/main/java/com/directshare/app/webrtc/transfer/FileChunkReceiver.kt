package com.directshare.app.webrtc.transfer

import android.net.Uri
import com.directshare.app.domain.model.CompletedReceivedFile
import com.directshare.app.domain.model.TransferProgress
import com.directshare.app.utils.AppLogger
import com.directshare.app.utils.HashUtils
import com.directshare.app.webrtc.datachannel.DataChannelTransport
import com.directshare.app.webrtc.datachannel.PacketProtocol
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Receives binary file chunks sequentially per fileId, writes incrementally to a temporary file
 * off the main thread, computes a running SHA-256 digest, verifies both exact byte size and
 * SHA-256 hash upon FILE_END, sends FILE_VERIFIED_ACK, and cleans up incomplete files on cancel/error.
 */
class FileChunkReceiver(
    private val transport: DataChannelTransport,
    private val tempFileCreator: (String) -> File,
    private val finalizeFileCallback: suspend (File, String, String) -> Result<Uri>,
    private val onProgress: (TransferProgress) -> Unit
) {
    private class ActiveReceivingFile(
        val batchId: String,
        val fileId: String,
        val fileIndex: Int,
        val totalFiles: Int,
        val fileName: String,
        val mimeType: String,
        val expectedSizeBytes: Long,
        val expectedSha256: String,
        val tempFile: File,
        val outputStream: BufferedOutputStream,
        val digest: MessageDigest = HashUtils.createSha256Digest(),
        var bytesReceived: Long = 0L,
        var chunksReceived: Long = 0L
    )

    private var currentBatchId: String = ""
    private var overallTotalBytes: Long = 0L
    private var overallBytesReceived: Long = 0L
    private var batchStartTimeMs: Long = 0L
    private var activeFile: ActiveReceivingFile? = null
    private val completedFiles = mutableListOf<CompletedReceivedFile>()

    fun initializeBatch(batchId: String, totalBytes: Long) {
        cleanupIncomplete()
        currentBatchId = batchId
        overallTotalBytes = totalBytes
        overallBytesReceived = 0L
        batchStartTimeMs = System.currentTimeMillis()
        completedFiles.clear()
    }

    fun onFileStart(msg: PacketProtocol.ControlMessage.FileStart): Result<Unit> {
        return try {
            closeAndDeleteActiveTempFile()
            val tempFile = tempFileCreator(msg.fileId)
            val bos = BufferedOutputStream(FileOutputStream(tempFile), 64 * 1024)
            activeFile = ActiveReceivingFile(
                batchId = msg.batchId,
                fileId = msg.fileId,
                fileIndex = msg.fileIndex,
                totalFiles = msg.totalFiles,
                fileName = msg.fileName,
                mimeType = msg.mimeType,
                expectedSizeBytes = msg.sizeBytes,
                expectedSha256 = msg.sha256Checksum,
                tempFile = tempFile,
                outputStream = bos
            )
            emitCurrentProgress()
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e("FileChunkReceiver", "Failed to open temp file for ${msg.fileName}", e)
            Result.failure(e)
        }
    }

    fun onChunkReceived(chunk: PacketProtocol.FileChunkFrame): Result<Unit> {
        val current = activeFile
            ?: return Result.failure(IllegalStateException("Received chunk for ${chunk.fileId} with no active FILE_START"))

        if (current.fileId != chunk.fileId) {
            return Result.failure(
                IllegalStateException("Chunk fileId mismatch: expected ${current.fileId}, got ${chunk.fileId}")
            )
        }
        if (chunk.chunkIndex != current.chunksReceived) {
            return Result.failure(
                IllegalStateException("Out-of-order chunk index: expected ${current.chunksReceived}, got ${chunk.chunkIndex}")
            )
        }
        if (chunk.byteOffset != current.bytesReceived) {
            return Result.failure(
                IllegalStateException("Chunk byteOffset mismatch: expected ${current.bytesReceived}, got ${chunk.byteOffset}")
            )
        }

        return try {
            if (chunk.payload.isNotEmpty()) {
                current.outputStream.write(chunk.payload)
                current.digest.update(chunk.payload)
                current.bytesReceived += chunk.payload.size
                overallBytesReceived += chunk.payload.size
            }
            current.chunksReceived++
            emitCurrentProgress()
            Result.success(Unit)
        } catch (e: Exception) {
            closeAndDeleteActiveTempFile()
            AppLogger.e("FileChunkReceiver", "Disk write failed while receiving ${current.fileName}", e)
            Result.failure(e)
        }
    }

    suspend fun onFileEnd(msg: PacketProtocol.ControlMessage.FileEnd): Result<CompletedReceivedFile> {
        val current = activeFile
            ?: return Result.failure(IllegalStateException("Received FILE_END with no active receiving file"))

        if (current.fileId != msg.fileId) {
            closeAndDeleteActiveTempFile()
            return Result.failure(IllegalStateException("FILE_END fileId mismatch"))
        }

        return try {
            current.outputStream.flush()
            current.outputStream.close()

            val actualHash = HashUtils.bytesToHex(current.digest.digest())
            val sizeMatches = (current.bytesReceived == current.expectedSizeBytes) &&
                (current.bytesReceived == msg.totalBytes) &&
                (current.tempFile.length() == current.expectedSizeBytes)
            val hashMatches = actualHash.equals(current.expectedSha256, ignoreCase = true) &&
                actualHash.equals(msg.sha256Checksum, ignoreCase = true)

            if (!sizeMatches || !hashMatches) {
                val errMsg = "Integrity check failed for ${current.fileName} (sizeMatches=$sizeMatches, hashMatches=$hashMatches)"
                current.tempFile.delete()
                activeFile = null
                sendAck(current.batchId, current.fileId, verified = false, errorMessage = errMsg)
                return Result.failure(SecurityException(errMsg))
            }

            val savedUri = finalizeFileCallback(current.tempFile, current.fileName, current.mimeType)
                .getOrElse { err ->
                    current.tempFile.delete()
                    activeFile = null
                    sendAck(current.batchId, current.fileId, verified = false, errorMessage = err.message ?: "Save failed")
                    return Result.failure(err)
                }

            val completed = CompletedReceivedFile(
                fileId = current.fileId,
                name = current.fileName,
                mimeType = current.mimeType,
                sizeBytes = current.bytesReceived,
                savedUri = savedUri,
                sha256Verified = true,
                sha256Digest = actualHash
            )
            completedFiles.add(completed)
            activeFile = null

            sendAck(current.batchId, current.fileId, verified = true)
            Result.success(completed)
        } catch (e: Exception) {
            closeAndDeleteActiveTempFile()
            Result.failure(e)
        }
    }

    fun getCompletedFiles(): List<CompletedReceivedFile> = completedFiles.toList()

    fun cleanupIncomplete() {
        closeAndDeleteActiveTempFile()
    }

    private fun closeAndDeleteActiveTempFile() {
        val current = activeFile ?: return
        try {
            current.outputStream.close()
        } catch (_: Exception) {
        }
        try {
            if (current.tempFile.exists()) {
                current.tempFile.delete()
            }
        } catch (_: Exception) {
        }
        activeFile = null
    }

    private fun sendAck(batchId: String, fileId: String, verified: Boolean, errorMessage: String = "") {
        val ackFrame = PacketProtocol.encodeControlMessage(
            PacketProtocol.ControlMessage.FileVerifiedAck(
                batchId = batchId,
                fileId = fileId,
                verified = verified,
                errorMessage = errorMessage
            )
        )
        transport.sendFrame(ackFrame, isBinary = true)
    }

    private fun emitCurrentProgress() {
        val current = activeFile ?: return
        val elapsedMs = (System.currentTimeMillis() - batchStartTimeMs).coerceAtLeast(1L)
        val speedBps = if (elapsedMs >= 100L) (overallBytesReceived * 1000L) / elapsedMs else 0L
        val remainingBytes = (overallTotalBytes - overallBytesReceived).coerceAtLeast(0L)
        val etaSeconds = if (speedBps > 0L) remainingBytes / speedBps else null

        onProgress(
            TransferProgress(
                batchId = current.batchId,
                currentFileIndex = current.fileIndex,
                totalFiles = current.totalFiles,
                currentFileName = current.fileName,
                currentFileBytesTransferred = current.bytesReceived,
                currentFileTotalBytes = current.expectedSizeBytes,
                overallBytesTransferred = overallBytesReceived,
                overallTotalBytes = overallTotalBytes,
                bytesPerSecond = speedBps,
                estimatedRemainingSeconds = etaSeconds
            )
        )
    }
}
