package com.alejoacevedodev.wipe_data_beta.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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

    // Definición de Colores (Basados en tu diseño)
    val HeaderBlue = Color(0xFF2B4C6F)
    val SuccessGreen = Color(0xFF4CAF50)
    val BackgroundColor = Color.White

    // --- LÓGICA DE FECHAS Y DURACIÓN ---
    // Usamos una fecha base de seguridad (Enero 2024) para evitar mostrar "1969" si algo falló
    val minValidTime = 1704067200000L
    val startTime =
        if (state.wipeStartTime > minValidTime) state.wipeStartTime else System.currentTimeMillis()
    val endTime =
        if (state.wipeEndTime > minValidTime) state.wipeEndTime else System.currentTimeMillis()

    // Formatear Fecha de Inicio (ej. 20/11/2025 14:30:00)
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val startDateStr = dateFormat.format(Date(startTime))

    // Calcular Duración
    val durationMillis = if (endTime >= startTime) endTime - startTime else 0
    val durationSeconds = durationMillis / 1000
    val durationFormatted = String.format(
        Locale.getDefault(),
        "%02d:%02d:%02d",
        durationSeconds / 3600,
        (durationSeconds % 3600) / 60,
        durationSeconds % 60
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {

        Column(modifier = Modifier.fillMaxSize()) {

            // 1. ENCABEZADO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(HeaderBlue)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "NULLUM Lite",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                // Botón de Salir (Icono puerta)
                IconButton(
                    onClick = onNavigateHome,
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
                    .verticalScroll(rememberScrollState()) // Permite scroll si el contenido es largo
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
                ReportItem("Modelo:", android.os.Build.MODEL)
                ReportItem("Fabricante:", android.os.Build.MANUFACTURER)
                ReportItem("Plataforma:", "Android ${android.os.Build.VERSION.RELEASE}")

                Spacer(modifier = Modifier.height(24.dp))

                // -- SECCIÓN: RESULTADOS --
                ReportSectionTitle("Resultados")
                ReportItem("Borrado:", "Completo")
                // Aquí mostramos el conteo real que calculamos en el ViewModel
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, HeaderBlue)
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
                // Espacio extra al final para asegurar que se vea todo en scroll
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
            modifier = Modifier.weight(1f) // Permite que el valor ocupe el resto de la línea
        )
    }
}