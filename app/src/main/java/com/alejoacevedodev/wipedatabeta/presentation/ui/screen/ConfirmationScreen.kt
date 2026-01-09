package com.alejoacevedodev.wipedatabeta.presentation.ui.screen

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alejoacevedodev.wipedatabeta.presentation.ui.composables.CardWipeOption
import com.alejoacevedodev.wipedatabeta.presentation.ui.composables.CurvedHeader
import com.alejoacevedodev.wipedatabeta.presentation.wipe.WipeViewModel

@Composable
fun ConfirmationScreen(
    viewModel: WipeViewModel,
    onNavigateBack: () -> Unit,
    onProcessFinished: () -> Unit, // Navega a la vista de reporte
    onNavigateHome: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val PrimaryDarkBlue = Color(0xFF1A3365)
    val PrimaryBlue = Color(0xFF2E61F1)

    // Escucha el fin del proceso para navegar al Reporte
    LaunchedEffect(state.wipeFinished) {
        if (state.wipeFinished) {
            Toast.makeText(context, "Proceso finalizado.", Toast.LENGTH_SHORT).show()
            onProcessFinished() // Navegación a ReportScreen
            viewModel.resetWipeStatus()
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. Header con degradado y logo (Figma)
            CurvedHeader(
                onSettingsClick = {},
                onLogoutClick = onNavigateHome
            )

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-48).dp),
                color = Color.White,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Botón Regresar superior
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.height(35.dp),
                            enabled = !state.isWiping
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Regresar", fontSize = 12.sp)
                        }
                    }

                    Text(
                        text = "Método seleccionado",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryDarkBlue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )

                    // 2. Card del método con la curva naranja y logo compuesto
                    CardWipeOption(
                        title = state.selectedMethod?.name ?: "No seleccionado",
                        onClick = {},
                        curveColor = Color(0x19DA6512)// Deshabilitado en esta vista
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Elementos a borrar",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDarkBlue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    // 3. Listado detallado (LazyColumn) con estilo Figma
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFF)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E7F5))
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Apps/Paquetes
                            if (state.packagesToWipe.isNotEmpty()) {
                                item {
                                    Text(
                                        "Paquetes (${state.packagesToWipe.size})",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PrimaryBlue,
                                        fontSize = 14.sp
                                    )
                                }
                                items(state.packagesToWipe.toList()) { pkg ->
                                    ConfirmationItemRow(name = pkg, isPackage = true)
                                }
                            }

                            // Carpetas
                            if (state.selectedFolders.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Carpetas (${state.selectedFolders.size})",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PrimaryBlue,
                                        fontSize = 14.sp
                                    )
                                }
                                items(state.selectedFolders.toList()) { uri ->
                                    ConfirmationItemRow(
                                        name = uri.path?.substringAfterLast('/') ?: "Carpeta",
                                        isPackage = false
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 4. Botón Confirmar Rojo vibrante (Figma)
                    Button(
                        onClick = { viewModel.startWipeProcess() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3D00)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        enabled = !state.isWiping
                    ) {
                        Text(
                            "Confirmar y borrar",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Versión 1.0.12.1", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // 5. Overlay de Loading mejorado visualmente
        if (state.isWiping) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = PrimaryBlue, strokeWidth = 4.dp)
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "Borrando datos...",
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryDarkBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.currentWipingFile,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmationItemRow(name: String, isPackage: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (isPackage) Icons.Default.Settings else Icons.Default.Delete,
            contentDescription = null,
            tint = Color(0xFF1A3365),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = name.substringAfterLast('/'),
            fontSize = 13.sp,
            color = Color.DarkGray,
            maxLines = 1
        )
    }
}