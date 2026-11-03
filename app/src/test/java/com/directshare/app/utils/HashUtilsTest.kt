package com.directshare.app.utils

import com.directshare.app.domain.usecase.VerifyFileIntegrityUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream

class HashUtilsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val verifyUseCase = VerifyFileIntegrityUseCase()

    @Test
    fun `empty byte array produces standard NIST SHA-256 digest`() {
        val emptyHash = HashUtils.computeSha256(ByteArray(0))
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            emptyHash
        )
    }

    @Test
    fun `multi-megabyte stream and file produce identical SHA-256 and pass integrity verification`() {
        // 1.5 MB + 371 odd tail bytes to test non-aligned chunk boundaries
        val data = ByteArray(1_573_235) { idx -> ((idx * 31 + 7) and 0xFF).toByte() }
        val expectedHash = HashUtils.computeSha256(data)
        val streamHash = HashUtils.computeSha256(ByteArrayInputStream(data))
        assertEquals(expectedHash, streamHash)

        val tempFile = tempFolder.newFile("sample_payload.bin")
        tempFile.writeBytes(data)

        assertTrue(verifyUseCase.verifyFile(tempFile, data.size.toLong(), expectedHash))
        assertFalse(verifyUseCase.verifyFile(tempFile, data.size.toLong() - 1L, expectedHash))
        assertFalse(verifyUseCase.verifyFile(tempFile, data.size.toLong(), "00000000" + expectedHash.drop(8)))
    }
}
