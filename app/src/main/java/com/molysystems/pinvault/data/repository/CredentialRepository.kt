package com.molysystems.pinvault.data.repository

import com.molysystems.pinvault.data.crypto.CryptoManager
import com.molysystems.pinvault.data.db.AppEntryDao
import com.molysystems.pinvault.data.db.CredentialFieldDao
import com.molysystems.pinvault.data.db.RecoveryCodeDao
import com.molysystems.pinvault.data.model.AppEntry
import com.molysystems.pinvault.data.model.CredentialField
import com.molysystems.pinvault.data.model.RecoveryCode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialRepository @Inject constructor(
    private val appEntryDao: AppEntryDao,
    private val credentialFieldDao: CredentialFieldDao,
    private val recoveryCodeDao: RecoveryCodeDao,
    private val cryptoManager: CryptoManager
) {

    // ── AppEntry ──────────────────────────────────────────────────────────────

    fun getAllApps(): Flow<List<AppEntry>> = appEntryDao.getAllApps()

    suspend fun getApp(id: Long): AppEntry? = appEntryDao.getById(id)

    suspend fun getAppByPackage(packageName: String): AppEntry? =
        appEntryDao.getByPackageName(packageName)

    suspend fun getAllPackageNames(): List<String> = appEntryDao.getAllPackageNames()

    suspend fun insertApp(appEntry: AppEntry): Long = appEntryDao.insert(appEntry)

    suspend fun updateApp(appEntry: AppEntry) = appEntryDao.update(appEntry)

    suspend fun deleteApp(appEntry: AppEntry) = appEntryDao.delete(appEntry)

    suspend fun touchApp(id: Long) = appEntryDao.updateLastAccessed(id)

    // ── CredentialField ───────────────────────────────────────────────────────

    fun getCredentialFields(appEntryId: Long): Flow<List<CredentialField>> =
        credentialFieldDao.getByAppEntryId(appEntryId)

    suspend fun getCredentialFieldsOnce(appEntryId: Long): List<CredentialField> =
        credentialFieldDao.getByAppEntryIdOnce(appEntryId)

    /**
     * Encrypts [plainValue] and inserts the credential field.
     */
    suspend fun insertCredentialField(appEntryId: Long, label: String, plainValue: String, sortOrder: Int = 0): Long {
        val (iv, ciphertext) = cryptoManager.encrypt(plainValue)
        val field = CredentialField(
            appEntryId = appEntryId,
            label = label,
            encryptedValue = ciphertext,
            iv = iv,
            sortOrder = sortOrder
        )
        return credentialFieldDao.insert(field)
    }

    /**
     * Updates an existing field's label and value. Both are required.
     */
    suspend fun updateCredentialField(field: CredentialField, newLabel: String, newPlainValue: String) {
        val (iv, ciphertext) = cryptoManager.encrypt(newPlainValue)
        credentialFieldDao.update(
            field.copy(label = newLabel, encryptedValue = ciphertext, iv = iv)
        )
    }

    /**
     * Updates only the label, preserving the existing encrypted value.
     */
    suspend fun updateCredentialFieldLabel(field: CredentialField, newLabel: String) {
        credentialFieldDao.update(field.copy(label = newLabel))
    }

    suspend fun deleteCredentialField(field: CredentialField) = credentialFieldDao.delete(field)

    /**
     * Decrypts a credential field's value.
     */
    fun decryptField(field: CredentialField): String =
        cryptoManager.decrypt(field.iv, field.encryptedValue)

    // ── RecoveryCode ──────────────────────────────────────────────────────────

    fun getRecoveryCodes(appEntryId: Long): Flow<List<RecoveryCode>> =
        recoveryCodeDao.getByAppEntryId(appEntryId)

    fun getRemainingRecoveryCodeCount(appEntryId: Long): Flow<Int> =
        recoveryCodeDao.getRemainingCount(appEntryId)

    /**
     * Parses bulk-pasted codes (one per line) and inserts them encrypted.
     */
    suspend fun insertRecoveryCodes(appEntryId: Long, serviceLabel: String, rawCodes: String) {
        val lines = rawCodes.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val existing = recoveryCodeDao.getByAppEntryIdOnce(appEntryId)
        val nextOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1

        val entities = lines.mapIndexed { index, code ->
            val (iv, ciphertext) = cryptoManager.encrypt(code)
            RecoveryCode(
                appEntryId = appEntryId,
                serviceLabel = serviceLabel,
                encryptedCode = ciphertext,
                iv = iv,
                sortOrder = nextOrder + index
            )
        }
        recoveryCodeDao.insertAll(entities)
    }

    suspend fun markRecoveryCodeUsed(id: Long) = recoveryCodeDao.markUsed(id)

    suspend fun markRecoveryCodeUnused(id: Long) = recoveryCodeDao.markUnused(id)

    suspend fun deleteRecoveryCode(code: RecoveryCode) = recoveryCodeDao.delete(code)

    /**
     * Decrypts a recovery code's value.
     */
    fun decryptRecoveryCode(code: RecoveryCode): String =
        cryptoManager.decrypt(code.iv, code.encryptedCode)
}
