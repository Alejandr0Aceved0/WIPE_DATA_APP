package com.alejoacevedodev.wipe_data_beta.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alejoacevedodev.wipe_data_beta.presentation.viewmodel.WipeViewModel

@Composable
fun WipeScreen(
    modifier: Modifier = Modifier,
    viewModel: WipeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    // <<< LA CORRECCIÓN ESTÁ AQUÍ >>>
    // 1. Copia los valores de estado a variables locales inmutables
    val selectedUri = state.selectedUri
    val isWiping = state.isWiping

    // 1. Lanzador de SAF para seleccionar CARPETA
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.onFolderSelected(uri)
            }
        }
    )

    // 2. Lanzador de SAF para seleccionar ARCHIVO (opcional)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.onFolderSelected(uri) // Lo tratamos igual
            }
        }
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = { folderPickerLauncher.launch(null) }) {
            Text("Seleccionar Carpeta")
        }

        // <<< CORRECCIÓN AQUÍ >>>
        // 2. Usa la variable local 'selectedUri'
        if (selectedUri != null) {
            Text("Seleccionado: ${selectedUri.path}")
        }

        // Aquí iría un Dropdown para seleccionar el método
        // ...

        Button(
            onClick = { viewModel.onWipeClicked() },
            // <<< CORRECCIÓN AQUÍ >>>
            // 3. Usa las variables locales 'selectedUri' y 'isWiping'
            enabled = selectedUri != null && !isWiping
        ) {
            // 'isWiping' también es una variable local ahora, es más limpio
            if (isWiping) {
                CircularProgressIndicator()
            } else {
                Text("BORRAR AHORA")
            }
        }
    }
}