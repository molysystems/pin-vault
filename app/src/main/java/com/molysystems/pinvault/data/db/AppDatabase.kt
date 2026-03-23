package com.molysystems.pinvault.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.molysystems.pinvault.data.model.AppEntry
import com.molysystems.pinvault.data.model.CredentialField
import com.molysystems.pinvault.data.model.RecoveryCode

@Database(
    entities = [
        AppEntry::class,
        CredentialField::class,
        RecoveryCode::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appEntryDao(): AppEntryDao
    abstract fun credentialFieldDao(): CredentialFieldDao
    abstract fun recoveryCodeDao(): RecoveryCodeDao
}
