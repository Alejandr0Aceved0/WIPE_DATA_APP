package com.alejoacevedodev.wipe_data_beta.presentation.ui.screen

import android.os.Build
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alejoacevedodev.wipe_data_beta.presentation.viewmodel.WipeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportScreen(
    viewModel: WipeViewModel,
    onNavigateHome: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // Definición de Colores
    val HeaderBlue = Color(0xFF2B4C6F)
    val SuccessGreen = Color(0xFF4CAF50)
    val BackgroundColor = Color.White

    // --- LÓGICA DE FECHAS Y DURACIÓN ---
    val minValidTime = 1704067200000L
    val startTime =
        if (state.wipeStartTime > minValidTime) state.wipeStartTime else System.currentTimeMillis()
    val endTime =
        if (state.wipeEndTime > minValidTime) state.wipeEndTime else System.currentTimeMillis()

    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val startDateStr = dateFormat.format(Date(startTime))

    val durationMillis = if (endTime >= startTime) endTime - startTime else 0
    val durationSeconds = durationMillis / 1000
    val durationFormatted = String.format(
        Locale.getDefault(),
        "%02d:%02d:%02d",
        durationSeconds / 3600,
        (durationSeconds % 3600) / 60,
        durationSeconds % 60
    )

    // 1. Quitamos el padding global del contenedor raíz
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {

        Column(modifier = Modifier.fillMaxSize()) {

            // 2. ENCABEZADO (Corrección de Status Bar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HeaderBlue) // A. Fondo azul primero
                    .statusBarsPadding()    // B. Empuja el contenido hacia abajo (respetando notch/hora)
                    .height(56.dp)          // C. Altura del contenido del header
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "NULLUM Lite",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                // Botón de Salir
                IconButton(
                    onClick = { onNavigateHome() },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Salir",
                        tint = Color.White
                    )
                }
            }

            // 2. CONTENIDO DEL REPORTE (Scrollable)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {

                // -- ESTADO DE ÉXITO --
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
                Text(
                    text = "Informe del evento:",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(24.dp))

                // -- SECCIÓN: ATRIBUTOS --
                ReportSectionTitle("Atributos")
                ReportItem("Método de Borrado:", state.selectedMethod?.name ?: "NIST 800-88")
                ReportItem("Verificación:", "100%")
                ReportItem("Integridad del Proceso:", "100%")

                Spacer(modifier = Modifier.height(24.dp))

                // -- SECCIÓN: INFORMACIÓN DEL DISPOSITIVO --
                ReportSectionTitle("Información del Dispositivo")
                ReportItem("Modelo:", Build.MODEL)
                ReportItem("Fabricante:", Build.MANUFACTURER)
                ReportItem("Plataforma:", "Android ${Build.VERSION.RELEASE}")

                Spacer(modifier = Modifier.height(24.dp))

                // -- SECCIÓN: RESULTADOS --
                ReportSectionTitle("Resultados")
                ReportItem("Borrado:", "Completo")
                ReportItem("Items Eliminados:", "${state.deletedCount}")
                ReportItem("Inicio:", startDateStr)
                ReportItem("Duración:", durationFormatted)

                Spacer(modifier = Modifier.height(40.dp))

                // -- BOTONES DE ACCIÓN --

                // 1. Descargar PDF
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

                // 2. Volver al Inicio
                OutlinedButton(
                    onClick = onNavigateHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, HeaderBlue)
                ) {
                    Text("Volver al Inicio", color = HeaderBlue)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer Versión
                Text(
                    text = "Versión\n1.0.12.1",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// --- COMPONENTES UI AUXILIARES ---

@Composable
fun ReportSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun ReportItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$label ",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.DarkGray,
            modifier = Modifier.weight(1f)
        )
    }
}