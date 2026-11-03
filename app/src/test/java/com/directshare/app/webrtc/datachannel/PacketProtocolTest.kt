package com.directshare.app.webrtc.datachannel

import com.directshare.app.domain.model.IncomingFileDescriptor
import com.directshare.app.domain.model.IncomingTransferOffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacketProtocolTest {

    @Test
    fun `encode and decode empty file chunk preserves 0-length payload`() {
        val raw = PacketProtocol.encodeFileChunk(
            fileId = "empty-file-001",
            chunkIndex = 0L,
            byteOffset = 0L,
            payload = ByteArray(0),
            payloadLength = 0
        )
        val decoded = PacketProtocol.decodeFrame(raw)
        assertTrue(decoded is PacketProtocol.DecodedFrame.Chunk)
        val chunk = (decoded as PacketProtocol.DecodedFrame.Chunk).chunkFrame
        assertEquals("empty-file-001", chunk.fileId)
        assertEquals(0L, chunk.chunkIndex)
        assertEquals(0L, chunk.byteOffset)
        assertEquals(0, chunk.payload.size)
    }

    @Test
    fun `encode and decode partial final chunk slices exact payloadLength`() {
        val reusableBuffer = ByteArray(64 * 1024) { (it % 251).toByte() }
        val finalChunkLength = 1337
        val raw = PacketProtocol.encodeFileChunk(
            fileId = "video-file-02",
            chunkIndex = 19L,
            byteOffset = 19L * 65536L,
            payload = reusableBuffer,
            payloadLength = finalChunkLength
        )
        val decoded = PacketProtocol.decodeFrame(raw) as PacketProtocol.DecodedFrame.Chunk
        assertEquals("video-file-02", decoded.chunkFrame.fileId)
        assertEquals(19L, decoded.chunkFrame.chunkIndex)
        assertEquals(19L * 65536L, decoded.chunkFrame.byteOffset)
        assertEquals(finalChunkLength, decoded.chunkFrame.payload.size)
        assertArrayEquals(reusableBuffer.copyOfRange(0, finalChunkLength), decoded.chunkFrame.payload)
    }

    @Test
    fun `encode and decode TransferOffer control message roundtrips all descriptors`() {
        val offer = IncomingTransferOffer(
            batchId = "batch-99",
            senderUid = "uid-sender-1",
            senderDeviceName = "Pixel 9 Pro",
            files = listOf(
                IncomingFileDescriptor("f1", "photo.jpg", "image/jpeg", 409600L, "abc123sha256"),
                IncomingFileDescriptor("f2", "archive.zip", "application/zip", 10485760L, "def456sha256")
            ),
            totalBytes = 409600L + 10485760L,
            timestamp = 1730000000000L
        )
        val encoded = PacketProtocol.encodeControlMessage(PacketProtocol.ControlMessage.TransferOffer(offer))
        val decoded = PacketProtocol.decodeFrame(encoded) as PacketProtocol.DecodedFrame.Control
        val roundtrip = (decoded.message as PacketProtocol.ControlMessage.TransferOffer).offer

        assertEquals("batch-99", roundtrip.batchId)
        assertEquals("Pixel 9 Pro", roundtrip.senderDeviceName)
        assertEquals(2, roundtrip.files.size)
        assertEquals("archive.zip", roundtrip.files[1].name)
        assertEquals(10485760L, roundtrip.files[1].sizeBytes)
    }
}
