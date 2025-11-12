package com.alejoacevedodev.wipe_data_beta.presentation.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alejoacevedodev.wipe_data_beta.domain.model.WipeMethod
import com.alejoacevedodev.wipe_data_beta.domain.usecase.GetLogsUseCase
import com.alejoacevedodev.wipe_data_beta.domain.usecase.PerformWipeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WipeViewModel @Inject constructor(
    private val application: Application,
    private val performWipeUseCase: PerformWipeUseCase,
    private val getLogsUseCase: GetLogsUseCase
) : ViewModel() {

    val logs = getLogsUseCase().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Estado de la UI
    private val _uiState = MutableStateFlow(WipeUiState())
    val uiState = _uiState.asStateFlow()

    fun onFolderSelected(uri: Uri) {
        val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            application.contentResolver.takePersistableUriPermission(uri, flag)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        _uiState.update { it.copy(selectedUri = uri) }
    }

    fun onWipeMethodSelected(method: WipeMethod) {
        _uiState.update { it.copy(selectedMethod = method) }
    }

    fun onWipeClicked() {
        val uri = _uiState.value.selectedUri ?: return
        val method = _uiState.value.selectedMethod
        val fileName = getFileNameFromUri(uri)

        _uiState.update { it.copy(isWiping = true) }

        viewModelScope.launch {
            // ¡Llama al Caso de Uso!
            performWipeUseCase(uri, method, fileName)

            _uiState.update { it.copy(isWiping = false, selectedUri = null) }
        }
    }


    private fun getFileNameFromUri(uri: Uri): String {
        // Usa el ContentResolver (disponible desde 'application') para consultar
        // el nombre del archivo.
        try {
            application.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                // Si el cursor es válido y se mueve al primer (y único) resultado
                if (cursor.moveToFirst()) {

                    // Busca el índice de la columna "display name"
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex == -1) {
                        // Si no se encuentra, retorna la última parte del URI como fallback
                        return uri.lastPathSegment ?: "unknown"
                    }

                    // Retorna el nombre del archivo
                    return cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return uri.lastPathSegment ?: "unknown"
    }
}

data class WipeUiState(
    val selectedUri: Uri? = null,
    val selectedMethod: WipeMethod = WipeMethod.NIST_SP_800_88, // Default
    val isWiping: Boolean = false
)