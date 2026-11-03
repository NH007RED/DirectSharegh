package com.directshare.app.domain.usecase

import android.net.Uri
import com.directshare.app.domain.model.FileTransferItem
import com.directshare.app.utils.StorageFileHelper
import javax.inject.Inject

class ValidateFileSelectionUseCase @Inject constructor(
    private val storageFileHelper: StorageFileHelper
) {
    suspend operator fun invoke(uris: List<Uri>): Result<List<FileTransferItem>> {
        if (uris.isEmpty()) {
            return Result.failure(IllegalArgumentException("No files selected"))
        }
        val resolvedItems = mutableListOf<FileTransferItem>()
        for (uri in uris.distinct()) {
            val item = storageFileHelper.inspectUri(uri)
                ?: return Result.failure(
                    SecurityException("Unable to read metadata or access URI: $uri")
                )
            resolvedItems.add(item)
        }
        return Result.success(resolvedItems)
    }
}
