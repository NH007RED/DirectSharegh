package com.directshare.app.webrtc.datachannel

import com.directshare.app.domain.model.IncomingFileDescriptor
import com.directshare.app.domain.model.IncomingTransferOffer
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * Framing protocol for DirectShare's reliable WebRTC DataChannel.
 *
 * Frame layout (Big-Endian):
 * Byte 0: FrameType
 *   0x01 = CONTROL_JSON (UTF-8 JSON control message)
 *   0x02 = FILE_CHUNK   (Binary file chunk associated with a specific fileId and sequence index)
 *
 * For CONTROL_JSON (0x01):
 *   Bytes 1..N: UTF-8 encoded JSON string
 *
 * For FILE_CHUNK (0x02):
 *   Bytes 1..2:   uint16 fileIdUtf8Length (L)
 *   Bytes 3..3+L: UTF-8 fileId string
 *   Next 8 bytes: int64 chunkIndex (0-based sequential chunk index)
 *   Next 8 bytes: int64 byteOffset (exact offset in file where this chunk begins)
 *   Next 4 bytes: int32 payloadLength (P, can be 0 for empty file marker)
 *   Next P bytes: Raw file chunk bytes
 */
object PacketProtocol {
    const val FRAME_TYPE_CONTROL_JSON: Byte = 0x01
    const val FRAME_TYPE_FILE_CHUNK: Byte = 0x02

    const val HIGH_WATER_MARK_BYTES: Long = 8L * 1024L * 1024L // Pause sending above 8 MB buffered
    const val LOW_WATER_MARK_BYTES: Long = 2L * 1024L * 1024L  // Resume sending below 2 MB buffered

    sealed interface DecodedFrame {
        data class Control(val message: ControlMessage) : DecodedFrame
        data class Chunk(val chunkFrame: FileChunkFrame) : DecodedFrame
    }

