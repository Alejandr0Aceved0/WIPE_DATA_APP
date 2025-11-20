package com.alejoacevedodev.wipe_data_beta.presentation.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alejoacevedodev.wipe_data_beta.domain.model.WipeMethod
import com.alejoacevedodev.wipe_data_beta.domain.usecase.GetLogsUseCase
import com.alejoacevedodev.wipe_data_beta.domain.usecase.PerformWipeUseCase
import com.alejoacevedodev.wipe_data_beta.utils.PdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado de la UI.
 */

@HiltViewModel
class WipeViewModel @Inject constructor(
    private val application: Application,
    private val performWipeUseCase: PerformWipeUseCase,
    private val getLogsUseCase: GetLogsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WipeUiState())
    val uiState = _uiState.asStateFlow()

    val logs = getLogsUseCase()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- 1. SELECCIÓN DE CARPETAS ---

    fun onFolderSelected(uri: Uri) {
        val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            application.contentResolver.takePersistableUriPermission(uri, flag)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        _uiState.update { currentState ->
            if (uri !in currentState.selectedFolders) {
                currentState.copy(selectedFolders = currentState.selectedFolders + uri)
            } else {
                currentState
            }
        }
    }

    fun onRemoveFolder(uri: Uri) {
        val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            application.contentResolver.releasePersistableUriPermission(uri, flag)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        _uiState.update { it.copy(selectedFolders = it.selectedFolders - uri) }
    }

    // --- 2. SELECCIÓN DE MÉTODO (¡ESTA ES LA QUE TE FALTABA!) ---

    /**
     * Guarda el método seleccionado en el estado para usarlo después en la confirmación.
     */
    fun selectMethod(method: WipeMethod) {
        _uiState.update { it.copy(selectedMethod = method) }
    }

    // --- 3. EJECUCIÓN DEL BORRADO ---

    /**
     * Ejecuta el borrado final usando las carpetas y el método guardados.
     */
    fun executeWipe() {
        val folders = _uiState.value.selectedFolders
        // Si no hay método seleccionado, usamos NIST por defecto
        val method = _uiState.value.selectedMethod ?: WipeMethod.NIST_SP_800_88

        if (folders.isEmpty()) return

        _uiState.update {
            it.copy(isWiping = true, wipeFinished = false, deletedCount = 0)
        }

        viewModelScope.launch {
            var count = 0
            try {
                for (folderUri in folders) {
                    val folderName = getFileNameFromUri(folderUri)
                    _uiState.update { it.copy(currentWipingFile = folderName) }

                    performWipeUseCase(folderUri, method, folderName)
                    count++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _uiState.update {
                    it.copy(
                        isWiping = false,
                        currentWipingFile = "",
                        selectedFolders = emptyList(), // Limpiamos la lista
                        wipeFinished = true,
                        deletedCount = count
                    )
                }
            }
        }
    }

    /**
     * Resetea el estado de "finalizado" para evitar que el mensaje de éxito salga repetido.
     */
    fun resetWipeStatus() {
        _uiState.update { it.copy(wipeFinished = false, deletedCount = 0) }
    }

    fun generatePdf() {
        val state = _uiState.value
        PdfGenerator.generateReportPdf(
            context = application,
            methodName = state.selectedMethod?.name ?: "NIST",
            startTime = state.wipeStartTime,
            endTime = state.wipeEndTime,
            deletedCount = state.deletedCount
        )
    }

    // --- HELPERS ---

    private fun getFileNameFromUri(uri: Uri): String {
        val queryUri = if (DocumentsContract.isTreeUri(uri)) {
            DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        } else {
            uri
        }
        return try {
            application.contentResolver.query(queryUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) cursor.getString(index) else null
                } else null
            } ?: uri.lastPathSegment ?: "Desconocido"
        } catch (e: Exception) {
            uri.lastPathSegment ?: "Desconocido"
        }
    }
}