package com.alejoacevedodev.wipedatabeta.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alejoacevedodev.wipedatabeta.domain.model.WipeMethod
import com.alejoacevedodev.wipedatabeta.domain.usecase.GetLogsUseCase
import com.alejoacevedodev.wipedatabeta.domain.usecase.PerformWipeUseCase
import com.alejoacevedodev.wipedatabeta.utils.FtpPrefs
import com.alejoacevedodev.wipedatabeta.utils.FtpUploader
import com.alejoacevedodev.wipedatabeta.utils.PdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject

@HiltViewModel
class WipeViewModel @Inject constructor(
    private val application: Application,
    private val performWipeUseCase: PerformWipeUseCase,
    private val getLogsUseCase: GetLogsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WipeUiState())
    val uiState = _uiState.asStateFlow()

    private val SHIZUKU_REQUEST_CODE = 100
    private val _isShizukuPermitted = MutableStateFlow(false)
    val isShizukuPermitted = _isShizukuPermitted.asStateFlow()

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_REQUEST_CODE) {
                _isShizukuPermitted.value = grantResult == PackageManager.PERMISSION_GRANTED
                if (!_isShizukuPermitted.value) {
                    Toast.makeText(application, "Permiso de Shizuku denegado.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    init {
        loadFtpConfig()
        Shizuku.addRequestPermissionResultListener(permissionListener)
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

                    val result = performWipeUseCase(folderUri, method, folderName)

                    allDeletedFiles.addAll(result.deletedFiles)
                    totalBytesAccumulated += result.freedBytes
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
                        deletedCount = allDeletedFiles.size,
                        deletedFilesList = allDeletedFiles,
                        freedBytes = totalBytesAccumulated,
                        wipeEndTime = endTime
                    )
                }
            }
        }
    }

    // ========================================================================
    // 4. EJECUCIÓN DEL BORRADO (SHIZUKU/ADB - pm clear)
    // ========================================================================
    fun requestShizukuPermission() {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(
                application,
                "Shizuku no está activo. Por favor, inícialo.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
        } else {
            _isShizukuPermitted.value = true
        }
    }

    fun executeShizukuWipe(packageName: String) {
        if (!isShizukuPermitted.value) {
            Toast.makeText(application, "Permiso de Shizuku no otorgado.", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(application, "Iniciando limpieza con newProcess...", Toast.LENGTH_LONG).show()
        executePmClearWithNewProcess(packageName)
    }


    // ========================================================================
    // 5. GENERACIÓN DE PDF Y SUBIDA FTP
    // ========================================================================

    fun generatePdf() {
        val state = _uiState.value

        val minValidTime = 1704067200000L
        val safeStart =
            if (state.wipeStartTime > minValidTime) state.wipeStartTime else System.currentTimeMillis()
        val safeEnd =
            if (state.wipeEndTime > minValidTime) state.wipeEndTime else System.currentTimeMillis()

        val pdfFile = PdfGenerator.generateReportPdf(
            context = application,
            operatorName = state.userName,
            methodName = state.selectedMethod?.name ?: "NIST",
            startTime = safeStart,
            endTime = safeEnd,
            deletedCount = state.deletedCount,
            deletedFiles = state.deletedFilesList,
            freedBytes = state.freedBytes
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
                        port = portInt
                    )

                    if (uploaded) {
                        Toast.makeText(
                            application,
                            "Reporte subido a la nube exitosamente",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            application,
                            "Fallo la subida FTP (Guardado local)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else {
                Toast.makeText(
                    application,
                    "PDF guardado localmente (FTP no configurado)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun resetWipeStatus() {
        _uiState.update { it.copy(wipeFinished = false) }
    }

    // ========================================================================
    // 6. CONFIGURACIÓN FTP Y HELPERS
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

    fun updateFtpHost(value: String) {
        _uiState.update { it.copy(ftpHost = value) }
    }

    fun updateFtpUser(value: String) {
        _uiState.update { it.copy(ftpUser = value) }
    }

    fun updateFtpPass(value: String) {
        _uiState.update { it.copy(ftpPass = value) }
    }

    fun saveFtpConfig() {
        val s = _uiState.value
        val portInt = s.ftpPort.toIntOrNull() ?: 21
        FtpPrefs.saveConfig(application, s.ftpHost, portInt, s.ftpUser, s.ftpPass)
        Toast.makeText(application, "Configuración FTP Guardada", Toast.LENGTH_SHORT).show()
    }

    private fun getFileNameFromUri(uri: Uri): String {
        val queryUri = if (DocumentsContract.isTreeUri(uri)) {
            DocumentsContract.buildDocumentUriUsingTree(
                uri,
                DocumentsContract.getTreeDocumentId(uri)
            )
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

    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else true
    }


    private fun executePmClearWithNewProcess(packageName: String) {
        if (!isShizukuPermitted.value) {
            Toast.makeText(application, "Permiso de Shizuku no otorgado.", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val command = arrayOf("pm", "clear", packageName)

            try {
                val process = Shizuku.newProcess(command, null, null)
                val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText().trim() }
                val error = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText().trim() }
                val exitCode = process.waitFor()
                Handler(Looper.getMainLooper()).post {
                    if (exitCode == 0 && output == "Success") {
                        Log.i("ShizukuNewProcess", "Limpieza de $packageName EXITOSA. Output: $output")
                        Toast.makeText(application, "Limpieza de $packageName ÉXITO.", Toast.LENGTH_LONG).show()
                    } else {
                        Log.e("ShizukuNewProcess", "Limpieza de $packageName FALLIDA. Error: $error, Exit: $exitCode")
                        Toast.makeText(application, "Limpieza de $packageName FALLÓ. Detalles en Logcat.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ShizukuNewProcess", "Excepción: ${e.message}")
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(application, "Error al ejecutar proceso: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    /**
    * Mueve (mv) el contenido de la carpeta de datos de la aplicación en /Android/data
    * a una carpeta de destino en el almacenamiento compartido.
    */
    fun moveAllDataToMedia(context: Context) {
        if (!isShizukuPermitted.value) {
            logAndToast(context, "Permiso de Shizuku no otorgado.", isError = true)
            return
        }

        // 🔑 DESGLOSE DEL COMANDO PARA Shizuku.newProcess
        val sourcePath = "/storage/emulated/0/Android/data/*"
        val destPath = "/storage/emulated/0/Android/media"
        val fullShellCommand = "mv $sourcePath $destPath"

// 🔑 CORRECCIÓN CLAVE: Usar sh -c para forzar la expansión del comodín
        val command = arrayOf(
            "sh",
            "-c",
            fullShellCommand // El comando completo va como un solo argumento
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Ejecutar el movimiento (mv) con Shizuku.newProcess
                val process = Shizuku.newProcess(command, null, null)
                val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText().trim() }
                val error = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText().trim() }
                val exitCode = process.waitFor()

                // 2. Reportar resultado
                Handler(Looper.getMainLooper()).post {
                    if (exitCode == 0 && error.isEmpty()) {
                        Log.i("ShizukuMove", "Comando de movimiento EXITOSO.")
                        Toast.makeText(context, "Movimiento EXITOSO a Android/media.", Toast.LENGTH_LONG).show()
                    } else {
                        val errorMessage = if (error.isNotBlank()) error else "Fallo al mover datos. Código: $exitCode. Output: $output"
                        Log.e("ShizukuMove", errorMessage)
                        Toast.makeText(context, "MOVIMIENTO FALLIDO. Ver Logcat.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                logAndToast(context, "Excepción al ejecutar comando: ${e.message}", isError = true)
            }
        }
    }

    // Función helper para manejar logs y toasts
    private fun logAndToast(context: Context, message: String, isError: Boolean) {
        if (isError) {
            Log.e("ShizukuMove", message)
        } else {
            Log.i("ShizukuMove", message)
        }
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, if (isError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        }
    }
}