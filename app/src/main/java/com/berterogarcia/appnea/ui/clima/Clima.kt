package com.berterogarcia.appnea.ui.clima

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.data.remote.ClimaActual
import com.berterogarcia.appnea.data.remote.MarActual
import com.berterogarcia.appnea.data.remote.descripcionClima
import com.berterogarcia.appnea.data.remote.direccionCardinal
import kotlin.math.roundToInt

@Composable
fun ClimaScreen(
    modifier: Modifier = Modifier,
    viewModel: ClimaViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val lanzadorPermiso = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) viewModel.usarMiUbicacion()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "📍 ${uiState.ubicacion.nombre ?: "Ubicación seleccionada"}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { lanzadorPermiso.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                modifier = Modifier.weight(1f)
            ) { Text("Usar mi ubicación") }
            OutlinedButton(
                onClick = { viewModel.usarComodoroRivadavia() },
                modifier = Modifier.weight(1f)
            ) { Text("Comodoro Rivadavia") }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { viewModel.cargarClima() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Actualizar") }

        Spacer(Modifier.height(16.dp))

        when {
            uiState.cargando -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) { CircularProgressIndicator() }
            }
            uiState.error != null -> {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                    Text(
                        text = uiState.error!!,
                        modifier = Modifier.padding(14.dp),
                        color = Color(0xFFB71C1C)
                    )
                }
            }
            uiState.clima != null -> {
                TarjetaClima(uiState.clima!!)
                Spacer(Modifier.height(12.dp))
                if (uiState.marDisponible && uiState.mar != null) {
                    TarjetaMar(uiState.mar!!)
                } else {
                    Card {
                        Text(
                            text = "No hay datos de oleaje disponibles para este punto (puede ser " +
                                "porque está lejos de la costa).",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                    Text(
                        text = "Recordá: estas condiciones son orientativas. Evaluá siempre las " +
                            "condiciones reales en el lugar antes de entrar al agua, y no salgas a " +
                            "pescar solo.",
                        modifier = Modifier.padding(14.dp),
                        color = Color(0xFFE65100),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaClima(clima: ClimaActual) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Clima", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("${clima.temperaturaC.roundToInt()}°C — ${descripcionClima(clima.codigoClima)}")
            clima.sensacionTermicaC?.let { Text("Sensación térmica: ${it.roundToInt()}°C") }
            clima.humedadPorc?.let { Text("Humedad: $it%") }
            Text(
                "Viento: ${clima.vientoKmh.roundToInt()} km/h (${direccionCardinal(clima.direccionVientoGrados)})"
            )
        }
    }
}

@Composable
private fun TarjetaMar(mar: MarActual) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Estado del mar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Altura de ola: ${"%.1f".format(mar.alturaOlaM)} m")
            mar.direccionOlaGrados?.let { Text("Dirección: ${direccionCardinal(it)}") }
            mar.periodoOlaS?.let { Text("Período: ${"%.0f".format(it)} s") }
        }
    }
}
