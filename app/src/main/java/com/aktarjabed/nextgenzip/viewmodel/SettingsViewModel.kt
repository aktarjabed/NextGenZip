package com.aktarjabed.nextgenzip.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.nextgenzip.data.DataStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _compressionLevel = MutableStateFlow(5)
    val compressionLevel: StateFlow<Int> = _compressionLevel

    private val _splitSize = MutableStateFlow(0L)
    val splitSize: StateFlow<Long> = _splitSize

    private val _password = MutableStateFlow<String?>(null)
    val password: StateFlow<String?> = _password

    init {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            _compressionLevel.value = DataStoreManager.getCompressionLevel(ctx).first()
            _splitSize.value = DataStoreManager.getSplitSize(ctx).first()
            _password.value = DataStoreManager.getPassword(ctx).first()
        }
    }

    fun setCompressionLevel(level: Int) {
        _compressionLevel.value = level
        viewModelScope.launch {
            DataStoreManager.setCompressionLevel(getApplication(), level)
        }
    }

    fun setSplitSize(size: Long) {
        _splitSize.value = size
        viewModelScope.launch {
            DataStoreManager.setSplitSize(getApplication(), size)
        }
    }

    fun setPassword(pass: String?) {
        _password.value = pass
        viewModelScope.launch {
            DataStoreManager.setPassword(getApplication(), pass)
        }
    }
}
