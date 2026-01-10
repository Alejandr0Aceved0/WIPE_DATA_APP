package com.alejoacevedodev.wipedatabeta.presentation.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alejoacevedodev.wipedatabeta.R

/**
 * Copyright 2026 Obtelco.s.a.s, Inc. All rights reserved
 * File CurvedHeader 1.2.5 7/01/26
 *
 * @author Carlos Alejandro Acevedo Espitia
 */

@Composable
fun CurvedHeader(
    settingsAvailable: Boolean? = false,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val HeaderGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF2E61F1), Color(0xFF1E3A8A))
    )

    val headerHeight = 150.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
    ) {
        /* 1. Fondo con curva (Canvas) */
        Canvas(modifier = Modifier.fillMaxSize()) {
            val curveRadius = size.height * 0.42f
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(curveRadius, size.height)
                quadraticBezierTo(0f, size.height, 0f, size.height - curveRadius)
                close()
            }
            drawPath(path, HeaderGradient)
        }

        /* 2. LOGO INDEPENDIENTE (Box con offset) */
        // Al estar en un Box, este Image "flota" sobre el resto
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = null,
            modifier = Modifier
                .offset(x = -25.dp, y = 28.dp) // Lo mueves donde quieras
                .size(150.dp)                             // Cambias tamaño sin afectar al texto
        )

        /* 3. CONTENIDO ESTRUCTURADO (Iconos y Texto) */
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Iconos superiores
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp, top = 6.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (settingsAvailable == true) {
                    IconButton(onClick = onSettingsClick) {
                        Icon(painterResource(R.drawable.ic_settings), null, tint = Color.White)
                    }
                }
                IconButton(onClick = onLogoutClick) {
                    Icon(painterResource(R.drawable.ic_exit), null, tint = Color.White)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 88.dp),
                horizontalArrangement = Arrangement.Start
            ) {

                // Título (Ahora solo, ya no depende del logo)
                Text(
                    text = "Nullum",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(
                        start = 20.dp,
                        top = 10.dp
                    ) // Ajusta el padding para que no choque con el logo
                )

                Text(
                    text = " Lite",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(
                        start = 0.dp,
                        top = 10.dp
                    ) // Ajusta el padding para que no choque con el logo
                )
            }
        }
    }
}

@Composable
@Preview
fun CurvedHeaderPreview() {
    CurvedHeader(
        settingsAvailable = true,
        onSettingsClick = {},
        onLogoutClick = {}
    )
}