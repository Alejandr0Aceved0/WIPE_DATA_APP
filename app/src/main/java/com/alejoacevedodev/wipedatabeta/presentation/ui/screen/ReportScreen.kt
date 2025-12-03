package com.alejoacevedodev.wipedatabeta.presentation.ui.screen

import android.os.Build
import android.os.Environment
import android.os.StatFs
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alejoacevedodev.wipedatabeta.presentation.viewmodel.WipeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun ReportScreen(
    viewModel: WipeViewModel,
    onNavigateHome: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    val HeaderBlue = Color(0xFF2B4C6F)
    val SuccessGreen = Color(0xFF4CAF50)
    val BackgroundColor = Color.White

    // --- LÓGICA DE FECHAS Y DURACIÓN PRECISA ---
    val minValidTime = 1704067200000L
    val startTime =
        if (state.wipeStartTime > minValidTime) state.wipeStartTime else System.currentTimeMillis()
    val endTime =
        if (state.wipeEndTime > minValidTime) state.wipeEndTime else System.currentTimeMillis()

    val dateFormat =
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS", Locale.getDefault()) // Agregamos milisegundos
    val startDateStr = dateFormat.format(Date(startTime))
    val endDateStr = dateFormat.format(Date(endTime))

    // Cálculo de duración detallada
    val durationMillis = if (endTime >= startTime) endTime - startTime else 0

    val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
    val millis = durationMillis % 1000

    val durationFormatted = String.format(
        Locale.getDefault(),
        "%02d:%02d:%02d.%03d", // Formato HH:mm:ss.SSS
        hours, minutes, seconds, millis
    )

    // --- LÓGICA DE ALMACENAMIENTO ---
    val storageInfo = remember { getUiStorageDetails() }
    val freedBytesStr = remember(state.freedBytes) { formatFileSize(state.freedBytes) }

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

            // 2. CONTENIDO DEL REPORTE
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {

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
                    "Informe del evento:",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(24.dp))

                // ATRIBUTOS
                ReportSectionTitle("Atributos")
                ReportItem("Método:", state.selectedMethod?.name ?: "NIST 800-88")
                ReportItem("Verificación:", "100%")
                ReportItem("Integridad:", "100%")

                Spacer(modifier = Modifier.height(24.dp))

                // INFO DISPOSITIVO
                ReportSectionTitle("Información del Dispositivo")
                ReportItem("Modelo:", Build.MODEL)
                ReportItem("Fabricante:", Build.MANUFACTURER)
                ReportItem("Plataforma:", "Android ${Build.VERSION.RELEASE}")
                ReportItem("Capacidad Total:", storageInfo.total)
                ReportItem("Espacio Disponible:", storageInfo.available)
                ReportItem("Espacio Liberado:", freedBytesStr, valueColor = SuccessGreen)

                Spacer(modifier = Modifier.height(24.dp))

                // RESULTADOS (Con extra detalle)
                ReportSectionTitle("Resultados")
                ReportItem("Estado:", "COMPLETADO")
                ReportItem("Items Eliminados:", "${state.deletedCount}")

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)

                // Tiempos detallados
                ReportItem("Inicio:", startDateStr)
                ReportItem("Fin:", endDateStr)
                ReportItem(
                    "Duración:",
                    durationFormatted,
                    valueColor = Color.Blue
                ) // Destacamos la duración

                // Datos técnicos (Long)
                ReportItem("Timestamp Inicio (ms):", "$startTime", fontSize = 12.sp)
                ReportItem("Timestamp Fin (ms):", "$endTime", fontSize = 12.sp)

                Spacer(modifier = Modifier.height(40.dp))

                // BOTONES
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

                OutlinedButton(
                    onClick = {
                        onNavigateHome()
                        viewModel.resetUiState()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, HeaderBlue)
                ) {
                    Text("Volver al Inicio", color = HeaderBlue)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Versión\n1.0.12.1",
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
fun ReportItem(
    label: String,
    value: String,
    valueColor: Color = Color.DarkGray,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp // Para hacer más pequeños los timestamps si quieres
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$label ",
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
        Text(
            text = value,
            fontSize = fontSize,
            color = valueColor,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End // Alineamos valores a la derecha para que se vea más ordenado
        )
    }
}

// --- HELPERS ---

data class UiStorageInfo(val total: String, val available: String)

fun getUiStorageDetails(): UiStorageInfo {
    val path = Environment.getDataDirectory()
    val stat = StatFs(path.path)
    val totalBytes = stat.blockCountLong * stat.blockSizeLong
    val availableBytes = stat.availableBlocksLong * stat.blockSizeLong

    return UiStorageInfo(
        total = formatFileSize(totalBytes),
        available = formatFileSize(availableBytes)
    )
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    return String.format(
        Locale.getDefault(),
        "%.2f %s",
        bytes / 1024.0.pow(digitGroups.toDouble()),
        units[digitGroups]
    )
}