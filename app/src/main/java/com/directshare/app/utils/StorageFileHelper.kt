package com.directshare.app.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.directshare.app.domain.model.FileTransferItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * Handles all ContentResolver / Storage Access Framework (SAF) / MediaStore operations
 * on Dispatchers.IO without assuming raw file system paths for user-picked URIs.
 */
class StorageFileHelper(private val context: Context) {

    suspend fun inspectUri(uri: Uri): FileTransferItem? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            var displayName = "shared_file_${System.currentTimeMillis()}"
            var sizeBytes = -1L

            val cursor: Cursor? = resolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1 && !c.isNull(nameIdx)) {
                        displayName = sanitizeFileName(c.getString(nameIdx))
                    }
                    val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx != -1 && !c.isNull(sizeIdx)) {
                        sizeBytes = c.getLong(sizeIdx)
                    }
                }
            }

            if (sizeBytes < 0L) {
                resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                    if (afd.length >= 0L) {
                        sizeBytes = afd.length
                    }
                }
            }

            if (sizeBytes < 0L) {
                // Fallback: count bytes via stream if provider omits size metadata
                resolver.openInputStream(uri)?.use { stream ->
                    var counted = 0L
                    val buf = ByteArray(16 * 1024)
                    var r = stream.read(buf)
                    while (r != -1) {
                        counted += r
                        r = stream.read(buf)
                    }
                    sizeBytes = counted
                } ?: return@withContext null
            }

            val detectedMime = resolver.getType(uri)
                ?: guessMimeTypeFromName(displayName)
                ?: "application/octet-stream"

            FileTransferItem(
                fileId = UUID.randomUUID().toString(),
                uri = uri,
                name = displayName,
                mimeType = detectedMime,
                sizeBytes = sizeBytes.coerceAtLeast(0L)
            )
        } catch (e: Exception) {
            AppLogger.e("StorageFileHelper", "Failed to inspect URI: $uri", e)
            null
        }
    }

    suspend fun computeUriSha256(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                HashUtils.computeSha256(stream)
            }
        } catch (e: Exception) {
            AppLogger.e("StorageFileHelper", "Failed to compute SHA-256 for URI", e)
            null
        }
    }

    fun openInputStream(uri: Uri): InputStream? {
        return context.contentResolver.openInputStream(uri)
    }

    fun hasEnoughFreeSpace(requiredBytes: Long): Boolean {
        val safetyMarginBytes = 10L * 1024L * 1024L // 10 MB safety buffer
        val stat = StatFs(context.filesDir.absolutePath)
        return stat.availableBytes >= (requiredBytes + safetyMarginBytes)
    }

    fun createTempTransferFile(fileId: String): File {
        val tempDir = File(context.cacheDir, "transfers")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        return File(tempDir, "part_${sanitizeFileName(fileId)}.tmp")
    }

    fun cleanupTempFiles() {
        val tempDir = File(context.cacheDir, "transfers")
        tempDir.listFiles()?.forEach { file ->
            try {
                file.delete()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Moves a verified temporary file into the user's configured SAF directory,
     * Android MediaStore Downloads (API 29+), or the app's FileProvider-accessible directory.
     */
    suspend fun finalizeReceivedFile(
        tempFile: File,
        displayName: String,
        mimeType: String,
        customSafTreeUriString: String?
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val cleanName = sanitizeFileName(displayName)
        try {
            // 1. If user chose a custom SAF folder in Settings, try writing there first
            if (!customSafTreeUriString.isNullOrBlank()) {
                val treeUri = Uri.parse(customSafTreeUriString)
                val docTree = DocumentFile.fromTreeUri(context, treeUri)
                if (docTree != null && docTree.canWrite()) {
                    val targetDoc = docTree.createFile(mimeType, cleanName)
                    if (targetDoc != null) {
                        context.contentResolver.openOutputStream(targetDoc.uri)?.use { out ->
                            FileInputStream(tempFile).use { input ->
                                input.copyTo(out, 32 * 1024)
                            }
                        }
                        tempFile.delete()
                        return@withContext Result.success(targetDoc.uri)
                    }
                }
            }

            // 2. On Android 10+ (API 29+), save into MediaStore.Downloads so user can access in Files app without storage permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, cleanName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DirectShare")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collection, values)
                if (itemUri != null) {
                    resolver.openOutputStream(itemUri)?.use { out ->
                        FileInputStream(tempFile).use { input ->
                            input.copyTo(out, 32 * 1024)
                        }
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(itemUri, values, null, null)
                    tempFile.delete()
                    return@withContext Result.success(itemUri)
                }
            }

            // 3. Fallback: Save into app-private received directory and expose via FileProvider
            val receivedDir = File(context.filesDir, "received")
            if (!receivedDir.exists()) {
                receivedDir.mkdirs()
            }
            val destFile = resolveUniqueFile(receivedDir, cleanName)
            FileInputStream(tempFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output, 32 * 1024)
                }
            }
            tempFile.delete()
            val providerUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                destFile
            )
            Result.success(providerUri)
        } catch (e: Exception) {
            AppLogger.e("StorageFileHelper", "Failed to finalize received file: $cleanName", e)
            Result.failure(e)
        }
    }

    fun buildOpenFileIntent(uri: Uri, mimeType: String): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType.ifBlank { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildShareFileIntent(uri: Uri, mimeType: String, fileName: String): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType.ifBlank { "*/*" }
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(shareIntent, "Share $fileName").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun resolveUniqueFile(dir: File, fileName: String): File {
        var candidate = File(dir, fileName)
        if (!candidate.exists()) return candidate
        val base = fileName.substringBeforeLast(".")
        val ext = fileName.substringAfterLast(".", "")
        var counter = 1
        while (candidate.exists()) {
            val numbered = if (ext.isNotEmpty()) "${base}_($counter).$ext" else "${base}_($counter)"
            candidate = File(dir, numbered)
            counter++
        }
        return candidate
    }

    fun sanitizeFileName(raw: String): String {
        return raw.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifEmpty { "received_file" }
    }

    private fun guessMimeTypeFromName(fileName: String): String? {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext.isEmpty()) return null
        return when (ext) {
            "apk" -> "application/vnd.android.package-archive"
            "zip" -> "application/zip"
            "pdf" -> "application/pdf"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        }
    }
}
