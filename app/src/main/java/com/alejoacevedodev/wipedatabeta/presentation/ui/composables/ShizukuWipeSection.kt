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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alejoacevedodev.wipedatabeta.domain.model.WipeMethod
import com.alejoacevedodev.wipedatabeta.presentation.viewmodel.WipeViewModel
import rikka.shizuku.Shizuku

@Composable
fun ShizukuWipeSection(
    viewModel: WipeViewModel,
    headerColor: Color,
    onNavigateToMethods: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val isPermitted by viewModel.isShizukuPermitted.collectAsState()
    var packageNameInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Iniciar la verificación de permisos de Shizuku al cargar la sección
    LaunchedEffect(Unit) {
        viewModel.requestShizukuPermission()
    }

    // Contenedor principal de la Opción 1
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Título de la Opción
            Text(
                text = "Opción 1 – Borrado Seguro desde Paquetes del sistema",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = headerColor
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Input para el Nombre del Paquete
            OutlinedTextField(
                value = packageNameInput,
                onValueChange = { packageNameInput = it },
                label = { Text("Nombre del Paquete (ej. com.skg.helios)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = headerColor,
                    unfocusedBorderColor = Color.Gray
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Nota y Estado de Permisos
            Text(
                text = "Nota: si vas a borrar archivos protegidos del sistema, utilizar este método. ",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = when {
                    !Shizuku.pingBinder() -> "⚠️ Shizuku no activo. Inicie el servicio ADB."
                    isPermitted -> "✅ Privilegio Shell (UID 2000) ACTIVO."
                    else -> "❌ Permiso no otorgado. Toque para solicitar."
                },
                color = if (isPermitted) Color.Green.copy(alpha = 0.8f) else Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.clickable { viewModel.requestShizukuPermission() }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Botón Aplicar
            Button(
                onClick = {
                    if (packageNameInput.isNotBlank()) {
                        if (viewModel.validatePackageExists(packageName = packageNameInput)) {
                            viewModel.setPackageName(packageNameInput)
                            viewModel.selectMethod(WipeMethod.PM_CLEAR)
                            Toast.makeText(context, "Nombre de paquete aplicado.", Toast.LENGTH_SHORT).show()
                            onNavigateToMethods()
                        } else {
                            Toast.makeText(context, "El paquete no existe en el dispositivo.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Ingrese un nombre de paquete válido.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = headerColor),
                shape = RoundedCornerShape(8.dp),
                // Solo activo si el permiso es viable
                enabled = isPermitted && packageNameInput.isNotBlank() && Shizuku.pingBinder()
            ) {
                Text("Aplicar", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}