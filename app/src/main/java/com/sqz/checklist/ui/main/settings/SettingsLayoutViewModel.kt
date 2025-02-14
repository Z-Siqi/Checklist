package com.sqz.checklist.ui.main.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class SettingsLayoutViewModel : ViewModel() {
    private var _isSearch by mutableStateOf(false)

    fun requestSearch() {
        _isSearch = true
    }

    fun getSearchState(): Boolean {
        return _isSearch
    }

    fun resetSearchState() {
        _isSearch = false
    }
}
