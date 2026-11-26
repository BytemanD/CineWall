package com.cinewall.app.ui.screen.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinewall.app.repository.ConfigRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConfigViewModel(private val rep: ConfigRepository) : ViewModel() {

    val autoSubtitle: StateFlow<Boolean> = rep.autoSubtitle.get().stateIn(viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000), initialValue = rep.autoScanAfterStarted.default)
    val autoScanAfterStarted: StateFlow<Boolean> = rep.autoScanAfterStarted.get().stateIn(viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000), initialValue = rep.autoScanAfterStarted.default)
    val autoScrapAfterScan: StateFlow<Boolean> = rep.autoScrapAfterScan.get().stateIn(viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000), initialValue = rep.autoScrapAfterScan.default)
    val autoScrollDelay: StateFlow<Int> = rep.autoScrollDelay.get().stateIn(viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000), initialValue = rep.autoScrollDelay.default)

    fun updateAutoSubtitle(value: Boolean) {
        viewModelScope.launch { rep.autoSubtitle.set(value) }
    }
    fun updateAutoScrapAfterScan(value: Boolean) {
        viewModelScope.launch { rep.autoScrapAfterScan.set(value) }
    }
    suspend fun load() {
        autoSubtitle.first()
        autoScanAfterStarted.first()
        autoScrapAfterScan.first()
        autoScrollDelay.first()
    }
}