package com.alejoacevedodev.wipe_data_beta.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alejoacevedodev.wipe_data_beta.domain.model.WipeMethod
import com.alejoacevedodev.wipe_data_beta.presentation.viewmodel.WipeViewModel

@Composable
fun WipeMethodScreen(
    viewModel: WipeViewModel,
    onNavigateBack: () -> Unit,
    onMethodSelected: () -> Unit // 👈 ¡ESTE ES EL PARÁMETRO QUE FALTABA!
) {
    val state by viewModel.uiState.collectAsState()
    val HeaderBlue = Color(0xFF2B4C6F)
    val ButtonGray = Color(0xFFE0E0E0)

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(HeaderBlue)
                    .padding(WindowInsets.statusBars.asPaddingValues()),
                contentAlignment = Alignment.CenterStart
            ) {
                Text("NULLUM Lite", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Salir",
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Regresar", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(text = "Selección del método de borrado", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                Spacer(modifier = Modifier.height(30.dp))

                // --- BOTONES DE MÉTODOS ---
                // Al hacer click:
                // 1. Guardamos el método en el ViewModel (viewModel.selectMethod)
                // 2. Ejecutamos la navegación (onMethodSelected)

                MethodSelectionCard("DoD 5220.22-M", ButtonGray) {
                    viewModel.selectMethod(WipeMethod.DoD_5220_22_M)
                    onMethodSelected() // Navega a confirmación
                }
                Spacer(modifier = Modifier.height(16.dp))

                MethodSelectionCard("NIST SP 800-88", ButtonGray) {
                    viewModel.selectMethod(WipeMethod.NIST_SP_800_88)
                    onMethodSelected() // Navega a confirmación
                }
                Spacer(modifier = Modifier.height(16.dp))

                MethodSelectionCard("BSI TL-03423", ButtonGray) {
                    viewModel.selectMethod(WipeMethod.BSI_TL_03423)
                    onMethodSelected() // Navega a confirmación
                }

                Spacer(modifier = Modifier.weight(1f))
                Text("Versión\n1.0.12.1", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 12.sp)
            }
        }

        // Indicador de carga (si aplica)
        if (state.isWiping) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
fun MethodSelectionCard(title: String, color: Color, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(12.dp)).clickable { onClick() }
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Black, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, fontSize = 16.sp, color = Color.Black)
        }
    }
}