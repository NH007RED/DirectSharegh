package com.directshare.app.webrtc.transfer

import com.directshare.app.domain.model.AppSettings
import com.directshare.app.domain.model.FileTransferItem
import com.directshare.app.domain.model.TransferProgress
import com.directshare.app.utils.AppLogger
import com.directshare.app.utils.HashUtils
import com.directshare.app.webrtc.datachannel.DataChannelTransport
import com.directshare.app.webrtc.datachannel.PacketProtocol
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Streams multiple queued files sequentially over an open DataChannelTransport using configurable
 * chunk sizes, incremental InputStream reading, SHA-256 integrity calculation, and
 * RTCDataChannel.bufferedAmount backpressure control.
 */
class FileChunkSender(
    private val transport: DataChannelTransport,
    private val inputStreamProvider: (FileTransferItem) -> InputStream?
) {
    private val pendingFileVerificationAcks = ConcurrentHashMap<String, CompletableDeferred<PacketProtocol.ControlMessage.FileVerifiedAck>>()

    fun onFileVerifiedAckReceived(ack: PacketProtocol.ControlMessage.FileVerifiedAck) {
        pendingFileVerificationAcks.remove(ack.fileId)?.complete(ack)
    }

    suspend fun sendBatch(
        batchId: String,
        filesWithChecksums: List<FileTransferItem>,
        chunkSizeBytes: Int = AppSettings.DEFAULT_CHUNK_SIZE_BYTES,
        highWaterMarkBytes: Long = PacketProtocol.HIGH_WATER_MARK_BYTES,
        lowWaterMarkBytes: Long = PacketProtocol.LOW_WATER_MARK_BYTES,
        onProgress: (TransferProgress) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val safeChunkSize = chunkSizeBytes.coerceIn(
            AppSettings.MIN_CHUNK_SIZE_BYTES,
            AppSettings.MAX_CHUNK_SIZE_BYTES
        )
        val overallTotalBytes = filesWithChecksums.sumOf { it.sizeBytes }
        var overallBytesSent = 0L
        val startTimeMs = System.currentTimeMillis()

        try {
            for ((index, item) in filesWithChecksums.withIndex()) {
                currentCoroutineContext().ensureActive()
                if (!transport.isOpen()) {
                    return@withContext Result.failure(IllegalStateException("WebRTC DataChannel closed during transfer"))
                }

                val expectedSha256 = item.sha256Checksum
                    ?: return@withContext Result.failure(IllegalStateException("Missing SHA-256 digest for ${item.name}"))

                val ackDeferred = CompletableDeferred<PacketProtocol.ControlMessage.FileVerifiedAck>()
                pendingFileVerificationAcks[item.fileId] = ackDeferred

                // 1. Send FILE_START control message
                val startFrame = PacketProtocol.encodeControlMessage(
                    PacketProtocol.ControlMessage.FileStart(
                        batchId = batchId,
                        fileId = item.fileId,
                        fileIndex = index,
                        totalFiles = filesWithChecksums.size,
                        fileName = item.name,
                        mimeType = item.mimeType,
                        sizeBytes = item.sizeBytes,
                        sha256Checksum = expectedSha256
                    )
                )
                if (!transport.sendFrame(startFrame, isBinary = true)) {
                    return@withContext Result.failure(IllegalStateException("Failed to send FILE_START frame for ${item.name}"))
                }

                var fileBytesSent = 0L
                var chunkIndex = 0L
                val streamDigest = HashUtils.createSha256Digest()

                if (item.sizeBytes == 0L) {
                    // Explicitly handle 0-byte empty files with a single 0-length chunk frame
                    val emptyChunk = PacketProtocol.encodeFileChunk(
                        fileId = item.fileId,
                        chunkIndex = 0L,
                        byteOffset = 0L,
                        payload = ByteArray(0),
                        payloadLength = 0
                    )
                    if (!transport.sendFrame(emptyChunk, isBinary = true)) {
                        return@withContext Result.failure(IllegalStateException("Failed to send empty file frame"))
                    }
                    chunkIndex = 1L
                } else {
                    val stream = inputStreamProvider(item)
                        ?: return@withContext Result.failure(SecurityException("Cannot open InputStream for ${item.name}"))

                    stream.use { input ->
                        val buffer = ByteArray(safeChunkSize)
                        while (true) {
                            currentCoroutineContext().ensureActive()

                            // Respect RTCDataChannel.bufferedAmount backpressure
                            var isPausedForBackpressure = false
                            while (transport.bufferedAmount() > highWaterMarkBytes) {
                                currentCoroutineContext().ensureActive()
                                if (!transport.isOpen()) {
                                    return@withContext Result.failure(
                                        IllegalStateException("DataChannel closed while waiting for buffer drain")
                                    )
                                }
                                if (!isPausedForBackpressure) {
                                    isPausedForBackpressure = true
                                    emitProgress(
                                        batchId = batchId,
                                        fileIndex = index,
                                        totalFiles = filesWithChecksums.size,
                                        fileName = item.name,
                                        fileBytesSent = fileBytesSent,
                                        fileTotalBytes = item.sizeBytes,
                                        overallBytesSent = overallBytesSent,
                                        overallTotalBytes = overallTotalBytes,
                                        startTimeMs = startTimeMs,
                                        isPaused = true,
                                        onProgress = onProgress
                                    )
                                }
                                // Wait until bufferedAmount drops to or below lowWaterMarkBytes
                                while (transport.bufferedAmount() > lowWaterMarkBytes && transport.isOpen()) {
                                    currentCoroutineContext().ensureActive()
                                    delay(15)
                                }
                                break
                            }

                            val bytesRead = readChunkFullyOrPartialFinal(input, buffer)
                            if (bytesRead <= 0) break

                            streamDigest.update(buffer, 0, bytesRead)
                            val chunkFrame = PacketProtocol.encodeFileChunk(
                                fileId = item.fileId,
                                chunkIndex = chunkIndex,
                                byteOffset = fileBytesSent,
                                payload = buffer,
                                payloadLength = bytesRead
                            )

                            if (!transport.sendFrame(chunkFrame, isBinary = true)) {
                                return@withContext Result.failure(
                                    IllegalStateException("Failed to transmit chunk #$chunkIndex for ${item.name}")
                                )
                            }

                            fileBytesSent += bytesRead
                            overallBytesSent += bytesRead
                            chunkIndex++

                            emitProgress(
                                batchId = batchId,
                                fileIndex = index,
                                totalFiles = filesWithChecksums.size,
                                fileName = item.name,
                                fileBytesSent = fileBytesSent,
                                fileTotalBytes = item.sizeBytes,
                                overallBytesSent = overallBytesSent,
                                overallTotalBytes = overallTotalBytes,
                                startTimeMs = startTimeMs,
                                isPaused = false,
                                onProgress = onProgress
                            )
                        }
                    }
                }

                val computedStreamHash = HashUtils.bytesToHex(streamDigest.digest())
                if (fileBytesSent != item.sizeBytes) {
                    return@withContext Result.failure(
                        IllegalStateException("File size changed during reading for ${item.name}: expected ${item.sizeBytes}, read $fileBytesSent")
                    )
                }

                // 2. Send FILE_END control frame
                val endFrame = PacketProtocol.encodeControlMessage(
                    PacketProtocol.ControlMessage.FileEnd(
                        batchId = batchId,
                        fileId = item.fileId,
                        totalChunks = chunkIndex,
                        totalBytes = fileBytesSent,
                        sha256Checksum = computedStreamHash
                    )
                )
                if (!transport.sendFrame(endFrame, isBinary = true)) {
                    return@withContext Result.failure(IllegalStateException("Failed to send FILE_END for ${item.name}"))
                }

                // 3. Wait for receiver's FILE_VERIFIED_ACK before starting next file in queue
                val ack = withTimeoutOrNull(60_000L) {
                    ackDeferred.await()
                } ?: return@withContext Result.failure(
                    IllegalStateException("Timed out waiting for receiver integrity verification of ${item.name}")
                )

                if (!ack.verified) {
                    return@withContext Result.failure(
                        IllegalStateException("Receiver reported integrity verification failure for ${item.name}: ${ack.errorMessage}")
                    )
                }
            }

            // 4. Send BATCH_COMPLETE
            val batchCompleteFrame = PacketProtocol.encodeControlMessage(
                PacketProtocol.ControlMessage.BatchComplete(
                    batchId = batchId,
                    totalFiles = filesWithChecksums.size,
                    totalBytes = overallBytesSent
                )
            )
            transport.sendFrame(batchCompleteFrame, isBinary = true)
            Result.success(Unit)
        } catch (ce: CancellationException) {
            pendingFileVerificationAcks.clear()
            throw ce
        } catch (e: Exception) {
            pendingFileVerificationAcks.clear()
            AppLogger.e("FileChunkSender", "Error during batch send", e)
            Result.failure(e)
        }
    }

    /**
     * Reads up to buffer.size bytes from InputStream, handling short reads on underlying streams
     * while returning the exact partial byte count on the final chunk before EOF.
     */
    private fun readChunkFullyOrPartialFinal(input: InputStream, buffer: ByteArray): Int {
        var totalRead = 0
        while (totalRead < buffer.size) {
            val read = input.read(buffer, totalRead, buffer.size - totalRead)
            if (read == -1) break
            totalRead += read
        }
        return totalRead
    }

    private fun emitProgress(
        batchId: String,
        fileIndex: Int,
        totalFiles: Int,
        fileName: String,
        fileBytesSent: Long,
        fileTotalBytes: Long,
        overallBytesSent: Long,
        overallTotalBytes: Long,
        startTimeMs: Long,
        isPaused: Boolean,
        onProgress: (TransferProgress) -> Unit
    ) {
        val elapsedMs = (System.currentTimeMillis() - startTimeMs).coerceAtLeast(1L)
        val speedBps = if (elapsedMs >= 100L) (overallBytesSent * 1000L) / elapsedMs else 0L
        val remainingBytes = (overallTotalBytes - overallBytesSent).coerceAtLeast(0L)
        val etaSeconds = if (speedBps > 0L) remainingBytes / speedBps else null

        onProgress(
            TransferProgress(
                batchId = batchId,
                currentFileIndex = fileIndex,
                totalFiles = totalFiles,
                currentFileName = fileName,
                currentFileBytesTransferred = fileBytesSent,
                currentFileTotalBytes = fileTotalBytes,
                overallBytesTransferred = overallBytesSent,
                overallTotalBytes = overallTotalBytes,
                bytesPerSecond = speedBps,
                estimatedRemainingSeconds = etaSeconds,
                bufferedAmountBytes = transport.bufferedAmount(),
                isPausedForBackpressure = isPaused
            )
        )
    }
}
