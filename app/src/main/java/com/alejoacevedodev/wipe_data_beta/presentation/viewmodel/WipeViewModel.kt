package com.alejoacevedodev.wipe_data_beta.presentation.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alejoacevedodev.wipe_data_beta.domain.model.WipeMethod
import com.alejoacevedodev.wipe_data_beta.domain.usecase.GetLogsUseCase
import com.alejoacevedodev.wipe_data_beta.domain.usecase.PerformWipeUseCase
import com.alejoacevedodev.wipe_data_beta.utils.FtpPrefs
import com.alejoacevedodev.wipe_data_beta.utils.FtpUploader
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
 * Estado unificado de la UI.
 */

@HiltViewModel
class WipeViewModel @Inject constructor(
    private val application: Application,
    private val performWipeUseCase: PerformWipeUseCase,
    private val getLogsUseCase: GetLogsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WipeUiState())
    val uiState = _uiState.asStateFlow()

    // Flujo de logs para historial (opcional)
    val logs = getLogsUseCase()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Cargar configuración FTP al iniciar
        loadFtpConfig()
    }

    fun setLoginUser(name: String) {
        _uiState.update { it.copy(userName = name) }
    }

    // ========================================================================
    // 1. GESTIÓN DE CARPETAS (SAF)
    // ========================================================================

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

    // ========================================================================
    // 2. SELECCIÓN DE MÉTODO
    // ========================================================================

    fun selectMethod(method: WipeMethod) {
        _uiState.update { it.copy(selectedMethod = method) }
    }

    // ========================================================================
    // 3. EJECUCIÓN DEL BORRADO
    // ========================================================================

    fun executeWipe() {
        val folders = _uiState.value.selectedFolders
        val method = _uiState.value.selectedMethod ?: WipeMethod.NIST_SP_800_88

        if (folders.isEmpty()) return

        val startTime = System.currentTimeMillis()

        // Reseteamos contadores e iniciamos estado de carga
        _uiState.update {
            it.copy(
                isWiping = true,
                wipeFinished = false,
                deletedCount = 0,
                deletedFilesList = emptyList(),
                freedBytes = 0L,
                wipeStartTime = startTime,
                wipeEndTime = 0
            )
        }

        viewModelScope.launch {
            val allDeletedFiles = ArrayList<String>()
            var totalBytesAccumulated = 0L

            try {
                for (folderUri in folders) {
                    val folderName = getFileNameFromUri(folderUri)
                    _uiState.update { it.copy(currentWipingFile = folderName) }

                    // El caso de uso retorna WipeResult con la lista y el peso liberado
                    val result = performWipeUseCase(folderUri, method, folderName)

                    allDeletedFiles.addAll(result.deletedFiles)
                    totalBytesAccumulated += result.freedBytes
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                val endTime = System.currentTimeMillis()

                // Actualizamos estado final con resultados
                _uiState.update {
                    it.copy(
                        isWiping = false,
                        currentWipingFile = "",
                        selectedFolders = emptyList(), // Limpiamos selección
                        wipeFinished = true,
                        deletedCount = allDeletedFiles.size,
                        deletedFilesList = allDeletedFiles, // Lista completa para el PDF
                        freedBytes = totalBytesAccumulated, // Peso total real
                        wipeEndTime = endTime
                    )
                }
            }
        }
    }

    // ========================================================================
    // 4. GENERACIÓN DE PDF Y SUBIDA FTP
    // ========================================================================

    fun generatePdf() {
        val state = _uiState.value

        // Validación de seguridad para fechas
        val minValidTime = 1704067200000L // Enero 1, 2024
        val safeStart = if (state.wipeStartTime > minValidTime) state.wipeStartTime else System.currentTimeMillis()
        val safeEnd = if (state.wipeEndTime > minValidTime) state.wipeEndTime else System.currentTimeMillis()

        // 1. Generar el PDF
        val pdfFile = PdfGenerator.generateReportPdf(
            context = application,
            operatorName = state.userName,
            methodName = state.selectedMethod?.name ?: "NIST",
            startTime = safeStart,
            endTime = safeEnd,
            deletedCount = state.deletedCount,
            deletedFiles = state.deletedFilesList, // Pasamos la lista detallada
            freedBytes = state.freedBytes          // Pasamos el peso total liberado
        )

        // 2. Subir al FTP si existe configuración y el archivo se creó
        if (pdfFile != null && pdfFile.exists()) {
            if (state.ftpHost.isNotEmpty() && state.ftpUser.isNotEmpty()) {
                viewModelScope.launch {
                    Toast.makeText(application, "Subiendo a FTP...", Toast.LENGTH_SHORT).show()

                    val portInt = state.ftpPort.toIntOrNull() ?: 21

                    val uploaded = FtpUploader.uploadFile(
                        file = pdfFile,
                        host = state.ftpHost,
                        user = state.ftpUser,
                        pass = state.ftpPass,
                        port = portInt
                    )

                    if (uploaded) {
                        Toast.makeText(application, "Reporte subido a la nube exitosamente", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(application, "Fallo la subida FTP (Guardado local)", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(application, "PDF guardado localmente (FTP no configurado)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Resetea solo la bandera de finalización para permitir nueva navegación.
     */
    fun resetWipeStatus() {
        _uiState.update { it.copy(wipeFinished = false) }
    }

    // ========================================================================
    // 5. CONFIGURACIÓN FTP
    // ========================================================================

    private fun loadFtpConfig() {
        val host = FtpPrefs.getHost(application)
        val user = FtpPrefs.getUser(application)
        val pass = FtpPrefs.getPass(application)
        val port = FtpPrefs.getPort(application)

        _uiState.update {
            it.copy(
                ftpHost = host,
                ftpPort = port.toString(),
                ftpUser = user,
                ftpPass = pass
            )
        }
    }

    fun updateFtpHost(value: String) { _uiState.update { it.copy(ftpHost = value) } }

    fun updateFtpPort(value: String) {
        // Solo permitir números
        if (value.all { it.isDigit() }) {
            _uiState.update { it.copy(ftpPort = value) }
        }
    }

    fun updateFtpUser(value: String) { _uiState.update { it.copy(ftpUser = value) } }
    fun updateFtpPass(value: String) { _uiState.update { it.copy(ftpPass = value) } }

    fun saveFtpConfig() {
        val s = _uiState.value
        val portInt = s.ftpPort.toIntOrNull() ?: 21
        FtpPrefs.saveConfig(application, s.ftpHost, portInt, s.ftpUser, s.ftpPass)
        Toast.makeText(application, "Configuración FTP Guardada", Toast.LENGTH_SHORT).show()
    }

    // ========================================================================
    // HELPERS
    // ========================================================================

    private fun getFileNameFromUri(uri: Uri): String {
        // Manejo seguro de Tree URIs
        val queryUri = if (DocumentsContract.isTreeUri(uri)) {
            DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        } else {
            uri
        }

        return try {
            application.contentResolver.query(queryUri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) c.getString(idx) else null
                } else null
            } ?: uri.lastPathSegment ?: "Desconocido"
        } catch (e: Exception) {
            uri.lastPathSegment ?: "Desconocido"
        }
    }
}