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

    fun onFolderSelected(uri: Uri) {
        val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            application.contentResolver.takePersistableUriPermission(uri, flag)
        } catch (e: SecurityException) { e.printStackTrace() }

        _uiState.update { s ->
            if (uri !in s.selectedFolders) s.copy(selectedFolders = s.selectedFolders + uri) else s
        }
    }

    fun onRemoveFolder(uri: Uri) {
        val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            application.contentResolver.releasePersistableUriPermission(uri, flag)
        } catch (e: SecurityException) { e.printStackTrace() }
        _uiState.update { it.copy(selectedFolders = it.selectedFolders - uri) }
    }

    fun selectMethod(method: WipeMethod) {
        _uiState.update { it.copy(selectedMethod = method) }
    }

    fun executeWipe() {
        val folders = _uiState.value.selectedFolders
        val method = _uiState.value.selectedMethod ?: WipeMethod.NIST_SP_800_88

        if (folders.isEmpty()) return

        val startTime = System.currentTimeMillis()

        _uiState.update {
            it.copy(
                isWiping = true,
                wipeFinished = false,
                deletedCount = 0,
                wipeStartTime = startTime
            )
        }

        viewModelScope.launch {
            var totalItemsDeleted = 0
            try {
                for (folderUri in folders) {
                    val folderName = getFileNameFromUri(folderUri)
                    _uiState.update { it.copy(currentWipingFile = folderName) }

                    val count = performWipeUseCase(folderUri, method, folderName)
                    totalItemsDeleted += count
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                val endTime = System.currentTimeMillis()
                _uiState.update {
                    it.copy(
                        isWiping = false,
                        currentWipingFile = "",
                        selectedFolders = emptyList(),
                        wipeFinished = true,
                        deletedCount = totalItemsDeleted, // Guardamos el total
                        wipeEndTime = endTime
                    )
                }
            }
        }
    }

    fun generatePdf() {
        val state = _uiState.value

        // --- LÓGICA DE SEGURIDAD PARA FECHAS ---
        // Si por alguna razón las fechas son 0 o muy antiguas (ej. 1970), usamos la fecha actual.
        // 1704067200000L corresponde a Enero 1, 2024.
        val minValidTime = 1704067200000L

        val finalStart = if (state.wipeStartTime > minValidTime) state.wipeStartTime else System.currentTimeMillis()
        // Si el fin es inválido, usamos el inicio + 2 segundos para que no se vea raro
        val finalEnd = if (state.wipeEndTime > minValidTime) state.wipeEndTime else finalStart + 2000

        PdfGenerator.generateReportPdf(
            context = application,
            methodName = state.selectedMethod?.name ?: "NIST",
            startTime = finalStart,
            endTime = finalEnd,
            deletedCount = state.deletedCount
        )
    }

    /**
     * CORREGIDO: Solo resetea la bandera 'wipeFinished'.
     * NO borra 'deletedCount' para que el reporte y el PDF puedan mostrar el número.
     */
    fun resetWipeStatus() {
        _uiState.update {
            it.copy(
                wipeFinished = false
                // deletedCount = 0  <-- ELIMINAMOS ESTO PARA NO PERDER EL DATO
            )
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        val queryUri = if (DocumentsContract.isTreeUri(uri)) {
            DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        } else { uri }
        return try {
            application.contentResolver.query(queryUri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) c.getString(idx) else null
                } else null
            } ?: uri.lastPathSegment ?: "Desconocido"
        } catch (e: Exception) { uri.lastPathSegment ?: "Desconocido" }
    }
}