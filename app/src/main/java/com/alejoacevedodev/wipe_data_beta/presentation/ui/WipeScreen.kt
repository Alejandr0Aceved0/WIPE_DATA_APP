package com.alejoacevedodev.wipe_data_beta.presentation.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alejoacevedodev.wipe_data_beta.presentation.viewmodel.WipeViewModel

@Composable
fun WipeScreen(
    modifier: Modifier = Modifier,
    viewModel: WipeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val selectedFolders = state.selectedFolders
    val isWiping = state.isWiping

    // 1. Lanzador de SAF para seleccionar CARPETA (ÁRBOL)
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.onFolderSelected(uri)
            }
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 2. Botón para AÑADIR carpetas
        Button(
            onClick = { folderPickerLauncher.launch(null) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Añadir Carpeta para Borrar")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Carpetas Seleccionadas:", style = MaterialTheme.typography.titleMedium)

        // 3. Lista de carpetas seleccionadas
        Box(modifier = Modifier.weight(1f)) {
            if (selectedFolders.isEmpty()) {
                Text(
                    "Añade las carpetas que deseas borrar (ej. DCIM, Downloads...)",
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(selectedFolders, key = { it.toString() }) { uri ->
                        FolderItem(
                            uri = uri,
                            onRemove = { viewModel.onRemoveFolder(uri) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Botón de Borrado (ahora borra la lista)
        Button(
            onClick = { viewModel.onWipeClicked() },
            enabled = selectedFolders.isNotEmpty() && !isWiping,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isWiping) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("BORRAR TODO LO SELECCIONADO")
            }
        }

        // Aquí puedes añadir el LazyColumn de los Logs
        // ...
    }
}

/**
 * Un Composable para mostrar un item de la lista de carpetas
 */
@Composable
private fun FolderItem(
    uri: Uri,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Mostramos la última parte del path como nombre
            Text(
                text = uri.path?.substringAfterLast('/') ?: "Carpeta desconocida",
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Quitar carpeta",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}