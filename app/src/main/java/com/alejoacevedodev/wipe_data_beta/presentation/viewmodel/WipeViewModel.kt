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

    init {
        loadFtpConfig()
    }

    // --- CONFIGURACIÓN FTP ---

    private fun loadFtpConfig() {
        val host = FtpPrefs.getHost(application)
        val port = FtpPrefs.getPort(application)
        val user = FtpPrefs.getUser(application)
        val pass = FtpPrefs.getPass(application)

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
    // Filtramos para que solo sean números
    fun updateFtpPort(value: String) {
        if (value.all { it.isDigit() }) {
            _uiState.update { it.copy(ftpPort = value) }
        }
    }
    fun updateFtpUser(value: String) { _uiState.update { it.copy(ftpUser = value) } }
    fun updateFtpPass(value: String) { _uiState.update { it.copy(ftpPass = value) } }

    fun saveFtpConfig() {
        val s = _uiState.value
        // Convertir puerto a Int, default 21 si está vacío
        val portInt = s.ftpPort.toIntOrNull() ?: 21

        FtpPrefs.saveConfig(application, s.ftpHost, s.ftpUser, s.ftpPass)
        Toast.makeText(application, "Configuración FTP Guardada", Toast.LENGTH_SHORT).show()
    }

    // --- LÓGICA DE BORRADO (EXISTENTE) ---

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
        _uiState.update { it.copy(isWiping = true, wipeFinished = false, deletedCount = 0, wipeStartTime = startTime) }

        viewModelScope.launch {
            var totalItemsDeleted = 0
            try {
                for (folderUri in folders) {
                    val folderName = getFileNameFromUri(folderUri)
                    _uiState.update { it.copy(currentWipingFile = folderName) }
                    totalItemsDeleted += performWipeUseCase(folderUri, method, folderName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                val endTime = System.currentTimeMillis()
                _uiState.update {
                    it.copy(isWiping = false, currentWipingFile = "", selectedFolders = emptyList(), wipeFinished = true, deletedCount = totalItemsDeleted, wipeEndTime = endTime)
                }
            }
        }
    }

    fun generatePdf() {
        val state = _uiState.value
        val minValidTime = 1704067200000L
        val safeStart = if (state.wipeStartTime > minValidTime) state.wipeStartTime else System.currentTimeMillis()
        val safeEnd = if (state.wipeEndTime > minValidTime) state.wipeEndTime else System.currentTimeMillis()

        val pdfFile = PdfGenerator.generateReportPdf(
            context = application,
            methodName = state.selectedMethod?.name ?: "NIST",
            startTime = safeStart,
            endTime = safeEnd,
            deletedCount = state.deletedCount
        )

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
                        port = portInt // Pasamos el puerto dinámico
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

    fun resetWipeStatus() {
        _uiState.update { it.copy(wipeFinished = false, deletedCount = 0) }
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