package com.directshare.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TransferHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DirectShareDatabase : RoomDatabase() {
    abstract fun transferHistoryDao(): TransferHistoryDao

    companion object {
        const val DATABASE_NAME = "directshare_history.db"
    }
}
