package com.alejoacevedodev.wipedatabeta.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Copyright 2026 Wireless&Mobile, Inc. All rights reserved
 * File TopBannerSection 1.2.5 8/01/26
 *
 * @author Carlos Alejandro Acevedo Espitia
 * @version 1.2.5 8/01/26
 * @since 1.2.5
 */

@Composable
fun TopBannerSection(
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val BannerGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E4198), Color(0xFF3B67D6))
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(BannerGradient)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Grupo de Logo y Título
            Row(verticalAlignment = Alignment.CenterVertically) {

                // AQUÍ USAMOS EL LOGO COMPUESTO
                CompositeLogo()

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Nullum Lite",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Botones de Icono
            Row {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                }
                IconButton(onClick = onLogoutClick) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}


@Composable
@Preview
fun TopBannerSectionPreview() {
    TopBannerSection(
        onSettingsClick = {},
        onLogoutClick = {}
    )
}