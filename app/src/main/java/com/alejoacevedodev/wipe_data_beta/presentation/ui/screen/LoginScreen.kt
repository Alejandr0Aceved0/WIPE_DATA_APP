package com.alejoacevedodev.wipe_data_beta.presentation.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onConfigureFtp: () -> Unit // Callback para ir a la config FTP
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Necesitamos el contexto para mostrar el Toast de error
    val context = LocalContext.current

    val DarkBlueButton = Color(0xFF2B4C6F)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {

        // Botón de configuración FTP (esquina superior derecha)
        IconButton(
            onClick = onConfigureFtp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Configurar FTP",
                tint = Color.Gray
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Ingreso",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Campo Usuario
            Text(
                text = "Usuario",
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black,
                fontSize = 14.sp
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Usuario o correo electrónico", color = Color.LightGray) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                },
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Gray,
                    unfocusedBorderColor = Color.LightGray,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Contraseña
            Text(
                text = "Contraseña",
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black,
                fontSize = 14.sp
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Contraseña", color = Color.LightGray) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                trailingIcon = {
                    val image =
                        if (passwordVisible) Icons.Filled.CheckCircle else Icons.Filled.Check
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = null, tint = Color.Gray)
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Gray,
                    unfocusedBorderColor = Color.LightGray,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Botón Ingresar con VALIDACIÓN
            Button(
                onClick = {
                    // 1. Obtener la fecha actual en formato dd/MM/yyyy
                    val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                    // 2. Construir las credenciales esperadas
                    val expectedUser = "Prueba2025"
                    val expectedPass = "Prueba2025.*$currentDate"

                    // 3. Validar
                    if (username == expectedUser && password == expectedPass) {
                        onLoginSuccess()
                    } else {
                        Toast.makeText(context, "Credenciales incorrectas. Verifique usuario y/o contraseña.", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlueButton),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Ingresar", fontSize = 18.sp, color = Color.White)
            }

            // Espaciador flexible para empujar el footer
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Versión\n1.0.12.1",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}