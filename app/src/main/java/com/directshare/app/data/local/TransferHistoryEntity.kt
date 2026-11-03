package com.directshare.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.directshare.app.domain.model.TransferDirection
import com.directshare.app.domain.model.TransferHistoryRecord
import com.directshare.app.domain.model.TransferRecordStatus

/**
 * Stores ONLY transfer metadata in Room. File contents are never stored in Room.
 */
@Entity(tableName = "transfer_history")
data class TransferHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val fileName: String,
    val fileSizeBytes: Long,
    val mimeType: String,
    val direction: String,
    val timestamp: Long,
    val status: String,
    val peerDisplayName: String,
    val sha256Checksum: String?,
    val savedFileUri: String?
) {
    fun toDomainModel(): TransferHistoryRecord {
        return TransferHistoryRecord(
            id = id,
            fileName = fileName,
            fileSizeBytes = fileSizeBytes,
            mimeType = mimeType,
            direction = runCatching { TransferDirection.valueOf(direction) }.getOrDefault(TransferDirection.SENT),
            timestamp = timestamp,
            status = runCatching { TransferRecordStatus.valueOf(status) }.getOrDefault(TransferRecordStatus.COMPLETED),
            peerDisplayName = peerDisplayName,
            sha256Checksum = sha256Checksum,
            savedFileUri = savedFileUri
        )
    }

    companion object {
        fun fromDomainModel(record: TransferHistoryRecord): TransferHistoryEntity {
            return TransferHistoryEntity(
                id = record.id,
                fileName = record.fileName,
                fileSizeBytes = record.fileSizeBytes,
                mimeType = record.mimeType,
                direction = record.direction.name,
                timestamp = record.timestamp,
                status = record.status.name,
                peerDisplayName = record.peerDisplayName,
                sha256Checksum = record.sha256Checksum,
                savedFileUri = record.savedFileUri
            )
        }
    }
}
