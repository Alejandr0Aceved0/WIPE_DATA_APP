package com.alejoacevedodev.wipedatabeta.presentation.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.alejoacevedodev.wipedatabeta.presentation.wipe.WipeViewModel

@Composable
fun ConfirmationScreen(
    viewModel: WipeViewModel,
    onNavigateBack: () -> Unit,
    onProcessFinished: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val HeaderBlue = Color(0xFF2B4C6F)


    LaunchedEffect(state.wipeFinished) {
        if (state.wipeFinished) {
            Toast.makeText(
                context,
                "Proceso finalizado. Se borraron ${state.deletedCount} carpetas.",
                Toast.LENGTH_LONG
            ).show()
            viewModel.resetWipeStatus()
            onProcessFinished()
        }
    }

    // 1. Quitamos el padding del contenedor raíz
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 2. Header con corrección de Status Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HeaderBlue) // Fondo azul primero (cubre status bar)
                    .statusBarsPadding()    // Empuja el contenido hacia abajo
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "NULLUM Lite",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { onNavigateHome }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Salir",
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }

            // Contenido
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Confirmación de Borrado",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Método seleccionado:", fontWeight = FontWeight.Bold)
                        Text(state.selectedMethod?.name ?: "Ninguno")
                        Spacer(modifier = Modifier.height(12.dp))

                        if (state.packagesToWipe.isNotEmpty()) {
                            Text(
                                "Paquetes a borrar (${state.packagesToWipe.size}):",
                                fontWeight = FontWeight.Bold
                            )
                            state.packagesToWipe.forEach {
                                Text("- ${it.substringAfterLast('/')}", fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }


                        if (state.selectedFolders.isNotEmpty()) {
                            Text(
                                "Carpetas a borrar (${state.selectedFolders.size}):",
                                fontWeight = FontWeight.Bold
                            )
                            state.selectedFolders.forEach {
                                Text("- ${it.path?.substringAfterLast('/')}", fontSize = 14.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Botón Confirmar
                Button(
                    onClick = {
                        viewModel.startWipeProcess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !state.isWiping
                ) {
                    Text("CONFIRMAR Y BORRAR", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onNavigateBack, enabled = !state.isWiping) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        }

        // Indicador de Carga (Overlay)
        if (state.isWiping) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Borrando ${state.currentWipingFile}...", color = Color.White)
                    Text("Por favor espere...", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }
    }
}