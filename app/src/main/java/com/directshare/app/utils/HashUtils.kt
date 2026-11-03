package com.directshare.app.utils

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

object HashUtils {
    private const val STREAM_BUFFER_SIZE = 32 * 1024

    fun createSha256Digest(): MessageDigest = MessageDigest.getInstance("SHA-256")

    fun computeSha256(bytes: ByteArray): String {
        val digest = createSha256Digest()
        digest.update(bytes)
        return bytesToHex(digest.digest())
    }

    fun computeSha256(file: File): String {
        FileInputStream(file).use { fis ->
            return computeSha256(fis)
        }
    }

    fun computeSha256(inputStream: InputStream): String {
        val digest = createSha256Digest()
        val buffer = ByteArray(STREAM_BUFFER_SIZE)
        var read = inputStream.read(buffer)
        while (read != -1) {
            if (read > 0) {
                digest.update(buffer, 0, read)
            }
            read = inputStream.read(buffer)
        }
        return bytesToHex(digest.digest())
    }

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789abcdef".toCharArray()
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = hexArray[v ushr 4]
            hexChars[i * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}
