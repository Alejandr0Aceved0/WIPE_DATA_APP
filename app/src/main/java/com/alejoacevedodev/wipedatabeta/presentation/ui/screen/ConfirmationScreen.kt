package com.alejoacevedodev.wipedatabeta.presentation.ui.screen

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alejoacevedodev.wipedatabeta.presentation.ui.composables.CardWipeOption
import com.alejoacevedodev.wipedatabeta.presentation.ui.composables.CurvedHeader
import com.alejoacevedodev.wipedatabeta.presentation.wipe.WipeViewModel
import com.alejoacevedodev.wipedatabeta.utils.getAppVersion


@Composable
fun ConfirmationScreen(
    viewModel: WipeViewModel,
    onNavigateBack: () -> Unit,
    onProcessFinished: () -> Unit,
    onNavigateHome: () -> Unit,
    onStartWipe: () -> Unit // Nuevo parámetro para activar la navegación a la animación
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val PrimaryDarkBlue = Color(0xFF1E3A8A)
    val PrimaryBlue = Color(0xFF2E61F1)
    val appVersion = getAppVersion(context)

    // Escucha el fin del proceso para navegar al Reporte
    LaunchedEffect(state.wipeFinished) {
        if (state.wipeFinished) {
            Toast.makeText(context, "Proceso finalizado.", Toast.LENGTH_SHORT).show()
            onProcessFinished()
            viewModel.resetWipeStatus()
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)) {

        Column(modifier = Modifier.fillMaxSize()) {
            CurvedHeader(
                onSettingsClick = {},
                onLogoutClick = onNavigateHome
            )

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 50.dp)
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
                    // Botón Regresar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.height(35.dp),
                            enabled = !state.isWiping
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Regresar", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    Text(
                        text = "Método seleccionado",
                        fontSize = 35.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryDarkBlue,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )

                    // Tarjeta de método con curva naranja
                    CardWipeOption(
                        title = state.selectedMethod?.name ?: "No seleccionado",
                        onClick = {},
                        curveColor = Color(0xFFF87858)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Elementos a borrar",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDarkBlue,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    // Listado Detallado
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFF)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E7F5))
                    ) {
                        LazyColumn(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (state.packagesToWipe.isNotEmpty()) {
                                item { Text("Paquetes (${state.packagesToWipe.size})", fontWeight = FontWeight.ExtraBold, color = PrimaryBlue, fontSize = 14.sp) }
                                items(state.packagesToWipe.toList()) { pkg ->
                                    ConfirmationItemRow(name = pkg, isPackage = true)
                                }
                            }
                            if (state.selectedFolders.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Carpetas (${state.selectedFolders.size})", fontWeight = FontWeight.ExtraBold, color = PrimaryBlue, fontSize = 14.sp)
                                }
                                items(state.selectedFolders.toList()) { uri ->
                                    ConfirmationItemRow(name = uri.path ?: "Carpeta", isPackage = false)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // BOTÓN CONFIRMAR CON ONSTARTWIPE
                    Button(
                        onClick = {
                            onStartWipe() // Llama a la navegación de la animación
                            viewModel.startWipeProcess() // Inicia el proceso lógico
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3D00)),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        enabled = !state.isWiping
                    ) {
                        Text("Confirmar y borrar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Versión", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(text = "$appVersion", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(12.dp))
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
            tint = Color(0xFF1E3A8A),
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