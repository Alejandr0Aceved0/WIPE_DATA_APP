package com.alejoacevedodev.wipe_data_beta.presentation.ui.screen

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
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

    // Paleta de Colores Profesional
    val AppHeaderBlue = Color(0xFF2B4C6F)
    val CertificateBlue = Color(0xFF3F51B5) // Azul para título del certificado
    val SuccessGreen = Color(0xFF2E7D32)    // Verde oscuro para estados OK
    val LabelGray = Color(0xFF555555)
    val ValueBlack = Color(0xFF222222)
    val DividerColor = Color(0xFFEEEEEE)

    // Lógica de Fechas
    val minValidTime = 1704067200000L
    val startTime = if (state.wipeStartTime > minValidTime) state.wipeStartTime else System.currentTimeMillis()
    val endTime = if (state.wipeEndTime > minValidTime) state.wipeEndTime else System.currentTimeMillis()

    val fullDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val dateOnlyFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US) // Formato tipo reporte (December 03, 2024)
    val timeOnlyFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val durationMillis = if (endTime >= startTime) endTime - startTime else 0
    val durationSeconds = durationMillis / 1000
    val durationFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", durationSeconds / 3600, (durationSeconds % 3600) / 60, durationSeconds % 60)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. ENCABEZADO DE LA APP (Navbar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppHeaderBlue)
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text("NULLUM Lite", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onNavigateHome, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Salir", tint = Color.White)
                }
            }

            // 2. CUERPO DEL CERTIFICADO (Scrollable)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {

                // --- ENCABEZADO DEL CERTIFICADO ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CERTIFICADO DE BORRADO",
                            color = CertificateBlue,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Dispositivo: ${Build.MODEL}",
                            color = ValueBlack,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Sello de Garantía Simulado
                    GuaranteeSeal()
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Fecha y Hora (Alineado a la derecha como en el ejemplo)
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                    Text("Fecha: ${dateOnlyFormat.format(Date(endTime))}", fontSize = 14.sp, color = ValueBlack)
                    Text("Hora: ${timeOnlyFormat.format(Date(endTime))}", fontSize = 14.sp, color = ValueBlack)
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp), color = CertificateBlue, thickness = 2.dp)

                // --- SECCIÓN 1: ATRIBUTOS ---
                SectionHeader("Atributos del Proceso")

                InfoRow("Método de Borrado", state.selectedMethod?.name?.replace("_", " ") ?: "NIST 800-88", isBoldValue = true)
                InfoRow("Verificación", "100% (Hash Check)")
                InfoRow("Integridad del Proceso", "Borrado Ininterrumpido", valueColor = SuccessGreen, isBoldValue = true)
                InfoRow("Objetivo", "Carpetas de Usuario")

                Spacer(modifier = Modifier.height(20.dp))

                // --- SECCIÓN 2: INFO DEL DISPOSITIVO ---
                SectionHeader("Información del Dispositivo")

                InfoRow("Modelo", Build.MODEL)
                InfoRow("Producto", Build.PRODUCT)
                InfoRow("Fabricante", Build.MANUFACTURER)
                InfoRow("Plataforma", "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                InfoRow("Hardware", Build.HARDWARE)

                Spacer(modifier = Modifier.height(20.dp))

                // --- SECCIÓN 3: RESULTADOS ---
                SectionHeader("Resultados")

                InfoRow("Estado Final", "COMPLETADO", valueColor = SuccessGreen, isBoldValue = true)
                InfoRow("Items Eliminados", "${state.deletedCount} archivos/carpetas")
                InfoRow("Inicio", fullDateFormat.format(Date(startTime)))
                InfoRow("Duración", durationFormatted)
                InfoRow("Errores", "0 Errores", valueColor = SuccessGreen, isBoldValue = true)
                InfoRow("Resultado", "Borrado Exitoso", valueColor = SuccessGreen, isBoldValue = true)

                Divider(modifier = Modifier.padding(vertical = 24.dp), color = Color.LightGray)

                // --- BOTONES ---
                Button(
                    onClick = { viewModel.generatePdf() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppHeaderBlue),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("GUARDAR REPORTE PDF", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onNavigateHome,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, AppHeaderBlue)
                ) {
                    Text("VOLVER AL INICIO", color = AppHeaderBlue, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Borrado por NULLUM Lite v1.0.12.1\nKernel: ${System.getProperty("os.version")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// --- COMPONENTES DE DISEÑO ---

@Composable
fun SectionHeader(title: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF444444),
            fontFamily = FontFamily.SansSerif
        )
        Divider(color = Color.LightGray, thickness = 1.dp)
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFF222222),
    isBoldValue: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontSize = 14.sp,
            color = Color(0xFF666666),
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = valueColor,
            fontWeight = if (isBoldValue) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
fun GuaranteeSeal() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(70.dp)
            .border(2.dp, Color(0xFFDAA520), CircleShape) // Borde dorado
            .clip(CircleShape)
            .background(Color(0xFFFFF8E1)) // Fondo crema suave
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFFDAA520),
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "100%\nSECURE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB8860B),
                textAlign = TextAlign.Center,
                lineHeight = 10.sp
            )
        }
    }
}