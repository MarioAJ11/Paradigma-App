package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.android.data.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val appPreferences: AppPreferences) : ViewModel() {

    // Para la preferencia de streaming (ya existente en AppPreferences)
    private val _isStreamActive = MutableStateFlow(appPreferences.loadIsStreamActive())
    val isStreamActive: StateFlow<Boolean> = _isStreamActive.asStateFlow()

    private val _isManuallySetToDarkTheme = MutableStateFlow<Boolean?>(appPreferences.loadIsManuallySetDarkTheme())
    val isManuallySetToDarkTheme: StateFlow<Boolean?> = _isManuallySetToDarkTheme.asStateFlow()

    fun toggleStreamActive() {
        val newState = !_isStreamActive.value
        appPreferences.saveIsStreamActive(newState)
        _isStreamActive.value = newState
    }

    fun setThemePreference(isDark: Boolean?) { // null para seguir al sistema
        appPreferences.saveIsManuallySetDarkTheme(isDark)
        _isManuallySetToDarkTheme.value = isDark
    }
}