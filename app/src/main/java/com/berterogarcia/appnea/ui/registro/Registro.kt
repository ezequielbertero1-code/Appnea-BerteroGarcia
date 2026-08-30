package com.berterogarcia.appnea.ui.registro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.components.CampoTexto

@Composable
fun RegistroScreen(viewModel: RegistroViewModel, onRegistroExitoso: () -> Unit) {
    var usuario by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmarPassword by rememberSaveable { mutableStateOf("") }
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(estado) {
        if (estado is RegistroUiState.Exitoso) onRegistroExitoso()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CampoTexto(valor = usuario, onValorChange = { usuario = it }, etiqueta = stringResource(R.string.registro_username))
        CampoTexto(valor = password, onValorChange = { password = it }, etiqueta = stringResource(R.string.registro_password), esPassword = true)
        CampoTexto(valor = confirmarPassword, onValorChange = { confirmarPassword = it }, etiqueta = stringResource(R.string.registro_confirmar_password), esPassword = true)
        Button(onClick = { viewModel.registrar(usuario, password, confirmarPassword) }) {
            Text(stringResource(R.string.registro_button))
        }
        if (estado is RegistroUiState.Error) {
            Text(
                text = (estado as RegistroUiState.Error).mensaje,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}