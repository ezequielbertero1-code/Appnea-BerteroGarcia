package com.berterogarcia.appnea.ui.cronometro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CronometroScreen(
    modifier: Modifier = Modifier,
    viewModel: CronometroViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = uiState.tiempoFormateado, fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(onClick = viewModel::onIniciar) { Text("Iniciar") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = viewModel::onParar) { Text("Parar") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = viewModel::onResetear) { Text("Resetear") }
        }
    }
}