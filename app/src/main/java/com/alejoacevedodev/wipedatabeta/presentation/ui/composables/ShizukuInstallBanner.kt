package com.alejoacevedodev.wipedatabeta.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ShizukuInstallBanner(onInstallClick: () -> Unit) {
    // Definición de colores basada en tu paleta
    val BgColor = Color(0xFFFEECE9) // Un coral muy suave de fondo
    val AccentColor = Color(0xFFF87858) // Tu color naranja/coral principal
    val TextDarkColor = Color(0xFF1E3A8A) // Azul oscuro para texto importante

    val HeaderGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF2E61F1),
            Color(0xFF1E3A8A)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGradient)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = BgColor),
        shape = RoundedCornerShape(20.dp), // Bordes más redondeados tipo Figma
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Sin sombras pesadas para un look flat/moderno
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono con un contenedor circular suave
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AccentColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AccentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Shizuku no instalado",
                    fontWeight = FontWeight.Bold,
                    color = TextDarkColor,
                    fontSize = 16.sp,
                    lineHeight = 18.sp
                )
                Text(
                    text = "Es necesario para gestionar el borrado de datos.",
                    fontSize = 13.sp,
                    color = TextDarkColor.copy(alpha = 0.7f),
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Botón con estilo "Chip" o cápsula
            Button(
                onClick = onInstallClick,
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                shape = RoundedCornerShape(50), // Botón tipo píldora
                modifier = Modifier.size(width = 90.dp, height = 36.dp)
            ) {
                Text(
                    "Instalar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}