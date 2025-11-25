package com.alejoacevedodev.wipe_data_beta.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alejoacevedodev.wipe_data_beta.presentation.viewmodel.WipeViewModel

@Composable
fun FtpSettingsScreen(
    viewModel: WipeViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    val HeaderBlue = Color(0xFF2B4C6F)
    val DarkBlueButton = Color(0xFF2B4C6F)

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. Encabezado con corrección de Status Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HeaderBlue) // 1. Fondo azul (cubre status bar)
                    .statusBarsPadding()    // 2. Padding para bajar el contenido
                    .height(56.dp)          // 3. Altura del contenido
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "NULLUM Lite",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Salir",
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            // 2. Contenido
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Botón Regresar
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Regresar", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Configuración del envío\nautomático FTP",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Campo: URL o Host
                Text(text = "URL o Host", modifier = Modifier.fillMaxWidth(), color = Color.Black, fontSize = 14.sp)
                OutlinedTextField(
                    value = state.ftpHost,
                    onValueChange = { viewModel.updateFtpHost(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enlace de conexión al FTP", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campo: Usuario
                Text(text = "Usuario", modifier = Modifier.fillMaxWidth(), color = Color.Black, fontSize = 14.sp)
                OutlinedTextField(
                    value = state.ftpUser,
                    onValueChange = { viewModel.updateFtpUser(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Usuario", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campo: Contraseña
                Text(text = "Contraseña", modifier = Modifier.fillMaxWidth(), color = Color.Black, fontSize = 14.sp)
                OutlinedTextField(
                    value = state.ftpPass,
                    onValueChange = { viewModel.updateFtpPass(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Contraseña", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Check else Icons.Filled.CheckCircle
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null, tint = Color.Gray)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Botón Guardar
                Button(
                    onClick = {
                        viewModel.saveFtpConfig()
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlueButton),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Guardar", fontSize = 18.sp, color = Color.White)
                }

                Spacer(modifier = Modifier.weight(1f))

                Text("Versión\n1.0.12.1", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}