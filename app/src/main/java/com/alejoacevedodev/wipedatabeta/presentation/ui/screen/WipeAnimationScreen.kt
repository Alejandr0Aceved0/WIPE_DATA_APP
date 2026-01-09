package com.alejoacevedodev.wipedatabeta.presentation.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alejoacevedodev.wipedatabeta.R
import com.alejoacevedodev.wipedatabeta.domain.model.WipeUiState
import com.alejoacevedodev.wipedatabeta.presentation.wipe.WipeViewModel

/**
 * Copyright 2026 Wireless&Mobile, Inc. All rights reserved
 * File WipeAnimationScreen 1.2.5 8/01/26
 *
 * @author Carlos Alejandro Acevedo Espitia
 * @version 1.2.5 8/01/26
 * @since 1.2.5
 */

@Composable
fun WipeAnimationScreen(
    viewModel: WipeViewModel,
    onProcessFinished: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val PrimaryDarkBlue = Color(0xFF1A3365)
    val PrimaryBlue = Color(0xFF2E61F1)

    // Navegación automática al terminar
    LaunchedEffect(state.wipeFinished) {
        if (state.wipeFinished) {
            onProcessFinished()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2E61F1),
                        Color(0xFF1A3365)
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Borrando Datos",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PrimaryDarkBlue
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Por favor, no cierre la aplicación ni apague el dispositivo.",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Contenedor de la animación central
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(280.dp)
        ) {
            // Indicador de progreso circular grande
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                color = PrimaryBlue,
                strokeWidth = 8.dp,
                trackColor = Color(0xFFE6EEFF)
            )

            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier.size(110.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Información del archivo actual
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFF)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E7F5))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PROCESANDO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.currentWipingFile.ifEmpty { "Preparando archivos..." },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryDarkBlue,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Versión 1.0.12.1",
            color = Color.LightGray,
            fontSize = 12.sp
        )
    }
}


@Composable
fun WipeAnimationContent(state: WipeUiState) {
    val PrimaryDarkBlue = Color(0xFF1A3365)
    val PrimaryBlue = Color(0xFF2E61F1)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2E61F1),
                        Color(0xFF1A3365)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Fondo azul superior (Header) igual que en Figma
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = "Borrando Datos",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Por favor, no cierre la aplicación ni apague el dispositivo.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Tarjeta central con animación
            Card(
                modifier = Modifier.size(300.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(220.dp),
                        color = PrimaryBlue,
                        strokeWidth = 8.dp,
                        trackColor = Color(0xFFE6EEFF)
                    )

                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = null,
                        modifier = Modifier.size(110.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }


            Spacer(modifier = Modifier.height(40.dp))

            // Info del archivo actual
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFF)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E7F5))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("PROCESANDO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.currentWipingFile.ifEmpty { "Analizando almacenamiento..." },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryDarkBlue,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Text("Versión 1.0.12.1", color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}



@Preview(showBackground = true, showSystemUi = true, name = "Wipe Animation - Procesando")
@Composable
fun WipeAnimationScreenPreview() {
    // Creamos un estado simulado para el preview
    val mockState = WipeUiState(
        isWiping = true,
        currentWipingFile = "com.android.providers.telephony/databases/mmssms.db",
        wipeFinished = false
    )

    WipeAnimationContent(state = mockState)
}