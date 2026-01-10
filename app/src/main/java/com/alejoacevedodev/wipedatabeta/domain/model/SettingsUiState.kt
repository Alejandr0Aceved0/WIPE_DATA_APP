package com.alejoacevedodev.wipedatabeta.domain.model

/**
 * Copyright 2026 Wireless&Mobile, Inc. All rights reserved
 * File SettingsUiState 1.2.5 6/01/26
 *
 * @author Carlos Alejandro Acevedo Espitia
 * @version 1.2.5 6/01/26
 * @since 1.2.5
 */

data class SettingsUiState(
    val ftpHost: String = "",
    val ftpUser: String = "",
    val ftpPass: String = "",
    val ftpPort: String = "21",
    // Estados de validación
    val hostError: Boolean = false,
    val userError: Boolean = false,
    val passError: Boolean = false,
    val portError: Boolean = false
)