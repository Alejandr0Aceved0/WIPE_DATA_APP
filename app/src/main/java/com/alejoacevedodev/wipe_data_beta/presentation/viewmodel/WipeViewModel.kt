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
import android.provider.DocumentsContract
import android.provider.DocumentsContract as SupportDocumentsContract

/**
 * Define el estado de la UI para la pantalla de borrado.
 * Maneja una lista de carpetas (URIs) en lugar de una sola.
 */

@HiltViewModel
class WipeViewModel @Inject constructor(
    private val application: Application, // Se usa para el ContentResolver
    private val performWipeUseCase: PerformWipeUseCase,
    private val getLogsUseCase: GetLogsUseCase
) : ViewModel() {

    // --- ESTADOS ---

    // Estado principal de la UI (carpetas seleccionadas, método, etc.)
    private val _uiState = MutableStateFlow(WipeUiState())
    val uiState = _uiState.asStateFlow()

    // Flujo de los logs de la base de datos
    val logs = getLogsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    // --- MANEJO DE EVENTOS DE LA UI ---

    /**
     * Se llama cuando el usuario selecciona una carpeta desde el selector SAF.
     * Toma permiso persistente y añade el URI a la lista de estado.
     */
    fun onFolderSelected(uri: Uri) {
        // 1. Toma el permiso persistente para que podamos borrar dentro de esta carpeta
        val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            application.contentResolver.takePersistableUriPermission(uri, flag)
        } catch (e: SecurityException) {
            e.printStackTrace()
            // (Opcional: mostrar un error al usuario)
            return
        }

        // 2. Añade el URI a la lista (evitando duplicados)
        _uiState.update { currentState ->
            if (uri in currentState.selectedFolders) {
                currentState // No hacer nada si ya está en la lista
            } else {
                val updatedList = currentState.selectedFolders + uri
                currentState.copy(selectedFolders = updatedList)
            }
        }
    }

    /**
     * Se llama cuando el usuario presiona el icono de borrar en un item de la lista.
     * Libera el permiso persistente y quita el URI de la lista.
     */
    fun onRemoveFolder(uri: Uri) {
        // 1. Libera el permiso que tomamos
        val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            application.contentResolver.releasePersistableUriPermission(uri, flag)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        // 2. Quita el URI de la lista
        _uiState.update { currentState ->
            val updatedList = currentState.selectedFolders - uri
            currentState.copy(selectedFolders = updatedList)
        }
    }

    /**
     * Actualiza el método de borrado seleccionado en el estado.
     */
    fun onWipeMethodSelected(method: WipeMethod) {
        _uiState.update { it.copy(selectedMethod = method) }
    }

    /**
     * Inicia el proceso de borrado.
     * Itera sobre todas las carpetas seleccionadas y llama al caso de uso.
     */
    fun onWipeClicked() {
        val foldersToWipe = _uiState.value.selectedFolders
        val method = _uiState.value.selectedMethod

        if (foldersToWipe.isEmpty()) return

        _uiState.update { it.copy(isWiping = true) }

        viewModelScope.launch {
            try {
                // Itera sobre cada carpeta y la borra
                for (folderUri in foldersToWipe) {
                    val folderName = getFileNameFromUri(folderUri)
                    performWipeUseCase(folderUri, method, folderName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // (Opcional: mostrar error al usuario)
            } finally {
                // Al terminar (o fallar), limpia la lista y detiene el progreso
                _uiState.update {
                    it.copy(
                        isWiping = false,
                        selectedFolders = emptyList() // Limpia la lista
                    )
                }
            }
        }
    }

    // --- FUNCIONES PRIVADAS DE AYUDA ---

    /**
     * Obtiene el nombre visible de una carpeta/archivo a partir de su URI de SAF.
     */
    private fun getFileNameFromUri(uri: Uri): String {
        // --- INICIO DE LA CORRECCIÓN ---
        // 1. Convertir el Tree URI (si es uno) a un Document URI
        val queryUri = if (SupportDocumentsContract.isTreeUri(uri)) {
            SupportDocumentsContract.buildDocumentUriUsingTree(uri, SupportDocumentsContract.getTreeDocumentId(uri))
        } else {
            uri
        }
        // --- FIN DE LA CORRECCIÓN ---

        try {
            // 2. Usar el 'queryUri' (que ahora es un Document URI)
            application.contentResolver.query(queryUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        return cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Fallback
        return uri.lastPathSegment ?: "Carpeta desconocida"
    }
}