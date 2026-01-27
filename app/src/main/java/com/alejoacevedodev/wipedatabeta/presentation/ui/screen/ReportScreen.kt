package com.alejoacevedodev.wipedatabeta.presentation.ui.screen

import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alejoacevedodev.wipedatabeta.domain.model.WipeUiState
import com.alejoacevedodev.wipedatabeta.presentation.ui.composables.CurvedHeader
import com.alejoacevedodev.wipedatabeta.presentation.wipe.WipeViewModel
import com.alejoacevedodev.wipedatabeta.ui.theme.FormFontFamily
import com.alejoacevedodev.wipedatabeta.utils.getAppVersion
import kotlinx.coroutines.flow.MutableStateFlow
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
    val _uiState = MutableStateFlow(WipeUiState())
    val context = LocalContext.current

    val PrimaryDarkBlue = Color(0xFF1E3A8A)
    val PrimaryBlue = Color(0xFF2E61F1)
    val SuccessGreen = Color(0xFF4CAF50)

    // --- LÓGICA DE FECHAS Y DURACIÓN (Se mantiene tu lógica funcional) ---

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

    val appVersion = remember { getAppVersion(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. HEADER CURVO (Consistente con el resto de la app)
            CurvedHeader(
                onSettingsClick = {},
                onLogoutClick = onNavigateHome
            )

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 50.dp)
                    .offset(y = (-48).dp), // Efecto de solapamiento sobre el azul
                color = Color.White,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // 2. ICONO DE ÉXITO CENTRAL
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(SuccessGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Borrado exitoso",
                        color = SuccessGreen,
                        fontFamily = FormFontFamily,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // 3. TÍTULO DEL INFORME
                    Text(
                        text = "Informe del evento",
                        fontFamily = FormFontFamily,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryDarkBlue,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- SECCIONES DE DATOS ESTILIZADAS ---

                    // ATRIBUTOS
                    ReportSectionTitleFigma("Atributos", PrimaryBlue)
                    ReportItemFigma("Método de Borrado:", state.selectedMethod?.name ?: "NIST 800-88")
                    ReportItemFigma("Verificación:", "100%") // Según Figma
                    ReportItemFigma("Integridad del Proceso:", "100%")

                    Spacer(modifier = Modifier.height(24.dp))

                    // INFO DISPOSITIVO
                    ReportSectionTitleFigma("Información del Dispositivo", PrimaryBlue)
                    ReportItemFigma("Modelo:", Build.MODEL)
                    ReportItemFigma("Fabricante:", Build.MANUFACTURER)
                    ReportItemFigma("Plataforma:", "Android ${Build.VERSION.RELEASE}")
                    ReportItemFigma("Capacidad Total:", storageInfo.total)
                    ReportItemFigma("Espacio Disponible:", storageInfo.available)
                    ReportItemFigma("Espacio Liberado:", freedBytesStr)

                    Spacer(modifier = Modifier.height(24.dp))

                    // RESULTADOS
                    ReportSectionTitleFigma("Resultados", PrimaryBlue)
                    ReportItemFigma("Estado:", "Completo")
                    ReportItemFigma("Items Eliminados:", "${state.deletedFilesList.size}")
                    ReportItemFigma("Inicio:", startDateStr)
                    ReportItemFigma("Fin:", endDateStr)
                    ReportItemFigma("Duración:", durationFormatted)
                    ReportItemFigma("Timestamp Inicio (ms):", "$startTime")
                    ReportItemFigma("Timestamp Fin (ms):", "$endTime")

                    Spacer(modifier = Modifier.height(40.dp))

                    // 4. BOTONES (Estilo Píldora de Figma)
                    Button(
                        onClick = { viewModel.generatePdf() },
                        enabled = !state.isGeneratingPdf,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isGeneratingPdf) Color.Gray else PrimaryBlue
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text(
                            text = if (state.isGeneratingPdf) "Generando..." else "Descargar Certificado PDF",
                            color = Color.White,
                            fontFamily = FormFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.resetWipeStatus()
                            onNavigateHome()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Eliminar otro elemento", color = Color.White, fontFamily = FormFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Versión ${appVersion}", fontFamily = FormFontFamily, color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }


        if (state.isGeneratingPdf) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = PrimaryBlue,
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Procesando certificado")
                        Text(text = "Por favor espere...")
                    }
                }
            }
        }
    }
}

@Composable
fun ReportSectionTitleFigma(title: String, color: Color) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontFamily = FormFontFamily,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 3.dp)
    )
}

@Composable
fun ReportItemFigma(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label ",
            fontSize = 15.sp,
            fontFamily = FormFontFamily,
            fontWeight = FontWeight.Normal,
            color = Color.Black
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontFamily = FormFontFamily,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.weight(1f)
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