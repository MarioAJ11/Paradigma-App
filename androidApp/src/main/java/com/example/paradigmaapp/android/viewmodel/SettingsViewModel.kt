package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.android.data.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de Ajustes.
 * Gestiona las preferencias del usuario, como la activación del stream por defecto
 * y la selección del tema de la aplicación (claro, oscuro o seguir al sistema).
 *
 * @property appPreferences Instancia de [AppPreferences] para guardar y cargar las preferencias.
 *
 * @author Mario Alguacil Juárez
 */
class SettingsViewModel(private val appPreferences: AppPreferences) : ViewModel() {

    // StateFlow para la preferencia de si el streaming debe estar activo al iniciar la app.
    private val _isStreamActive = MutableStateFlow(appPreferences.loadIsStreamActive())
    val isStreamActive: StateFlow<Boolean> = _isStreamActive.asStateFlow()

    // StateFlow para la preferencia manual del tema:
    // null = seguir al sistema, true = oscuro manual, false = claro manual.
    private val _isManuallySetToDarkTheme = MutableStateFlow<Boolean?>(appPreferences.loadIsManuallySetDarkTheme())
    val isManuallySetToDarkTheme: StateFlow<Boolean?> = _isManuallySetToDarkTheme.asStateFlow()

    /**
     * Alterna la preferencia de si el streaming debe estar activo al iniciar la app
     * y guarda el nuevo estado.
     */
    fun toggleStreamActive() {
        val newState = !_isStreamActive.value
        appPreferences.saveIsStreamActive(newState)
        _isStreamActive.value = newState
    }

    /**
     * Establece la preferencia manual del tema de la aplicación y guarda el nuevo estado.
     *
     * @param isDark `true` para seleccionar tema oscuro, `false` para tema claro,
     * o `null` para indicar que se debe seguir la configuración del sistema.
     */
    fun setThemePreference(isDark: Boolean?) {
        appPreferences.saveIsManuallySetDarkTheme(isDark)
        _isManuallySetToDarkTheme.value = isDark
    }
}