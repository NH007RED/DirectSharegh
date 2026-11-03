package com.directshare.app.webrtc.transfer

import android.net.Uri
import com.directshare.app.domain.model.FileTransferItem
import com.directshare.app.domain.model.TransferProgress
import com.directshare.app.utils.HashUtils
import com.directshare.app.webrtc.datachannel.DataChannelTransport
import com.directshare.app.webrtc.datachannel.PacketProtocol
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class FileChunkStreamingTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    /**
     * In-memory loopback WebRTC DataChannelTransport that routes frames directly between
     * FileChunkSender and FileChunkReceiver and simulates bufferedAmount backpressure.
     */
    private class LoopbackTransport(
        private val onFrameSent: suspend (ByteArray) -> Unit
    ) : DataChannelTransport {
        val open = AtomicBoolean(true)
        val simulatedBufferedBytes = AtomicLong(0L)
        private val scope = CoroutineScope(Dispatchers.Unconfined)

        override fun isOpen(): Boolean = open.get()
        override fun bufferedAmount(): Long = simulatedBufferedBytes.get()

        override fun sendFrame(bytes: ByteArray, isBinary: Boolean): Boolean {
            if (!open.get()) return false
            scope.launch {
                onFrameSent(bytes)
            }
            return true
        }
    }

    @Test
    fun `streams multi-file queue including empty file and unaligned large file without mixing chunks`() = runBlocking {
        val file1Bytes = ByteArray(0) // 0-byte empty file
        val file2Bytes = ByteArray(64 * 1024) { (it and 0xFF).toByte() } // Exact 1-chunk boundary (64 KB)
        val file3Bytes = ByteArray(215_917) { ((it * 13) and 0xFF).toByte() } // 3 full chunks + 19,309-byte partial tail

        val payloads = mapOf(
            "file-empty" to file1Bytes,
            "file-exact" to file2Bytes,
            "file-multi" to file3Bytes
        )

        val savedFiles = mutableMapOf<String, ByteArray>()
        lateinit var sender: FileChunkSender
        lateinit var receiver: FileChunkReceiver

        val senderToReceiverTransport = LoopbackTransport { rawFrame ->
            when (val decoded = PacketProtocol.decodeFrame(rawFrame)) {
                is PacketProtocol.DecodedFrame.Control -> {
                    when (val msg = decoded.message) {
                        is PacketProtocol.ControlMessage.FileStart -> receiver.onFileStart(msg)
                        is PacketProtocol.ControlMessage.FileEnd -> receiver.onFileEnd(msg)
                        else -> Unit
                    }
                }
                is PacketProtocol.DecodedFrame.Chunk -> {
                    receiver.onChunkReceived(decoded.chunkFrame)
                }
            }
        }

        val receiverToSenderTransport = LoopbackTransport { rawFrame ->
            val decoded = PacketProtocol.decodeFrame(rawFrame)
            if (decoded is PacketProtocol.DecodedFrame.Control &&
                decoded.message is PacketProtocol.ControlMessage.FileVerifiedAck
            ) {
                sender.onFileVerifiedAckReceived(decoded.message)
            }
        }

        receiver = FileChunkReceiver(
            transport = receiverToSenderTransport,
            tempFileCreator = { fileId -> File(tempFolder.root, "temp_$fileId.tmp") },
            finalizeFileCallback = { tempFile, displayName, _ ->
                savedFiles[displayName] = tempFile.readBytes()
                tempFile.delete()
                Result.success(Uri.EMPTY)
            },
            onProgress = {}
        )

        sender = FileChunkSender(
            transport = senderToReceiverTransport,
            inputStreamProvider = { item ->
                payloads[item.fileId]?.let { ByteArrayInputStream(it) }
            }
        )

        val items = listOf(
            FileTransferItem("file-empty", Uri.EMPTY, "empty.txt", "text/plain", 0L, HashUtils.computeSha256(file1Bytes)),
            FileTransferItem("file-exact", Uri.EMPTY, "exact64k.bin", "application/octet-stream", file2Bytes.size.toLong(), HashUtils.computeSha256(file2Bytes)),
            FileTransferItem("file-multi", Uri.EMPTY, "movie_clip.mp4", "video/mp4", file3Bytes.size.toLong(), HashUtils.computeSha256(file3Bytes))
        )

        receiver.initializeBatch("batch-test-1", items.sumOf { it.sizeBytes })

        val progressSnapshots = mutableListOf<TransferProgress>()
        val result = sender.sendBatch(
            batchId = "batch-test-1",
            filesWithChecksums = items,
            chunkSizeBytes = 64 * 1024,
            onProgress = { progressSnapshots.add(it) }
        )

        assertTrue("Batch transfer should succeed", result.isSuccess)
        assertEquals(3, savedFiles.size)
        assertArrayEquals(file1Bytes, savedFiles["empty.txt"])
        assertArrayEquals(file2Bytes, savedFiles["exact64k.bin"])
        assertArrayEquals(file3Bytes, savedFiles["movie_clip.mp4"])
        assertEquals(100, progressSnapshots.last().overallPercentage)
    }

    @Test
    fun `corrupted chunk fails SHA-256 verification and deletes incomplete temporary file`() = runBlocking {
        val original = ByteArray(50_000) { 0x42 }
        val expectedHash = HashUtils.computeSha256(original)
        val tempFile = File(tempFolder.root, "corrupt_test.tmp")

        val dummyTransport = LoopbackTransport {}
        val receiver = FileChunkReceiver(
            transport = dummyTransport,
            tempFileCreator = { tempFile },
            finalizeFileCallback = { _, _, _ -> Result.success(Uri.EMPTY) },
            onProgress = {}
        )

        receiver.initializeBatch("batch-corrupt", original.size.toLong())
        receiver.onFileStart(
            PacketProtocol.ControlMessage.FileStart(
                batchId = "batch-corrupt",
                fileId = "f-corrupt",
                fileIndex = 0,
                totalFiles = 1,
                fileName = "archive.zip",
                mimeType = "application/zip",
                sizeBytes = original.size.toLong(),
                sha256Checksum = expectedHash
            )
        )

        // Tamper with byte 100 in the transmitted payload
        val tampered = original.clone()
        tampered[100] = 0x7F
        receiver.onChunkReceived(
            PacketProtocol.FileChunkFrame(
                fileId = "f-corrupt",
                chunkIndex = 0L,
                byteOffset = 0L,
                payload = tampered
            )
        )

        val endResult = receiver.onFileEnd(
            PacketProtocol.ControlMessage.FileEnd(
                batchId = "batch-corrupt",
                fileId = "f-corrupt",
                totalChunks = 1L,
                totalBytes = original.size.toLong(),
                sha256Checksum = expectedHash
            )
        )

        assertTrue("Receiver must reject tampered file", endResult.isFailure)
        assertFalse("Incomplete/corrupted temp file must be deleted", tempFile.exists())
    }

    @Test
    fun `transfer cancellation cleans up temporary files immediately`() = runBlocking {
        val data = ByteArray(512 * 1024) { 0x11 }
        val tempFile = File(tempFolder.root, "cancel_test.tmp")
        val startedFirstChunk = CompletableDeferred<Unit>()

        val slowTransport = LoopbackTransport { rawFrame ->
            val decoded = PacketProtocol.decodeFrame(rawFrame)
            if (decoded is PacketProtocol.DecodedFrame.Chunk && decoded.chunkFrame.chunkIndex == 0L) {
                startedFirstChunk.complete(Unit)
            }
            delay(200)
        }

        val receiver = FileChunkReceiver(
            transport = slowTransport,
            tempFileCreator = { tempFile },
            finalizeFileCallback = { _, _, _ -> Result.success(Uri.EMPTY) },
            onProgress = {}
        )
        receiver.initializeBatch("batch-cancel", data.size.toLong())
        receiver.onFileStart(
            PacketProtocol.ControlMessage.FileStart(
                batchId = "batch-cancel",
                fileId = "f-cancel",
                fileIndex = 0,
                totalFiles = 1,
                fileName = "large.bin",
                mimeType = "application/octet-stream",
                sizeBytes = data.size.toLong(),
                sha256Checksum = HashUtils.computeSha256(data)
            )
        )

        val sender = FileChunkSender(
            transport = slowTransport,
            inputStreamProvider = { ByteArrayInputStream(data) }
        )

        val job = launch {
            try {
                sender.sendBatch(
                    batchId = "batch-cancel",
                    filesWithChecksums = listOf(
                        FileTransferItem(
                            "f-cancel",
                            Uri.EMPTY,
                            "large.bin",
                            "application/octet-stream",
                            data.size.toLong(),
                            HashUtils.computeSha256(data)
                        )
                    ),
                    chunkSizeBytes = 16 * 1024,
                    onProgress = {}
                )
            } catch (_: CancellationException) {
            }
        }

        startedFirstChunk.await()
        job.cancelAndJoin()
        receiver.cleanupIncomplete()
        assertFalse("Temp file must be deleted on cancellation", tempFile.exists())
    }
}
