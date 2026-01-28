package com.alejoacevedodev.wipedatabeta.presentation.wipe

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alejoacevedodev.wipedatabeta.domain.model.WipeMethod
import com.alejoacevedodev.wipedatabeta.domain.model.WipeUiState
import com.alejoacevedodev.wipedatabeta.domain.usecase.PerformWipeUseCase
import com.alejoacevedodev.wipedatabeta.utils.FtpUploader
import com.alejoacevedodev.wipedatabeta.utils.PdfGenerator
import com.alejoacevedodev.wipedatabeta.utils.ShizukuManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

@HiltViewModel
class WipeViewModel @Inject constructor(
    private val application: Application,
    private val performWipeUseCase: PerformWipeUseCase,
    private val shizukuManager: ShizukuManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WipeUiState())
    val uiState = _uiState.asStateFlow()

    fun resetUiState() {
        _uiState.value = WipeUiState()
    }

    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else true
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

    fun moveAllDataToMedia(context: Context) {
        if (!isShizukuPermitted.value) {
            logAndToast(context, "Permiso de Shizuku no otorgado.", isError = true)
            return
        }

        // Definimos las rutas.
        // Usamos /data/media/0 que es la ruta real para evitar problemas de permisos de FUSE
        val mediaDir = "/storage/emulated/0/Android/AcopioData"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Crear la carpeta de destino con permisos totales
                val setupCmd = "mkdir -p $mediaDir && chmod 777 $mediaDir"
                Shizuku.newProcess(arrayOf("sh", "-c", setupCmd), null, null).waitFor()

                // 2. Script de Shell para mover con seguridad
                // - Mueve contenido de /data/* (lo que Shizuku alcance a ver)
                // - Mueve contenido de /storage/emulated/0/* (evitando la carpeta destino)
                val moveScript = """
                # Mover datos de sdcard evitando bucles
                for item in /storage/emulated/0/*; do
                    if [ "${'$'}item" != "/storage/emulated/0/Android" ]; then
                        mv "${'$'}item" "$mediaDir/" 2>/dev/null
                    fi
                done

                # Mover contenido específico de Android (Data y OBB) a la nueva ubicación
                mv /storage/emulated/0/Android/data "$mediaDir/data_backup" 2>/dev/null
                mv /storage/emulated/0/Android/obb "$mediaDir/obb_backup" 2>/dev/null
                
                # Intentar con /data/ (solo si Shizuku tiene acceso root o privilegios de shell)
                mv /data/local/tmp/* "$mediaDir/tmp_backup" 2>/dev/null
            """.trimIndent()

                val process = Shizuku.newProcess(arrayOf("sh", "-c", moveScript), null, null)

                // Capturar logs para ver qué pasó
                val error =
                    BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
                val exitCode = process.waitFor()

                Handler(Looper.getMainLooper()).post {
                    if (exitCode == 0) {
                        Toast.makeText(context, "Movimiento finalizado.", Toast.LENGTH_LONG).show()
                    } else {
                        Log.w(
                            "WipeViewModel",
                            "Aviso: Algunos archivos no se movieron (Permisos): $error"
                        )
                        Toast.makeText(
                            context,
                            "Proceso completado con algunas restricciones.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {
                logAndToast(context, "Error: ${e.message}", isError = true)
            }
        }
    }

    private fun logAndToast(context: Context, message: String, isError: Boolean) {
        if (isError) {
            Log.e("WipeViewModel", message)
        } else {
            Log.i("WipeViewModel", message)
        }
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, if (isError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
                .show()
        }
    }

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

    fun selectMethod(method: WipeMethod) {
        _uiState.update { it.copy(selectedMethod = method) }
    }

    fun resetWipeStatus() {
        _uiState.update { currentState ->
            currentState.copy(
                isWiping = false,
                currentWipingFile = "",
                wipeFinished = false,
                deletedCount = 0,
                deletedFilesList = emptyList(),
                freedBytes = 0L,
                wipeStartTime = 0,
                wipeEndTime = 0,
                selectedFolders = emptyList(),
                packagesToWipe = emptyList(),
                isPackageSelected = false,
                isFolderSelected = false,
                isGeneratingPdf = false
            )
        }
    }

    // --- EJECUCIÓN MAESTRA ---
    fun startWipeProcess() = viewModelScope.launch(Dispatchers.IO) {
        val state = _uiState.value
        _uiState.update { it.copy(isWiping = true, wipeStartTime = System.currentTimeMillis()) }

        var totalBytes = 0L
        val allLogs = mutableListOf<String>()

        // 1. Borrado de Carpetas (SAF)
        state.selectedFolders.forEach { uri ->
            val result = performWipeUseCase(uri, state.selectedMethod!!, "Carpeta")
            totalBytes += result.freedBytes
            allLogs.addAll(result.deletedFiles)
        }

        // 2. Borrado de Paquetes (Shizuku)
        if (state.packagesToWipe.isNotEmpty()) {
            val result = shizukuManager.executeMultiWipe(state.packagesToWipe)
            totalBytes += result.freedBytes
            allLogs.addAll(result.deletedFiles)
        }

        _uiState.update {
            it.copy(
                isWiping = false,
                wipeFinished = true,
                freedBytes = totalBytes,
                deletedFilesList = allLogs,
                wipeEndTime = System.currentTimeMillis()
            )
        }
    }

    fun generatePdf() {
        viewModelScope.launch {
            // 1. Iniciar estado de carga
            _uiState.update { it.copy(isGeneratingPdf = true) }

            try {
                val state = _uiState.value
                // Lógica de tiempos de seguridad
                val safeStart =
                    if (state.wipeStartTime > 1704067200000L) state.wipeStartTime else System.currentTimeMillis()
                val safeEnd =
                    if (state.wipeEndTime > 1704067200000L) state.wipeEndTime else System.currentTimeMillis()

                // 2. Generar PDF en hilo IO
                val pdfFile = withContext(Dispatchers.IO) {
                    PdfGenerator.generateReportPdf(
                        context = application,
                        operatorName = state.userName,
                        methodName = state.selectedMethod?.name ?: "NIST",
                        startTime = safeStart,
                        endTime = safeEnd,
                        deletedCount = state.deletedCount,
                        deletedFiles = state.deletedFilesList,
                        freedBytes = state.freedBytes,
                        packageWeights = state.packageWeights,
                        packagesToWipe = state.packagesToWipe
                    )
                }

                if (pdfFile != null && pdfFile.exists()) {
                    // 3. Proceso de FTP (si aplica)
                    if (state.ftpHost.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(application, "Subiendo reporte...", Toast.LENGTH_SHORT)
                                .show()
                        }

                        val uploaded = withContext(Dispatchers.IO) {
                            FtpUploader.uploadFile(
                                file = pdfFile,
                                host = state.ftpHost,
                                user = state.ftpUser,
                                pass = state.ftpPass,
                                port = state.ftpPort.toIntOrNull() ?: 21
                            )
                        }

                        withContext(Dispatchers.Main) {
                            if (uploaded) Toast.makeText(
                                application,
                                "✅ Sincronizado con la nube",
                                Toast.LENGTH_SHORT
                            ).show()
                            else Toast.makeText(
                                application,
                                "⚠️ Guardado local (Fallo FTP)",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(application, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                // 4. Detener carga SIEMPRE (incluso si hay error)
                _uiState.update { it.copy(isGeneratingPdf = false) }
            }
        }
    }


    val isShizukuPermitted = _uiState.map { it.isShizukuPermitted }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // 2. Comprobar estado del servicio
    fun checkShizukuStatus() {
        val granted = shizukuManager.isPermissionGranted()
        _uiState.update { it.copy(isShizukuPermitted = granted) }
    }

    // 3. Solicitar permiso
    fun requestShizukuPermission() {
        if (shizukuManager.isServiceAvailable()) {
            shizukuManager.requestPermission(100) // Código de solicitud
        }
    }

    // 4. Validar si el paquete existe (Llamada al Manager)
    fun validatePackageExists(packageName: String): Boolean {
        return shizukuManager.checkIfPackageExists(packageName)
    }

    // 5. Gestión de la lista de paquetes
    fun onPackageSelected(packageName: String) {
        _uiState.update { it.copy(packagesToWipe = it.packagesToWipe + packageName) }
    }

    fun onRemovePackage(packageName: String) {
        _uiState.update { it.copy(packagesToWipe = it.packagesToWipe - packageName) }
    }
}