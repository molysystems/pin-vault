package com.molysystems.pinvault.di

import android.content.Context
import androidx.room.Room
import com.molysystems.pinvault.data.db.AppDatabase
import com.molysystems.pinvault.data.db.AppEntryDao
import com.molysystems.pinvault.data.db.CredentialFieldDao
import com.molysystems.pinvault.data.db.RecoveryCodeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "pinvault.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideAppEntryDao(db: AppDatabase): AppEntryDao = db.appEntryDao()

    @Provides
    fun provideCredentialFieldDao(db: AppDatabase): CredentialFieldDao = db.credentialFieldDao()

    @Provides
    fun provideRecoveryCodeDao(db: AppDatabase): RecoveryCodeDao = db.recoveryCodeDao()
}
