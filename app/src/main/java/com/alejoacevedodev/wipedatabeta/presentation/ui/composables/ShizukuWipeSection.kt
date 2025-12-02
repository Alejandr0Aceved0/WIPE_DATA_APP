package com.alejoacevedodev.wipedatabeta.presentation.ui.composables

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alejoacevedodev.wipedatabeta.presentation.viewmodel.WipeViewModel
import rikka.shizuku.Shizuku

@Composable
fun ShizukuWipeSection(viewModel: WipeViewModel, headerColor: Color) {
    val isPermitted by viewModel.isShizukuPermitted.collectAsState()
    var packageNameInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    // 1. Iniciar la verificación de permisos de Shizuku al cargar la sección
    LaunchedEffect(Unit) {
        viewModel.requestShizukuPermission()
    }

    Divider(modifier = Modifier.padding(vertical = 16.dp))

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Text(
            text = "Limpieza de Datos vía Shizuku",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Estado del Permiso
        Text(
            text = when {
                !Shizuku.pingBinder() -> "⚠️ Shizuku no activo. Inicie el servicio ADB."
                isPermitted -> "✅ Privilegio Shell (UID 2000) ACTIVO."
                else -> "❌ Toque para solicitar permiso..."
            },
            color = when {
                !Shizuku.pingBinder() -> androidx.compose.ui.graphics.Color.Yellow.copy(alpha = 0.8f)
                isPermitted -> androidx.compose.ui.graphics.Color.Green.copy(alpha = 0.8f)
                else -> androidx.compose.ui.graphics.Color.Red
            },
            fontSize = 12.sp,
            modifier = Modifier.clickable { viewModel.requestShizukuPermission() } // Permite solicitar al hacer clic
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 3. Input para el Paquete
        OutlinedTextField(
            value = packageNameInput,
            onValueChange = { packageNameInput = it },
            label = { Text("Nombre del Paquete (ej. com.chrome)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 4. Botón de Ejecución (pm clear)
        Button(
            onClick = {
                if (packageNameInput.isNotBlank()) {
                    viewModel.executeShizukuWipe(packageNameInput)
                } else {
                    Toast.makeText(context, "Ingrese un nombre de paquete.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(8.dp),
            // Solo activo si Shizuku está conectado y el permiso fue otorgado por el usuario
            enabled = isPermitted && packageNameInput.isNotBlank()
        ) {
            Text("Limpiar Datos de App (Shizuku)", color = androidx.compose.ui.graphics.Color.White, fontSize = 16.sp)
        }
    }
}