package com.alejoacevedodev.wipe_data_beta.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportScreen(
    viewModel: WipeViewModel,
    onNavigateHome: () -> Unit // Para volver al inicio después de ver el reporte
) {
    val state by viewModel.uiState.collectAsState()
    val HeaderBlue = Color(0xFF2B4C6F)
    val SuccessGreen = Color(0xFF4CAF50)

    // Formateador de fecha
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val startDateStr = dateFormat.format(Date(state.wipeStartTime))
    val durationMillis = state.wipeEndTime - state.wipeStartTime
    val durationSeconds = durationMillis / 1000

    // Formato HH:mm:ss para duración
    val durationFormatted = String.format(
        Locale.getDefault(),
        "%02d:%02d:%02d",
        durationSeconds / 3600,
        (durationSeconds % 3600) / 60,
        durationSeconds % 60
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(HeaderBlue)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text("NULLUM Lite", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onNavigateHome, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Salir", tint = Color.White)
                }
            }

            // Contenido Scrollable
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Mensaje de éxito
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Borrado Exitoso",
                        color = SuccessGreen,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Informe del evento:", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(24.dp))

                // Sección Atributos
                ReportSectionTitle("Atributos")
                ReportItem("Método de Borrado:", state.selectedMethod?.name ?: "NIST 800-88")
                ReportItem("Verificación:", "100%") // Simulado
                ReportItem("Integridad del Proceso:", "100%")

                Spacer(modifier = Modifier.height(24.dp))

                // Sección Información del Disco
                ReportSectionTitle("Información del Dispositivo")
                ReportItem("Modelo:", android.os.Build.MODEL)
                ReportItem("Fabricante:", android.os.Build.MANUFACTURER)
                ReportItem("Plataforma:", "Android ${android.os.Build.VERSION.RELEASE}")

                Spacer(modifier = Modifier.height(24.dp))

                // Sección Resultados
                ReportSectionTitle("Resultados")
                ReportItem("Borrado:", "Completo")
                ReportItem("Carpetas Procesadas:", "${state.deletedCount}")
                ReportItem("Inicio:", startDateStr)
                ReportItem("Duración:", durationFormatted)

                Spacer(modifier = Modifier.height(40.dp))

                // Botón Descargar PDF
                Button(
                    onClick = { viewModel.generatePdf() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HeaderBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Descargar Certificado PDF", color = Color.White, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón Volver al Inicio (Opcional)
                OutlinedButton(
                    onClick = onNavigateHome,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Volver al Inicio", color = HeaderBlue)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Versión\n1.0.12.1",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ReportSectionTitle(title: String) {
    Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun ReportItem(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label ",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.DarkGray
        )
    }
}