package com.molysystems.pinvault.ui.main

import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.molysystems.pinvault.data.model.AppEntry
import com.molysystems.pinvault.data.repository.CredentialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: CredentialRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val apps: StateFlow<List<AppEntry>> = combine(
        repository.getAllApps(),
        _searchQuery
    ) { apps, query ->
        if (query.isBlank()) apps
        else apps.filter { it.displayName.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deleteApp(appEntry: AppEntry) {
        viewModelScope.launch {
            repository.deleteApp(appEntry)
        }
    }

    fun getAppIcon(packageName: String, pm: PackageManager) =
        runCatching { pm.getApplicationIcon(packageName) }.getOrNull()
}
