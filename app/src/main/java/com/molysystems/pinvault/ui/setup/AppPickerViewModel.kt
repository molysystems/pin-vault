package com.molysystems.pinvault.ui.setup

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.molysystems.pinvault.data.model.AppEntry
import com.molysystems.pinvault.data.repository.CredentialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class InstalledApp(
    val packageName: String,
    val displayName: String
)

@HiltViewModel
class AppPickerViewModel @Inject constructor(
    private val repository: CredentialRepository
) : ViewModel() {

    private val _allApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val filteredApps: StateFlow<List<InstalledApp>> = _filteredApps.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** ID of newly created AppEntry, consumed once to navigate away */
    private val _createdAppId = MutableStateFlow<Long?>(null)
    val createdAppId: StateFlow<Long?> = _createdAppId.asStateFlow()

    fun loadInstalledApps(pm: PackageManager) {
        viewModelScope.launch {
            _isLoading.value = true
            val apps = withContext(Dispatchers.IO) {
                val existingPackages = repository.getAllPackageNames().toSet()
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { info ->
                        // Show only user-installed and launchable apps, exclude already-linked
                        info.packageName !in existingPackages &&
                            pm.getLaunchIntentForPackage(info.packageName) != null &&
                            (info.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                    }
                    .map { info ->
                        InstalledApp(
                            packageName = info.packageName,
                            displayName = pm.getApplicationLabel(info).toString()
                        )
                    }
                    .sortedBy { it.displayName }
            }
            _allApps.value = apps
            applyFilter()
            _isLoading.value = false
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    private fun applyFilter() {
        val query = _searchQuery.value
        _filteredApps.value = if (query.isBlank()) {
            _allApps.value
        } else {
            _allApps.value.filter { it.displayName.contains(query, ignoreCase = true) }
        }
    }

    fun selectApp(app: InstalledApp) {
        viewModelScope.launch {
            val entry = AppEntry(
                packageName = app.packageName,
                displayName = app.displayName
            )
            val newId = repository.insertApp(entry)
            _createdAppId.value = newId
        }
    }

    fun consumeCreatedAppId() {
        _createdAppId.value = null
    }
}
