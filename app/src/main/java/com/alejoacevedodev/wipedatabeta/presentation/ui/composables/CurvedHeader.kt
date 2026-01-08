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
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val HeaderGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF2E61F1),
            Color(0xFF1A3365)
        )
    )

    val headerHeight = 150.dp   // 🔥 altura exacta tipo Figma

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
    ) {

        /* ===== Fondo azul con curva EXACTA ===== */
        Canvas(modifier = Modifier.fillMaxSize()) {
            val curveRadius = size.height * 0.42f

            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(curveRadius, size.height)

                // curva limpia solo esquina inferior izquierda
                quadraticBezierTo(
                    x1 = 0f,
                    y1 = size.height,
                    x2 = 0f,
                    y2 = size.height - curveRadius
                )

                close()
            }

            drawPath(path, HeaderGradient)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {

            /* ===== Iconos superiores ===== */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp, top = 6.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                IconButton(onClick = onLogoutClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_exit),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            /* ===== Logo + título ===== */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = null,
                    modifier = Modifier.size(92.dp) // 🔥 tamaño exacto
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Nullum Lite",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}