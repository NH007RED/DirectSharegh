package com.directshare.app.di

import android.content.Context
import androidx.room.Room
import com.directshare.app.data.firebase.FirebaseAuthManager
import com.directshare.app.data.firebase.FirebaseSignalingService
import com.directshare.app.data.local.DirectShareDatabase
import com.directshare.app.data.local.SettingsDataStore
import com.directshare.app.data.local.TransferHistoryDao
import com.directshare.app.data.repository.SettingsRepositoryImpl
import com.directshare.app.data.repository.SignalingRepositoryImpl
import com.directshare.app.data.repository.TransferHistoryRepositoryImpl
import com.directshare.app.domain.repository.SettingsRepository
import com.directshare.app.domain.repository.SignalingRepository
import com.directshare.app.domain.repository.TransferHistoryRepository
import com.directshare.app.utils.StorageFileHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return FirebaseDatabase.getInstance().apply {
            // Keep signaling purely online and never cache sensitive SDP payloads on disk
            setPersistenceEnabled(false)
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseAuthManager(firebaseAuth: FirebaseAuth): FirebaseAuthManager {
        return FirebaseAuthManager(firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideFirebaseSignalingService(
        database: FirebaseDatabase,
        authManager: FirebaseAuthManager
    ): FirebaseSignalingService {
        return FirebaseSignalingService(database, authManager)
    }

    @Provides
    @Singleton
    fun provideDirectShareDatabase(
        @ApplicationContext context: Context
    ): DirectShareDatabase {
        return Room.databaseBuilder(
            context,
            DirectShareDatabase::class.java,
            DirectShareDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideTransferHistoryDao(database: DirectShareDatabase): TransferHistoryDao {
        return database.transferHistoryDao()
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context
    ): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideStorageFileHelper(
        @ApplicationContext context: Context
    ): StorageFileHelper {
        return StorageFileHelper(context)
    }

    @Provides
    @Singleton
    fun provideSignalingRepository(
        signalingService: FirebaseSignalingService,
        authManager: FirebaseAuthManager
    ): SignalingRepository {
        return SignalingRepositoryImpl(signalingService, authManager)
    }

    @Provides
    @Singleton
    fun provideTransferHistoryRepository(
        dao: TransferHistoryDao
    ): TransferHistoryRepository {
        return TransferHistoryRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        dataStore: SettingsDataStore
    ): SettingsRepository {
        return SettingsRepositoryImpl(dataStore)
    }
}
