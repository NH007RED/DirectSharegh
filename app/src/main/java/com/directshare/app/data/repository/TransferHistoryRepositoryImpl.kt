package com.directshare.app.data.repository

import com.directshare.app.data.local.TransferHistoryDao
import com.directshare.app.data.local.TransferHistoryEntity
import com.directshare.app.domain.model.TransferDirection
import com.directshare.app.domain.model.TransferHistoryRecord
import com.directshare.app.domain.model.TransferRecordStatus
import com.directshare.app.domain.model.TransferStatisticsSummary
import com.directshare.app.domain.repository.TransferHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransferHistoryRepositoryImpl(
    private val dao: TransferHistoryDao
) : TransferHistoryRepository {

    override fun observeHistory(): Flow<List<TransferHistoryRecord>> {
        return dao.observeAllHistory().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun observeStatistics(): Flow<TransferStatisticsSummary> {
        return observeHistory().map { records ->
            val completedSent = records.filter {
                it.direction == TransferDirection.SENT && it.status == TransferRecordStatus.COMPLETED
            }
            val completedReceived = records.filter {
                it.direction == TransferDirection.RECEIVED && it.status == TransferRecordStatus.COMPLETED
            }
            val failedOrCancelled = records.count {
                it.status != TransferRecordStatus.COMPLETED
            }
            TransferStatisticsSummary(
                totalSentCount = completedSent.size,
                totalReceivedCount = completedReceived.size,
                totalBytesSent = completedSent.sumOf { it.fileSizeBytes },
                totalBytesReceived = completedReceived.sumOf { it.fileSizeBytes },
                successfulTransfersCount = completedSent.size + completedReceived.size,
                failedTransfersCount = failedOrCancelled
            )
        }
    }

    override suspend fun insertRecord(record: TransferHistoryRecord) {
        dao.insertHistory(TransferHistoryEntity.fromDomainModel(record))
    }

    override suspend fun clearAllHistory() {
        dao.clearAllHistory()
    }
}
