package com.berterogarcia.appnea.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.alPresionarEnter

@Composable
fun LoginScreen(viewModel: LoginViewModel, onLoginExitoso: () -> Unit, onRegistroClick: () -> Unit) {
    var usuario by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val estado by viewModel.uiState.collectAsState()
    val cargando = estado is LoginUiState.Cargando

    // [NUEVO] Manejo de foco y teclado para Enter / "Siguiente" / "Listo"
    val focoPassword = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val teclado = LocalSoftwareKeyboardController.current

    val irAPassword: () -> Unit = { focoPassword.requestFocus() }
    val intentarLogin: () -> Unit = {
        teclado?.hide()
        focusManager.clearFocus()
        viewModel.login(usuario, password)
    }

    LaunchedEffect(estado) {
        if (estado is LoginUiState.Exitoso) onLoginExitoso()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.login_greeting),
            fontSize = 42.sp,
            color = Color.White,
            modifier = Modifier.padding(vertical = 10.dp)
        )

        // Usuario: Enter (teclado físico) o "Siguiente" (teclado en pantalla) → pasa a contraseña.
        CampoTexto(
            valor = usuario,
            onValorChange = { usuario = it },
            etiqueta = stringResource(R.string.login_username),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { irAPassword() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .alPresionarEnter(irAPassword)
        )

        // Contraseña: Enter o "Listo" → intenta iniciar sesión (igual que tocar el botón).
        CampoTexto(
            valor = password,
            onValorChange = { password = it },
            etiqueta = stringResource(R.string.login_password),
            esPassword = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { intentarLogin() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .focusRequester(focoPassword)
                .alPresionarEnter(intentarLogin)
        )

        Button(onClick = intentarLogin, enabled = !cargando) {
            if (cargando) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.login_button))
            }
        }
        if (estado is LoginUiState.Error)
            Text(
                text = (estado as LoginUiState.Error).mensaje,
                color = MaterialTheme.colorScheme.error
            )

        TextButton(onClick = onRegistroClick) {
            Text(stringResource(R.string.sin_cuenta))
        }
    }
}
