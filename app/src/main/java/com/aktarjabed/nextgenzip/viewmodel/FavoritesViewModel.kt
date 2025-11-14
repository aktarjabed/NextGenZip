package com.aktarjabed.nextgenzip.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.nextgenzip.data.FavoritesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FavoritesRepository(application)

    val favoriteArchives: StateFlow<List<String>> = repository.favoriteArchives
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addFavorite(path: String) {
        viewModelScope.launch {
            repository.addFavorite(path)
        }
    }

    fun removeFavorite(path: String) {
        viewModelScope.launch {
            repository.removeFavorite(path)
        }
    }
}
