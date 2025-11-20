package com.alejoacevedodev.wipe_data_beta.presentation.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alejoacevedodev.wipe_data_beta.presentation.viewmodel.WipeViewModel

@Composable
fun OriginSelectionScreen(
    viewModel: WipeViewModel,
    onNavigateToMethods: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val HeaderBlue = Color(0xFF2B4C6F)
    val ButtonGray = Color(0xFFE0E0E0)

    // Launcher para abrir el selector de carpetas
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            if (uri != null) viewModel.onFolderSelected(uri)
        }
    )

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Header
        Box(
            modifier = Modifier.fillMaxWidth().height(56.dp).background(HeaderBlue).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("NULLUM Lite", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Salir", tint = Color.White, modifier = Modifier.align(Alignment.CenterEnd))
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Selección del origen de borrado",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(24.dp))

            // --- BOTONES DE ORIGEN (Abren el mismo picker por simplicidad en MVP) ---
            OriginCard2("Memoria Interna", ButtonGray) { folderPickerLauncher.launch(null) }
            Spacer(modifier = Modifier.height(12.dp))
            OriginCard2("Memoria Externa", ButtonGray) { folderPickerLauncher.launch(null) }
            Spacer(modifier = Modifier.height(12.dp))
            OriginCard2("Tarjeta SD", ButtonGray) { folderPickerLauncher.launch(null) }

            Spacer(modifier = Modifier.height(24.dp))

            // --- LISTA DE CARPETAS SELECCIONADAS ---
            if (state.selectedFolders.isNotEmpty()) {
                Text("Carpetas seleccionadas:", fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.selectedFolders, key = { it.toString() }) { uri ->
                        FolderItem(uri, onRemove = { viewModel.onRemoveFolder(uri) })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón Continuar
                Button(
                    onClick = onNavigateToMethods,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HeaderBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Continuar a Métodos", color = Color.White)
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Footer
            Text("Versión\n1.0.12.1", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
fun OriginCard2(title: String, color: Color, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = Modifier.fillMaxWidth().height(60.dp).clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Color.Black, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, fontSize = 16.sp, color = Color.Black)
        }
    }
}

@Composable
fun FolderItem(uri: Uri, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = uri.path?.substringAfterLast('/') ?: "...", modifier = Modifier.weight(1f), maxLines = 1)
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = Color.Red)
            }
        }
    }
}