    data class FileChunkFrame(
        val fileId: String,
        val chunkIndex: Long,
        val byteOffset: Long,
        val payload: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is FileChunkFrame) return false
            return fileId == other.fileId &&
                chunkIndex == other.chunkIndex &&
                byteOffset == other.byteOffset &&
                payload.contentEquals(other.payload)
        }

        override fun hashCode(): Int {
            var result = fileId.hashCode()
            result = 31 * result + chunkIndex.hashCode()
            result = 31 * result + byteOffset.hashCode()
            result = 31 * result + payload.contentHashCode()
            return result
        }
    }

    sealed interface ControlMessage {
        data class TransferOffer(val offer: IncomingTransferOffer) : ControlMessage
        data class TransferDecision(val batchId: String, val accepted: Boolean, val reason: String = "") : ControlMessage
        data class FileStart(
            val batchId: String,
            val fileId: String,
            val fileIndex: Int,
            val totalFiles: Int,
            val fileName: String,
            val mimeType: String,
            val sizeBytes: Long,
            val sha256Checksum: String
        ) : ControlMessage
        data class FileEnd(
            val batchId: String,
            val fileId: String,
            val totalChunks: Long,
            val totalBytes: Long,
            val sha256Checksum: String
        ) : ControlMessage
        data class FileVerifiedAck(
            val batchId: String,
            val fileId: String,
            val verified: Boolean,
            val errorMessage: String = ""
        ) : ControlMessage
        data class BatchComplete(val batchId: String, val totalFiles: Int, val totalBytes: Long) : ControlMessage
        data class CancelTransfer(val batchId: String, val reason: String) : ControlMessage
    }

    fun encodeControlMessage(message: ControlMessage): ByteArray {
        val json = when (message) {
            is ControlMessage.TransferOffer -> {
                val filesArr = JSONArray()
                for (f in message.offer.files) {
                    filesArr.put(
                        JSONObject().apply {
                            put("fileId", f.fileId)
                            put("name", f.name)
                            put("mimeType", f.mimeType)
                            put("sizeBytes", f.sizeBytes)
                            put("sha256Checksum", f.sha256Checksum)
                        }
                    )
                }
                JSONObject().apply {
                    put("type", "TRANSFER_OFFER")
                    put("batchId", message.offer.batchId)
                    put("senderUid", message.offer.senderUid)
                    put("senderDeviceName", message.offer.senderDeviceName)
                    put("totalBytes", message.offer.totalBytes)
                    put("timestamp", message.offer.timestamp)
                    put("files", filesArr)
                }
            }
            is ControlMessage.TransferDecision -> JSONObject().apply {
                put("type", "TRANSFER_DECISION")
                put("batchId", message.batchId)
                put("accepted", message.accepted)
                put("reason", message.reason)
            }
            is ControlMessage.FileStart -> JSONObject().apply {
                put("type", "FILE_START")
                put("batchId", message.batchId)
                put("fileId", message.fileId)
                put("fileIndex", message.fileIndex)
                put("totalFiles", message.totalFiles)
                put("fileName", message.fileName)
                put("mimeType", message.mimeType)
                put("sizeBytes", message.sizeBytes)
                put("sha256Checksum", message.sha256Checksum)
            }
            is ControlMessage.FileEnd -> JSONObject().apply {
                put("type", "FILE_END")
                put("batchId", message.batchId)
                put("fileId", message.fileId)
                put("totalChunks", message.totalChunks)
                put("totalBytes", message.totalBytes)
                put("sha256Checksum", message.sha256Checksum)
            }
            is ControlMessage.FileVerifiedAck -> JSONObject().apply {
                put("type", "FILE_VERIFIED_ACK")
                put("batchId", message.batchId)
                put("fileId", message.fileId)
                put("verified", message.verified)
                put("errorMessage", message.errorMessage)
            }
            is ControlMessage.BatchComplete -> JSONObject().apply {
                put("type", "BATCH_COMPLETE")
                put("batchId", message.batchId)
                put("totalFiles", message.totalFiles)
                put("totalBytes", message.totalBytes)
            }
            is ControlMessage.CancelTransfer -> JSONObject().apply {
                put("type", "CANCEL_TRANSFER")
                put("batchId", message.batchId)
                put("reason", message.reason)
            }
        }
        val jsonBytes = json.toString().toByteArray(StandardCharsets.UTF_8)
        val out = ByteArray(1 + jsonBytes.size)
        out[0] = FRAME_TYPE_CONTROL_JSON
        System.arraycopy(jsonBytes, 0, out, 1, jsonBytes.size)
        return out
    }

    fun encodeFileChunk(
        fileId: String,
        chunkIndex: Long,
        byteOffset: Long,
        payload: ByteArray,
        payloadLength: Int = payload.size
    ): ByteArray {
        val fileIdBytes = fileId.toByteArray(StandardCharsets.UTF_8)
        require(fileIdBytes.size <= 1024) { "fileId UTF-8 length exceeds 1024 bytes" }
        require(payloadLength in 0..payload.size) { "Invalid payloadLength: $payloadLength" }

        val headerSize = 1 + 2 + fileIdBytes.size + 8 + 8 + 4
        val buffer = ByteBuffer.allocate(headerSize + payloadLength)
        buffer.put(FRAME_TYPE_FILE_CHUNK)
        buffer.putShort(fileIdBytes.size.toShort())
        buffer.put(fileIdBytes)
        buffer.putLong(chunkIndex)
        buffer.putLong(byteOffset)
        buffer.putInt(payloadLength)
        if (payloadLength > 0) {
            buffer.put(payload, 0, payloadLength)
        }
        return buffer.array()
    }

    fun decodeFrame(rawBytes: ByteArray): DecodedFrame {
        require(rawBytes.isNotEmpty()) { "Cannot decode empty frame" }
        return when (val type = rawBytes[0]) {
            FRAME_TYPE_CONTROL_JSON -> {
                val jsonStr = String(rawBytes, 1, rawBytes.size - 1, StandardCharsets.UTF_8)
                val obj = JSONObject(jsonStr)
                val control = when (val msgType = obj.getString("type")) {
                    "TRANSFER_OFFER" -> {
                        val filesArr = obj.getJSONArray("files")
                        val descriptors = mutableListOf<IncomingFileDescriptor>()
                        for (i in 0 until filesArr.length()) {
                            val item = filesArr.getJSONObject(i)
                            descriptors.add(
                                IncomingFileDescriptor(
                                    fileId = item.getString("fileId"),
                                    name = item.getString("name"),
                                    mimeType = item.getString("mimeType"),
                                    sizeBytes = item.getLong("sizeBytes"),
                                    sha256Checksum = item.getString("sha256Checksum")
                                )
                            )
                        }
                        ControlMessage.TransferOffer(
                            IncomingTransferOffer(
                                batchId = obj.getString("batchId"),
                                senderUid = obj.getString("senderUid"),
                                senderDeviceName = obj.getString("senderDeviceName"),
                                files = descriptors,
                                totalBytes = obj.getLong("totalBytes"),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                    "TRANSFER_DECISION" -> ControlMessage.TransferDecision(
                        batchId = obj.getString("batchId"),
                        accepted = obj.getBoolean("accepted"),
                        reason = obj.optString("reason", "")
                    )
                    "FILE_START" -> ControlMessage.FileStart(
                        batchId = obj.getString("batchId"),
                        fileId = obj.getString("fileId"),
                        fileIndex = obj.getInt("fileIndex"),
                        totalFiles = obj.getInt("totalFiles"),
                        fileName = obj.getString("fileName"),
                        mimeType = obj.getString("mimeType"),
                        sizeBytes = obj.getLong("sizeBytes"),
                        sha256Checksum = obj.getString("sha256Checksum")
                    )
                    "FILE_END" -> ControlMessage.FileEnd(
                        batchId = obj.getString("batchId"),
                        fileId = obj.getString("fileId"),
                        totalChunks = obj.getLong("totalChunks"),
                        totalBytes = obj.getLong("totalBytes"),
                        sha256Checksum = obj.getString("sha256Checksum")
                    )
                    "FILE_VERIFIED_ACK" -> ControlMessage.FileVerifiedAck(
                        batchId = obj.getString("batchId"),
                        fileId = obj.getString("fileId"),
                        verified = obj.getBoolean("verified"),
                        errorMessage = obj.optString("errorMessage", "")
                    )
                    "BATCH_COMPLETE" -> ControlMessage.BatchComplete(
                        batchId = obj.getString("batchId"),
                        totalFiles = obj.getInt("totalFiles"),
                        totalBytes = obj.getLong("totalBytes")
                    )
                    "CANCEL_TRANSFER" -> ControlMessage.CancelTransfer(
                        batchId = obj.getString("batchId"),
                        reason = obj.optString("reason", "Cancelled by peer")
                    )
                    else -> throw IllegalArgumentException("Unsupported control message type: $msgType")
                }
                DecodedFrame.Control(control)
            }
            FRAME_TYPE_FILE_CHUNK -> {
                val buf = ByteBuffer.wrap(rawBytes)
                buf.get() // skip frame type
                val idLen = buf.short.toInt() and 0xFFFF
                val idBytes = ByteArray(idLen)
                buf.get(idBytes)
                val fileId = String(idBytes, StandardCharsets.UTF_8)
                val chunkIndex = buf.long
                val byteOffset = buf.long
                val payloadLen = buf.int
                require(payloadLen >= 0 && payloadLen <= buf.remaining()) {
                    "Corrupt chunk frame: payloadLen=$payloadLen remaining=${buf.remaining()}"
                }
                val payload = ByteArray(payloadLen)
                if (payloadLen > 0) {
                    buf.get(payload)
                }
                DecodedFrame.Chunk(
                    FileChunkFrame(
                        fileId = fileId,
                        chunkIndex = chunkIndex,
                        byteOffset = byteOffset,
                        payload = payload
                    )
                )
            }
            else -> throw IllegalArgumentException("Unknown frame type byte: $type")
        }
    }
}
