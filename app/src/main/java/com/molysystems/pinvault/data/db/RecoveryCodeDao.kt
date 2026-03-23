package com.molysystems.pinvault.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.molysystems.pinvault.data.model.RecoveryCode
import kotlinx.coroutines.flow.Flow

@Dao
interface RecoveryCodeDao {

    @Query("SELECT * FROM recovery_codes WHERE appEntryId = :appEntryId ORDER BY sortOrder ASC, id ASC")
    fun getByAppEntryId(appEntryId: Long): Flow<List<RecoveryCode>>

    @Query("SELECT * FROM recovery_codes WHERE appEntryId = :appEntryId ORDER BY sortOrder ASC, id ASC")
    suspend fun getByAppEntryIdOnce(appEntryId: Long): List<RecoveryCode>

    @Query("SELECT * FROM recovery_codes WHERE id = :id")
    suspend fun getById(id: Long): RecoveryCode?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(codes: List<RecoveryCode>)

    @Update
    suspend fun update(code: RecoveryCode)

    @Delete
    suspend fun delete(code: RecoveryCode)

    @Query("UPDATE recovery_codes SET isUsed = 1, usedAt = :usedAt WHERE id = :id")
    suspend fun markUsed(id: Long, usedAt: Long = System.currentTimeMillis())

    @Query("UPDATE recovery_codes SET isUsed = 0, usedAt = NULL WHERE id = :id")
    suspend fun markUnused(id: Long)

    @Query("SELECT COUNT(*) FROM recovery_codes WHERE appEntryId = :appEntryId AND isUsed = 0")
    fun getRemainingCount(appEntryId: Long): Flow<Int>
}
