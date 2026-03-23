package com.molysystems.pinvault.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.molysystems.pinvault.data.model.AppEntry
import com.molysystems.pinvault.data.model.CredentialField
import kotlinx.coroutines.flow.Flow

@Dao
interface AppEntryDao {

    @Query("SELECT * FROM app_entries ORDER BY lastAccessedAt DESC")
    fun getAllApps(): Flow<List<AppEntry>>

    @Query("SELECT * FROM app_entries WHERE id = :id")
    suspend fun getById(id: Long): AppEntry?

    @Query("SELECT * FROM app_entries WHERE packageName = :packageName")
    suspend fun getByPackageName(packageName: String): AppEntry?

    @Query("SELECT packageName FROM app_entries")
    suspend fun getAllPackageNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(appEntry: AppEntry): Long

    @Update
    suspend fun update(appEntry: AppEntry)

    @Delete
    suspend fun delete(appEntry: AppEntry)

    @Query("UPDATE app_entries SET lastAccessedAt = :timestamp WHERE id = :id")
    suspend fun updateLastAccessed(id: Long, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface CredentialFieldDao {

    @Query("SELECT * FROM credential_fields WHERE appEntryId = :appEntryId ORDER BY sortOrder ASC")
    fun getByAppEntryId(appEntryId: Long): Flow<List<CredentialField>>

    @Query("SELECT * FROM credential_fields WHERE appEntryId = :appEntryId ORDER BY sortOrder ASC")
    suspend fun getByAppEntryIdOnce(appEntryId: Long): List<CredentialField>

    @Query("SELECT * FROM credential_fields WHERE id = :id")
    suspend fun getById(id: Long): CredentialField?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(field: CredentialField): Long

    @Update
    suspend fun update(field: CredentialField)

    @Delete
    suspend fun delete(field: CredentialField)

    @Query("DELETE FROM credential_fields WHERE appEntryId = :appEntryId")
    suspend fun deleteByAppEntryId(appEntryId: Long)
}
