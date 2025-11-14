package com.aktarjabed.nextgenzip.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.nextgenzip.data.HistoryRepository
import com.aktarjabed.nextgenzip.data.models.ArchiveHistoryEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HistoryRepository(application)

    val archiveHistory: StateFlow<List<ArchiveHistoryEntry>> = repository.archiveHistory
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addArchiveHistory(entry: ArchiveHistoryEntry) {
        viewModelScope.launch {
            repository.addEntry(entry)
        }
    }
}
