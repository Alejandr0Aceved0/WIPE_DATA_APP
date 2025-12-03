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
import com.alejoacevedodev.wipedatabeta.data.model.WipeResult
import com.alejoacevedodev.wipedatabeta.domain.model.WipeMethod
import com.alejoacevedodev.wipedatabeta.domain.repository.ILogRepository
import com.alejoacevedodev.wipedatabeta.domain.usecase.GetLogsUseCase
import com.alejoacevedodev.wipedatabeta.domain.usecase.PerformWipeUseCase
import com.alejoacevedodev.wipedatabeta.utils.FtpPrefs
import com.alejoacevedodev.wipedatabeta.utils.FtpUploader
import com.alejoacevedodev.wipedatabeta.utils.PdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import javax.inject.Inject
import kotlin.math.pow

// NOTA: Se asumen las interfaces y data classes: WipeUiState, WipeMethod, WipeLog, ILogRepository, WipeResult
// ------------------------------------------------------------------------

@HiltViewModel
class WipeViewModel @Inject constructor(
    private val application: Application,
    private val performWipeUseCase: PerformWipeUseCase,
    private val getLogsUseCase: GetLogsUseCase,
    private val logRepository: ILogRepository
) : ViewModel() {

    // --- ESTADOS ---
    private val _uiState = MutableStateFlow(WipeUiState())
    val uiState = _uiState.asStateFlow()

    private val SHIZUKU_REQUEST_CODE = 100
    private val _isShizukuPermitted = MutableStateFlow(false)
    val isShizukuPermitted = _isShizukuPermitted.asStateFlow()

    // Almacena pesos de paquetes auditados
    private val packageWeights: MutableMap<String, Long> = mutableMapOf()

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
        // loadFtpConfig()
        Shizuku.addRequestPermissionResultListener(permissionListener)
    }

    // --- MANEJO DE ESTADO BÁSICO ---
    fun setLoginUser(name: String) { _uiState.update { it.copy(userName = name) } }
    fun selectMethod(method: WipeMethod) { _uiState.update { it.copy(selectedMethod = method) } }
    fun resetWipeStatus() { _uiState.update { it.copy(wipeFinished = false) } }

    // ------------------------------------------------------------------------
    // 1. MANEJO DE LISTAS Y SELECCIÓN
    // ------------------------------------------------------------------------

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
    fun onPackageSelected(packageName: String) {
        if (packageName.isNotBlank()) {
            _uiState.update { currentState ->
                currentState.copy(packagesToWipe = currentState.packagesToWipe + packageName)
            }
        }
    }
    fun onRemovePackage(packageName: String) {
        _uiState.update { currentState ->
            currentState.copy(packagesToWipe = currentState.packagesToWipe - packageName)
        }
    }

    // ------------------------------------------------------------------------
    // 2. BORRADO MAESTRO (CONCURRENCIA Y CONTROL DE ESTADO)
    // ------------------------------------------------------------------------

    fun requestShizukuPermission() {
        if (!Shizuku.pingBinder()) { /* ... */ return }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
        } else {
            _isShizukuPermitted.value = true
        }
    }

    // 🔑 FUNCIÓN MAESTRA (Única llamada desde ConfirmationScreen)
    fun startWipeProcess() = viewModelScope.launch(Dispatchers.IO) {
        val state = _uiState.value
        val packages = state.packagesToWipe
        val folders = state.selectedFolders
        if (folders.isEmpty() && packages.isEmpty()) return@launch

        // 1. INICIAR ESTADO DE CARGA
        _uiState.update { it.copy(isWiping = true, wipeFinished = false, freedBytes = 0L) }

        var totalBytesAccumulated = 0L
        var overallSuccess = true
        val startTime = System.currentTimeMillis()
        val allDeletedFiles = mutableListOf<String>()
        val selectedMethod = state.selectedMethod ?: WipeMethod.NIST_SP_800_88

        try {
            // --- FASE 1: BORRADO SAF (Carpetas) ---
            if (state.selectedFolders.isNotEmpty()) {

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

                            val result = performWipeUseCase(folderUri, selectedMethod, folderName)

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
                                deletedCount = it.deletedCount + allDeletedFiles.size,
                                deletedFilesList = allDeletedFiles,
                                freedBytes = totalBytesAccumulated,
                                wipeEndTime = endTime
                            )
                        }
                    }
                }


            }

            // --- FASE 2: BORRADO SHIZUKU (Paquetes) ---
            if (state.packagesToWipe.isNotEmpty()) {
                val resultShizuku = executeShizukuMultiWipeAudit(state.packagesToWipe, selectedMethod)

                allDeletedFiles.addAll(resultShizuku.deletedFiles)
                totalBytesAccumulated += resultShizuku.freedBytes
            }

        } catch (e: Exception) {
            overallSuccess = false
            Log.e("WipeProcess", "Fallo Maestro: ${e.message}")
        } finally {
            val endTime = System.currentTimeMillis()

            // 2. LOGGING FINAL Y ACTUALIZACIÓN DE ESTADO
            _uiState.update {
                it.copy(
                    isWiping = false, // 🛑 APAGAR VISTA DE CARGA
                    wipeFinished = overallSuccess,
                    deletedCount = allDeletedFiles.size,
                    deletedFilesList = allDeletedFiles,
                    freedBytes = totalBytesAccumulated,
                    wipeEndTime = endTime
                )
            }
            logAndToast(application, "Proceso de borrado completado.", isError = !overallSuccess)
        }
    }

    /**
     * Ejecuta pm clear para todos los paquetes en paralelo, audita el peso, y suma el total.
     */
    private suspend fun executeShizukuMultiWipeAudit(packages: List<String>, auditMethod: WipeMethod): WipeResult {
        // 1. AUDITORÍA PREVIA (Obtener pesos para todos los paquetes antes de borrar)
        auditPackageWeights(packages) // Popula el packageWeights Map

        var totalFreedBytes = 0L
        val successLogs = mutableListOf<String>()

        try {
            coroutineScope {
                val deferredClears = packages.map { packageName ->
                    async(Dispatchers.IO) {
                        val size = packageWeights[packageName] ?: 0L

                        // 🔑 EJECUCIÓN DEL CLEAR
                        val success = performPmClear(packageName)

                        if (success) {
                            // Guardar log en DB y preparar entrada para PDF
//                            logRepository.saveLog(WipeLog(packageName, auditMethod, System.currentTimeMillis(), "SUCCESS (Liberado: ${formatFileSize(size)})"))
                            successLogs.add("PAQUETE LIMPIADO: $packageName. Tamaño: ${formatFileSize(size)}")
                            size // Retorna el peso si tuvo éxito
                        } else {
                            0L
                        }
                    }
                }

                totalFreedBytes = deferredClears.awaitAll().sum()
            }
        } catch (e: Exception) {
            Log.e("ShizukuMultiWipe", "Fallo en ejecución: ${e.message}")
        }

        return WipeResult(deletedFiles = successLogs, freedBytes = totalFreedBytes)
    }

    // ------------------------------------------------------------------------
    // 3. SHIZUKU HELPERS
    // ------------------------------------------------------------------------

    private suspend fun performPmClear(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val command = arrayOf("sh", "-c", "pm clear $packageName")
        try {
            val process = Shizuku.newProcess(command, null, null)
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText().trim() }
            val exitCode = process.waitFor()
            return@withContext exitCode == 0 && output == "Success"
        } catch (e: Exception) {
            Log.e("PmClear", "Excepción al ejecutar pm clear para $packageName: ${e.message}")
            return@withContext false
        }
    }

    private suspend fun auditPackageWeights(packages: List<String>) {
        packageWeights.clear()
        packages.forEach { packageName ->
            val sizeInBytes = getPackageSizeInBytes(packageName)
            if (sizeInBytes > 0) {
                packageWeights[packageName] = sizeInBytes
            }
        }
    }

    private suspend fun getPackageSizeInBytes(packageName: String): Long = withContext(Dispatchers.IO) {
//        val command = arrayOf("sh", "-c", "dumpsys package $packageName")
        val command = arrayOf("sh", "-c", "dumpsys package $packageName")
        try {
            val process = Shizuku.newProcess(command, null, null)
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            process.waitFor()

            var totalSize = 0L
            val codeSizeRegex = "codeSize=(\\d+)".toRegex()
            val dataSizeRegex = "dataSize=(\\d+)".toRegex()
            val cacheSizeRegex = "cacheSize=(\\d+)".toRegex()

            codeSizeRegex.find(output)?.groupValues?.get(1)?.toLongOrNull()?.let { totalSize += it }
            dataSizeRegex.find(output)?.groupValues?.get(1)?.toLongOrNull()?.let { totalSize += it }
            cacheSizeRegex.find(output)?.groupValues?.get(1)?.toLongOrNull()?.let { totalSize += it }

            return@withContext totalSize
        } catch (e: Exception) {
            Log.e("ShizukuSize", "Fallo al ejecutar dumpsys para $packageName: ${e.message}")
            return@withContext 0L
        }
    }

    fun validatePackageExists(packageName: String): Boolean {
        // Comando: pm path <package_name>
        val command = arrayOf("sh", "-c", "pm path $packageName")

        return try {
            // Ejecución síncrona del comando (bloquea la corrutina)
            val process = Shizuku.newProcess(command, null, null)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val outputLine = reader.readLine()
            process.waitFor()

            // El paquete existe si la salida no es nula y comienza con "package:"
            val exists = outputLine != null && outputLine.startsWith("package:")

            if (!exists) {
                // Mostrar un Toast si la validación falla (para el usuario)
                logAndToast(application, "Error: El paquete '$packageName' no fue encontrado.", isError = true)
            }
            exists

        } catch (e: Exception) {
            // Esto captura fallos de ejecución de Shizuku
            Log.e("PackageValidation", "Fallo al validar con Shizuku: ${e.message}")
            logAndToast(application, "Error interno al validar paquete. Ver Logcat.", isError = true)
            false
        }
    }
    // --- OTROS HELPERS ---
    private fun logAndToast(context: Context, message: String, isError: Boolean) {
        if (isError) {
            Log.e("WipeViewModel", message)
        } else {
            Log.i("WipeViewModel", message)
        }
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, if (isError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        }
    }
    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (kotlin.math.log10(bytes.toDouble()) / kotlin.math.log10(1024.0)).toInt() // Se usa log10 y pow
        return String.format(
            Locale.getDefault(),
            "%.2f %s",
            bytes / 1024.0.pow(digitGroups.toDouble()),
            units[digitGroups]
        )
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
    // WipeViewModel.kt (Función generatePdf)

    fun generatePdf() {
        val state = _uiState.value

        // 1. LÓGICA DE FECHAS Y SEGURIDAD
        val minValidTime = 1704067200000L // Placeholder de seguridad (Enero 1, 2024)
        val safeStart =
            if (state.wipeStartTime > minValidTime) state.wipeStartTime else System.currentTimeMillis()
        val safeEnd =
            if (state.wipeEndTime > minValidTime) state.wipeEndTime else System.currentTimeMillis()

        // 2. GENERAR EL PDF (Punto de convergencia de los datos)
        val pdfFile = PdfGenerator.generateReportPdf(
            context = application,
            operatorName = state.userName,
            methodName = state.selectedMethod?.name ?: "NIST",
            startTime = safeStart,
            endTime = safeEnd,
            deletedCount = state.deletedCount,
            deletedFiles = state.deletedFilesList, // SAF logs o log simulado
            freedBytes = state.freedBytes,
            packageWeights = state.packageWeights, // 🔑 MAPA DE PESOS SHIZUKU
            packagesToWipe = state.packagesToWipe // Lista de paquetes
        )

        // 3. MANEJO DEL ARCHIVO GENERADO (FTP y Toast)
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
        } else {
            Toast.makeText(
                application,
                "Fallo al generar el reporte PDF.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun checkShizukuStatus() {
        // 1. Verificar si el binder está vivo (Shizuku App está activo)
        val isBinderActive = Shizuku.pingBinder()

        // 2. Verificar si el permiso ya fue concedido a esta app.
        val permissionResult = Shizuku.checkSelfPermission()

        val isGranted = isBinderActive && (permissionResult == PackageManager.PERMISSION_GRANTED)

        // 3. Actualizar el estado del ViewModel.
        _isShizukuPermitted.value = isGranted
    }

    fun resetUiState() {
        _uiState.value = WipeUiState()
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

    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else true
    }

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
                val output = BufferedReader(InputStreamReader(process.inputStream)).use {
                    it.readText().trim()
                }
                val error = BufferedReader(InputStreamReader(process.errorStream)).use {
                    it.readText().trim()
                }
                val exitCode = process.waitFor()

                // 2. Reportar resultado
                Handler(Looper.getMainLooper()).post {
                    if (exitCode == 0 && error.isEmpty()) {
                        Log.i("ShizukuMove", "Comando de movimiento EXITOSO.")
                        Toast.makeText(
                            context,
                            "Movimiento EXITOSO a Android/media.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        val errorMessage =
                            if (error.isNotBlank()) error else "Fallo al mover datos. Código: $exitCode. Output: $output"
                        Log.e("ShizukuMove", errorMessage)
                        Toast.makeText(
                            context,
                            "MOVIMIENTO FALLIDO. Ver Logcat.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                logAndToast(context, "Excepción al ejecutar comando: ${e.message}", isError = true)
            }
        }
    }

    fun isPackageSelected(isPackageSelected: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(isPackageSelected = isPackageSelected)
        }
    }

    fun isFolderSelected(isFolderSelected: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(isFolderSelected = isFolderSelected)
        }
    }
}