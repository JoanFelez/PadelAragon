package com.padelaragon.shared.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.padelaragon.shared.data.sync.DataSourceMode
import com.padelaragon.shared.data.sync.LeagueCatalog
import com.padelaragon.shared.data.sync.SyncEngine
import com.padelaragon.shared.data.sync.SyncSettings
import com.padelaragon.shared.data.sync.SyncState
import com.padelaragon.shared.di.MultiLeagueContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SyncSettings,
    private val engine: SyncEngine,
    private val catalog: LeagueCatalog
) : ViewModel() {

    data class UiState(
        val mode: DataSourceMode,
        val backendUrl: String,
        val appKey: String,
        /** True when the build ships default backend settings that the fields above override. */
        val hasEffectiveBackend: Boolean
    )

    private val _ui = MutableStateFlow(read())
    val ui: StateFlow<UiState> = _ui.asStateFlow()
    val syncState: StateFlow<SyncState> = engine.state

    private fun read() = UiState(
        mode = settings.mode,
        backendUrl = settings.urlOverride,
        appKey = settings.keyOverride,
        hasEffectiveBackend = settings.backendConfig.isComplete
    )

    fun setMode(mode: DataSourceMode) {
        settings.mode = mode
        _ui.update { read() }
    }

    /** Blank values clear the override and fall back to the build defaults. */
    fun saveBackend(url: String, key: String) {
        settings.urlOverride = url
        settings.keyOverride = key
        _ui.update { read() }
        syncNow()
    }

    fun syncNow() {
        if (engine.state.value.isSyncing) return
        viewModelScope.launch {
            catalog.refresh()
            engine.syncAll(catalog.leagues.value)
        }
    }
}

class SettingsViewModelFactory(private val container: MultiLeagueContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: kotlin.reflect.KClass<T>, extras: CreationExtras): T {
        require(modelClass == SettingsViewModel::class) { "Unknown ViewModel class: $modelClass" }
        return SettingsViewModel(container.syncSettings, container.syncEngine, container.leagueCatalog) as T
    }
}
