package com.alejoacevedodev.wipedatabeta.presentation.ui.screen

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alejoacevedodev.wipedatabeta.R
import com.alejoacevedodev.wipedatabeta.presentation.settings.SettingsViewModel
import com.alejoacevedodev.wipedatabeta.presentation.ui.composables.CurvedHeader
import com.alejoacevedodev.wipedatabeta.ui.theme.FormFontFamily
import com.alejoacevedodev.wipedatabeta.utils.getAppVersion

@Composable
fun FtpSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    val PrimaryDarkBlue = Color(0xFF1E3A8A)
    val PrimaryBlue = Color(0xFF2E61F1)
    val ErrorRed = Color(0xFFD32F2F)

    val context = androidx.compose.ui.platform.LocalContext.current
    val appVersion = remember { getAppVersion(context) }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {
            CurvedHeader(onSettingsClick = { }, onLogoutClick = onNavigateHome)

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-48).dp)
                    .padding(top = 50.dp),
                color = Color.White,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Botón Regresar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.height(35.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Regresar", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Configuración del envío automático FTP",
                        fontSize = 35.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryDarkBlue,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Campo: URL o Host
                    FtpInputField(
                        label = "URL o Host",
                        value = state.ftpHost,
                        placeholder = "Enlace de conexión al FTP",
                        iconRes = R.drawable.ic_link,
                        isError = state.hostError,
                        onValueChange = { viewModel.updateFtpHost(it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Fila para Puerto y Usuario
                    Row(modifier = Modifier.fillMaxWidth()) {

                        Box(modifier = Modifier.weight(0.7f)) {
                            FtpInputField(
                                label = "Usuario",
                                value = state.ftpUser,
                                placeholder = "Nombre de usuario",
                                iconRes = R.drawable.ic_mail,
                                isError = state.userError,
                                onValueChange = { viewModel.updateFtpUser(it) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo: Contraseña
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Contraseña",
                            color = PrimaryDarkBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.ftpPass,
                            onValueChange = { viewModel.updateFtpPass(it) },
                            modifier = Modifier.fillMaxWidth(),
                            isError = state.passError,
                            placeholder = { Text("Contraseña", color = Color.Gray) },
                            leadingIcon = {
                                Icon(
                                    painterResource(R.drawable.ic_lock),
                                    null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_figma_eye),
                                        contentDescription = null,
                                        tint = if (passwordVisible) Color.Unspecified else PrimaryBlue
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = PrimaryBlue.copy(alpha = 0.5f),
                                errorBorderColor = ErrorRed
                            )
                        )
                        if (state.passError) Text("Requerido", color = ErrorRed, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Button(
                        onClick = {
                            // Solo navegamos si la validación y guardado fueron exitosos
                            if (viewModel.saveFtpConfig()) {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text(
                            "Guardar",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "Versión", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(text = "$appVersion", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun FtpInputField(
    label: String,
    value: String,
    placeholder: String,
    @DrawableRes iconRes: Int,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Color(0xFF1E3A8A),
            fontFamily = FormFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            placeholder = {
                Text(
                    placeholder,
                    color = Color.Gray,
                    fontFamily = FormFontFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = 16.sp
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2E61F1),
                unfocusedBorderColor = Color(0xFF2E61F1).copy(alpha = 0.5f),
                errorBorderColor = Color(0xFFD32F2F)
            )
        )
    }
}