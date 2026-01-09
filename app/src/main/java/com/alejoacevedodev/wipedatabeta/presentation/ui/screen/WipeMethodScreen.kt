package com.alejoacevedodev.wipedatabeta.presentation.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.alejoacevedodev.wipedatabeta.domain.model.WipeMethod
import com.alejoacevedodev.wipedatabeta.presentation.ui.composables.CardWipeOption
import com.alejoacevedodev.wipedatabeta.presentation.ui.composables.CurvedHeader
import com.alejoacevedodev.wipedatabeta.presentation.wipe.WipeViewModel

@Composable
fun WipeMethodScreen(
    viewModel: WipeViewModel,
    onNavigateBack: () -> Unit,
    onMethodSelected: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val PrimaryDarkBlue = Color(0xFF1A3365)
    val PrimaryBlue = Color(0xFF2E61F1)
    val CardCurveColor = Color(0xFFF3F7FF) // Un tono azul sutil para la curva de métodos

    fun validateAndProceed(method: WipeMethod) {
        if (state.isPackageSelected && state.packagesToWipe.isEmpty()) {
            Toast.makeText(context, "⚠️ No hay paquetes seleccionados.", Toast.LENGTH_SHORT).show()
        } else if (state.isFolderSelected && state.selectedFolders.isEmpty()) {
            Toast.makeText(context, "⚠️ No hay carpetas seleccionadas.", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.selectMethod(method)
            onMethodSelected()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            CurvedHeader(
                onSettingsClick = { /* Si no hay settings aquí, puede quedar vacío o removerlo del composable */ },
                onLogoutClick = onNavigateHome
            )

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 55.dp)
                    .offset(y = (-48).dp),
                color = Color.White,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))

                        // Botón regresar estilizado
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = onNavigateBack,
                                contentPadding = PaddingValues(horizontal = 16.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Regresar", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Selección del método de borrado",
                            fontSize = 35.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryDarkBlue,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        Text(
                            text = "Elija el estándar de seguridad para el proceso.",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    // --- TARJETAS DE MÉTODOS REUTILIZANDO CARDOPTION ---
                    item {
                        CardWipeOption(
                            title = "DoD 5220.22-M",
                            curveColor = CardCurveColor,
                            onClick = {
                                validateAndProceed(WipeMethod.DoD_5220_22_M)
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        CardWipeOption(
                            title = "NIST SP 800-88",
                            curveColor = CardCurveColor,
                            onClick = {
                                validateAndProceed(WipeMethod.NIST_SP_800_88)
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        CardWipeOption(
                            title = "BSI TL-03423",
                            curveColor = CardCurveColor,
                            onClick = {
                                validateAndProceed(WipeMethod.BSI_TL_03423)
                            }
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "Versión 1.0.12.1",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }

        // Overlay de Carga
        if (state.isWiping) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = PrimaryBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Procesando...", fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
                    }
                }
            }
        }
    }
}


@androidx.compose.ui.tooling.preview.Preview(showBackground = true, showSystemUi = true)
@Composable
fun WipeMethodScreenPreview() {
    // Simulamos un ViewModel o un estado básico para el preview
    val PrimaryBlue = Color(0xFF2E61F1)

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Cabecera simulada
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E4198), Color(0xFF3B67D6))
                    ))
            )

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-48).dp),
                color = Color.White,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Selección del método",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1A3365)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Ejemplo de tarjetas
                    repeat(3) {
                        CardWipeOption(
                            title = "Método de Seguridad $it",
                            curveColor = Color(0xFFF3F7FF),
                            onClick = {}
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}