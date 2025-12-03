package com.alejoacevedodev.wipedatabeta.data.dataStore

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "session_prefs")

class SessionDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }

    // Flow<Boolean> con el estado actual
    val isLoggedInFlow = context.sessionDataStore.data.map { prefs ->
        prefs[IS_LOGGED_IN] ?: false
    }

    suspend fun setLoggedIn(value: Boolean) {
        context.sessionDataStore.edit { prefs ->
            prefs[IS_LOGGED_IN] = value
        }
    }
}