package com.molysystems.pinvault.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.molysystems.pinvault.data.model.AppEntry
import com.molysystems.pinvault.data.model.CredentialField
import com.molysystems.pinvault.data.model.RecoveryCode
import com.molysystems.pinvault.data.repository.CredentialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val repository: CredentialRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val appEntryId: Long = checkNotNull(savedStateHandle["appEntryId"])

    private val _app = MutableStateFlow<AppEntry?>(null)
    val app: StateFlow<AppEntry?> = _app.asStateFlow()

    val credentialFields: StateFlow<List<CredentialField>> =
        repository.getCredentialFields(appEntryId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recoveryCodes: StateFlow<List<RecoveryCode>> =
        repository.getRecoveryCodes(appEntryId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val remainingCodeCount: StateFlow<Int> =
        repository.getRemainingRecoveryCodeCount(appEntryId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        viewModelScope.launch {
            _app.value = repository.getApp(appEntryId)
        }
    }

    fun addCredentialField(label: String, plainValue: String) {
        if (label.isBlank() || plainValue.isBlank()) return
        viewModelScope.launch {
            val nextOrder = credentialFields.value.size
            repository.insertCredentialField(appEntryId, label, plainValue, nextOrder)
        }
    }

    fun updateCredentialField(field: CredentialField, newLabel: String, newPlainValue: String) {
        viewModelScope.launch {
            if (newPlainValue.isBlank()) {
                repository.updateCredentialFieldLabel(field, newLabel)
            } else {
                repository.updateCredentialField(field, newLabel, newPlainValue)
            }
        }
    }

    fun deleteCredentialField(field: CredentialField) {
        viewModelScope.launch {
            repository.deleteCredentialField(field)
        }
    }

    fun addRecoveryCodes(serviceLabel: String, rawCodes: String) {
        if (serviceLabel.isBlank() || rawCodes.isBlank()) return
        viewModelScope.launch {
            repository.insertRecoveryCodes(appEntryId, serviceLabel, rawCodes)
        }
    }

    fun markCodeUsed(codeId: Long) {
        viewModelScope.launch {
            repository.markRecoveryCodeUsed(codeId)
        }
    }

    fun markCodeUnused(codeId: Long) {
        viewModelScope.launch {
            repository.markRecoveryCodeUnused(codeId)
        }
    }

    fun deleteRecoveryCode(code: RecoveryCode) {
        viewModelScope.launch {
            repository.deleteRecoveryCode(code)
        }
    }

    fun decryptField(field: CredentialField): String =
        runCatching { repository.decryptField(field) }.getOrElse { "Decryption failed" }

    fun decryptRecoveryCode(code: RecoveryCode): String =
        runCatching { repository.decryptRecoveryCode(code) }.getOrElse { "Decryption failed" }
}
