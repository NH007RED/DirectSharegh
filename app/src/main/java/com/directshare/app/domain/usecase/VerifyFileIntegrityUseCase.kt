package com.directshare.app.domain.usecase

import com.directshare.app.utils.HashUtils
import java.io.File
import java.io.InputStream
import javax.inject.Inject

class VerifyFileIntegrityUseCase @Inject constructor() {

    fun verifyFile(file: File, expectedSizeBytes: Long, expectedSha256Hex: String): Boolean {
        if (!file.exists() || file.length() != expectedSizeBytes) {
            return false
        }
        val actualHash = HashUtils.computeSha256(file)
        return actualHash.equals(expectedSha256Hex.trim(), ignoreCase = true)
    }

    fun verifyStreamDigest(actualSha256Hex: String, actualBytes: Long, expectedSizeBytes: Long, expectedSha256Hex: String): Boolean {
        if (actualBytes != expectedSizeBytes) return false
        return actualSha256Hex.equals(expectedSha256Hex.trim(), ignoreCase = true)
    }

    fun computeSha256(inputStream: InputStream): String = HashUtils.computeSha256(inputStream)
}
