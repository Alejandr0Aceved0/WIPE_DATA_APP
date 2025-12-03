package com.alejoacevedodev.wipedatabeta.presentation.ui.screen

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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alejoacevedodev.wipedatabeta.R
import com.alejoacevedodev.wipedatabeta.presentation.ui.composables.ShizukuWipeSection
import com.alejoacevedodev.wipedatabeta.presentation.viewmodel.WipeViewModel

@Composable
fun OriginSelectionScreen(
    viewModel: WipeViewModel,
    onNavigateToMethods: () -> Unit,
    onNavigateHome: () -> Unit,
    onConfigureFtp: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val HeaderBlue = Color(0xFF2B4C6F)
    val ButtonGray = Color(0xFFE0E0E0)
    val context = LocalContext.current

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            if (uri != null) viewModel.onFolderSelected(uri)
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderBlue)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // 1. Título a la izquierda
            Text(
                text = "NULLUM Lite",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // 2. Fila para los iconos a la derecha
            Row(
                modifier = Modifier.align(Alignment.CenterEnd), // Alineamos toda la fila a la derecha
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Configuración
                IconButton(
                    onClick = onConfigureFtp
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Configurar FTP",
                        tint = Color.White
                    )
                }

                // Espacio entre los botones (Opcional, IconButton ya tiene padding interno)
                Spacer(modifier = Modifier.width(4.dp))

                // Botón Salir (Envuelto en IconButton para que sea clickeable y consistente)
                IconButton(
                    onClick = { onNavigateHome() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Salir",
                        tint = Color.White
                    )
                }
            }
        }

        // Contenido Principal
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 1. Título General
            item {
                Text(
                    "Selección del origen de borrado",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 2. Opción 1: BORRADO DE PAQUETES (SHIZUKU)
            item {
                ShizukuWipeSection(
                    viewModel = viewModel,
                    headerColor = HeaderBlue,
                    onNavigateToMethods = onNavigateToMethods
                )
            }

            // 3. Opción 2: BORRADO DE ARCHIVOS/CARPETAS (SAF)
            item {
                Text(
                    text = "Opción 2 – Borrado Seguro desde Archivos o Carpetas",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = HeaderBlue
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Botones de Origen
                OriginCard("Memoria Interna", ButtonGray) { folderPickerLauncher.launch(null) }
                Spacer(modifier = Modifier.height(12.dp))
                OriginCard("Memoria Externa", ButtonGray) { folderPickerLauncher.launch(null) }
                Spacer(modifier = Modifier.height(12.dp))
                OriginCard("Tarjeta SD", ButtonGray) { folderPickerLauncher.launch(null) }
                Spacer(modifier = Modifier.height(12.dp))
                // Botón MOVER ARCHIVOS
                OriginCard(
                    "MOVER ARCHIVOS",
                    ButtonGray
                ) { viewModel.moveAllDataToMedia(context = context) }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 4. Lista de Carpetas Seleccionadas (Si hay ítems)
            if (state.selectedFolders.isNotEmpty() || state.packagesToWipe.isNotEmpty()) {
                item {
                    Text(
                        text = "Carpetas seleccionadas:",
                        fontWeight = FontWeight.Bold,
                        color = HeaderBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(state.selectedFolders.toList(), key = { it.toString() }) { uri ->
                    FolderItem(uri, onRemove = { viewModel.onRemoveFolder(uri) })
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {

                            viewModel.isPackageSelected(isPackageSelected = state.packagesToWipe.isNotEmpty())
                            viewModel.isFolderSelected(isFolderSelected = state.selectedFolders.isNotEmpty())

                            onNavigateToMethods()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HeaderBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Continuar a Métodos", color = Color.White, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 5. Pie de página
            item {
                Text(
                    "Versión\n1.0.12.1",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}


@Composable
fun OriginCard(title: String, color: Color, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.folder),
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, fontSize = 16.sp, color = Color.Black)
        }
    }
}

@Composable
fun FolderItem(uri: Uri, onRemove: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = uri.path?.substringAfterLast('/') ?: "...",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                fontSize = 14.sp,
                color = Color.Black
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Quitar",
                    tint = Color.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}