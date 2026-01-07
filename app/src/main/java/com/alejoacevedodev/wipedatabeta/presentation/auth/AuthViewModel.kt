package com.alejoacevedodev.wipedatabeta.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alejoacevedodev.wipedatabeta.data.dataStore.SessionDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Copyright 2026 Wireless&Mobile, Inc. All rights reserved
 * File AuthViewModel 1.2.5 6/01/26
 *
 * @author Carlos Alejandro Acevedo Espitia
 * @version 1.2.5 6/01/26
 * @since 1.2.5
 */

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    val isLoggedIn = sessionDataStore.isLoggedInFlow.stateIn(
        viewModelScope, SharingStarted.Eagerly, false
    )

    fun loginSuccess(username: String) {
        viewModelScope.launch {
            sessionDataStore.setLoggedIn(true)
            // Aquí podrías guardar el nombre en el DataStore si lo necesitas
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionDataStore.setLoggedIn(false)
        }
    }
}