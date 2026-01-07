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

    fun updateFtpHost(value: String) = _uiState.update { it.copy(ftpHost = value) }
    fun updateFtpUser(value: String) = _uiState.update { it.copy(ftpUser = value) }
    fun updateFtpPass(value: String) = _uiState.update { it.copy(ftpPass = value) }

    fun saveFtpConfig() {
        val s = _uiState.value
        val portInt = s.ftpPort.toIntOrNull() ?: 21
        FtpPrefs.saveConfig(application, s.ftpHost, portInt, s.ftpUser, s.ftpPass)
    }
}