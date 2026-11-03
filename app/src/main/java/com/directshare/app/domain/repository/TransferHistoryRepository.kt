package com.directshare.app.domain.repository

import com.directshare.app.domain.model.TransferHistoryRecord
import com.directshare.app.domain.model.TransferStatisticsSummary
import kotlinx.coroutines.flow.Flow

interface TransferHistoryRepository {
    fun observeHistory(): Flow<List<TransferHistoryRecord>>
    fun observeStatistics(): Flow<TransferStatisticsSummary>
    suspend fun insertRecord(record: TransferHistoryRecord)
    suspend fun clearAllHistory()
}
