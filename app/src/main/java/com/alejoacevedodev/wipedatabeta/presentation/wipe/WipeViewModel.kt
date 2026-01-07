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

        val sourcePath = "/storage/emulated/0/Android/data/*"
        val mediaDir = "/storage/emulated/0/Android/media"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Verificar si existe Android/media
                val checkCommand = arrayOf("sh", "-c", "[ -d \"$mediaDir\" ] && echo EXISTS || echo NO")
                val checkProcess = Shizuku.newProcess(checkCommand, null, null)

                val existsOutput = BufferedReader(InputStreamReader(checkProcess.inputStream)).use {
                    it.readText().trim()
                }
                checkProcess.waitFor()

                // 2. Si no existe, crearla con mkdir -p
                if (existsOutput != "EXISTS") {
                    val mkdirCommand = arrayOf("sh", "-c", "mkdir -p \"$mediaDir\"")
                    val mkdirProcess = Shizuku.newProcess(mkdirCommand, null, null)
                    val mkdirExit = mkdirProcess.waitFor()

                    if (mkdirExit != 0) {
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                context,
                                "Error creando carpeta Android/media",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@launch
                    }
                }

                // 3. Ejecutar mv
                val fullShellCommand = "mv $sourcePath \"$mediaDir\""
                val moveCommand = arrayOf("sh", "-c", fullShellCommand)
                val process = Shizuku.newProcess(moveCommand, null, null)

                val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText().trim() }
                val error = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText().trim() }
                val exitCode = process.waitFor()

                Handler(Looper.getMainLooper()).post {
                    if (exitCode == 0 && error.isEmpty()) {
                        Toast.makeText(
                            context,
                            "Movimiento COMPLETADO a Android/media.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Error moviendo datos. Ver Logcat.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {
                logAndToast(context, "Excepción: ${e.message}", isError = true)
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
            Toast.makeText(context, message, if (isError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
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
    fun selectMethod(method: WipeMethod) { _uiState.update { it.copy(selectedMethod = method) } }

    fun resetWipeStatus() { _uiState.update { it.copy(wipeFinished = false) } }

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

        _uiState.update { it.copy(
            isWiping = false,
            wipeFinished = true,
            freedBytes = totalBytes,
            deletedFilesList = allLogs,
            wipeEndTime = System.currentTimeMillis()
        )}
    }

    fun generatePdf() {
        // Lógica de PDF usando PdfGenerator...
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