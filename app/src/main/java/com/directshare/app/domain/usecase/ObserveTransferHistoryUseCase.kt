package com.directshare.app.domain.usecase

import com.directshare.app.domain.model.TransferHistoryRecord
import com.directshare.app.domain.model.TransferStatisticsSummary
import com.directshare.app.domain.repository.TransferHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTransferHistoryUseCase @Inject constructor(
    private val historyRepository: TransferHistoryRepository
) {
    fun observeRecords(): Flow<List<TransferHistoryRecord>> = historyRepository.observeHistory()
    fun observeStats(): Flow<TransferStatisticsSummary> = historyRepository.observeStatistics()
    suspend fun clearAll() = historyRepository.clearAllHistory()
}
