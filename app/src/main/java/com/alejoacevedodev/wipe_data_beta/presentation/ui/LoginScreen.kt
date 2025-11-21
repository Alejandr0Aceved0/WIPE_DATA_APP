package com.alejoacevedodev.wipe_data_beta.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val HeaderBlue = Color(0xFF2B4C6F)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(WindowInsets.statusBars.asPaddingValues()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // Centrado verticalmente
    ) {
        Text(
            text = "Ingreso",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Usuario
        Text(text = "Usuario", modifier = Modifier.fillMaxWidth(), color = Color.Black)
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Usuario o correo electrónico") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Gray,
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Contraseña
        Text(text = "Contraseña", modifier = Modifier.fillMaxWidth(), color = Color.Black)
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.CheckCircle else Icons.Filled.Check
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Gray,
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Botón Ingresar
        Button(
            onClick = onLoginSuccess,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HeaderBlue),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Ingresar", fontSize = 18.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(text = "Versión\n1.0.12.1", color = Color.Gray, style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}