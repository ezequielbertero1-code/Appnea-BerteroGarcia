package com.berterogarcia.appnea.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.components.CampoTexto

@Composable
fun LoginScreen(viewModel: LoginViewModel, onLoginExitoso: () -> Unit, onRegistroClick: () -> Unit) {
    var usuario by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val estado by viewModel.uiState.collectAsState()

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
            modifier = Modifier.padding(vertical = 10.dp)
        )
        CampoTexto(valor = usuario, onValorChange = { usuario = it }, etiqueta = stringResource(R.string.login_username))
        CampoTexto(valor = password, onValorChange = { password = it }, etiqueta = stringResource(R.string.login_password), esPassword = true)
        Button(onClick = { viewModel.login(usuario, password) }) {
            Text(stringResource(R.string.login_button))
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