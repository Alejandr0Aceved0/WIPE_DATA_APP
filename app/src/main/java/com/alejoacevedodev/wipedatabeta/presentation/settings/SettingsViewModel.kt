package com.alejoacevedodev.wipedatabeta.presentation.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import com.alejoacevedodev.wipedatabeta.domain.model.SettingsUiState
import com.alejoacevedodev.wipedatabeta.utils.FtpPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Copyright 2026 Wireless&Mobile, Inc. All rights reserved
 * File SettingsViewModel 1.2.5 6/01/26
 *
 * @author Carlos Alejandro Acevedo Espitia
 * @version 1.2.5 6/01/26
 * @since 1.2.5
 */

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Al instanciar el ViewModel, cargamos lo que haya en SharedPreferences
        loadSavedConfig()
    }

    private fun loadSavedConfig() {
        _uiState.update {
            it.copy(
                ftpHost = FtpPrefs.getHost(application),
                ftpPort = FtpPrefs.getPort(application).toString(),
                ftpUser = FtpPrefs.getUser(application),
                ftpPass = FtpPrefs.getPass(application)
            )
        }
    }

    // Actualizamos el valor y de paso limpiamos el error si el usuario empieza a escribir
    fun updateFtpHost(value: String) = _uiState.update { it.copy(ftpHost = value, hostError = false) }
    fun updateFtpPort(value: String) = _uiState.update { it.copy(ftpPort = value, portError = false) }
    fun updateFtpUser(value: String) = _uiState.update { it.copy(ftpUser = value, userError = false) }
    fun updateFtpPass(value: String) = _uiState.update { it.copy(ftpPass = value, passError = false) }

    fun saveFtpConfig(): Boolean {
        val s = _uiState.value

        // Reglas de validación
        val isHostValid = s.ftpHost.isNotBlank()
        val isUserValid = s.ftpUser.isNotBlank()
        val isPassValid = s.ftpPass.isNotBlank()
        val portInt = s.ftpPort.toIntOrNull()
        val isPortValid = portInt != null && portInt in 1..65535

        if (isHostValid && isUserValid && isPassValid && isPortValid) {
            FtpPrefs.saveConfig(application, s.ftpHost, portInt!!, s.ftpUser, s.ftpPass)
            return true // Indicar a la UI que puede navegar atrás
        } else {
            // Si algo falla, marcamos los errores para que se vean en rojo
            _uiState.update {
                it.copy(
                    hostError = !isHostValid,
                    userError = !isUserValid,
                    passError = !isPassValid,
                    portError = !isPortValid
                )
            }
            return false
        }
    }
}