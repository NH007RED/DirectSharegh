package com.directshare.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferHistoryDao {

    @Query("SELECT * FROM transfer_history ORDER BY timestamp DESC")
    fun observeAllHistory(): Flow<List<TransferHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: TransferHistoryEntity)

    @Query("DELETE FROM transfer_history")
    suspend fun clearAllHistory()
}
