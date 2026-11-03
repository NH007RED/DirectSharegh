package com.directshare.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.directshare.app.data.local.DirectShareDatabase
import com.directshare.app.data.repository.TransferHistoryRepositoryImpl
import com.directshare.app.domain.model.TransferDirection
import com.directshare.app.domain.model.TransferHistoryRecord
import com.directshare.app.domain.model.TransferRecordStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransferIntegrationTest {

    private lateinit var database: DirectShareDatabase
    private lateinit var repository: TransferHistoryRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DirectShareDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TransferHistoryRepositoryImpl(database.transferHistoryDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertObserveAndClearHistoryRecordsComputesStatisticsAccurately() = runBlocking {
        repository.insertRecord(
            TransferHistoryRecord(
                fileName = "vacation_4k.mp4",
                fileSizeBytes = 104_857_600L,
                mimeType = "video/mp4",
                direction = TransferDirection.SENT,
                timestamp = System.currentTimeMillis(),
                status = TransferRecordStatus.COMPLETED,
                peerDisplayName = "Galaxy S24",
                sha256Checksum = "a1b2c3d4"
            )
        )
        repository.insertRecord(
            TransferHistoryRecord(
                fileName = "slides.pdf",
                fileSizeBytes = 5_242_880L,
                mimeType = "application/pdf",
                direction = TransferDirection.RECEIVED,
                timestamp = System.currentTimeMillis(),
                status = TransferRecordStatus.COMPLETED,
                peerDisplayName = "Pixel 9",
                sha256Checksum = "e5f6g7h8"
            )
        )

        val records = repository.observeHistory().first()
        assertEquals(2, records.size)

        val stats = repository.observeStatistics().first()
        assertEquals(1, stats.totalSentCount)
        assertEquals(1, stats.totalReceivedCount)
        assertEquals(104_857_600L, stats.totalBytesSent)
        assertEquals(5_242_880L, stats.totalBytesReceived)
        assertEquals(2, stats.successfulTransfersCount)

        repository.clearAllHistory()
        assertTrue(repository.observeHistory().first().isEmpty())
    }
}
