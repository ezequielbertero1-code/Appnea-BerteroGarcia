package com.berterogarcia.appnea.ui.clima

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.data.remote.emojiClima
import com.berterogarcia.appnea.ui.condiciones.FondoTarjeta
import com.berterogarcia.appnea.ui.condiciones.Semaforo
import com.berterogarcia.appnea.ui.condiciones.TextoSecundario
import com.berterogarcia.appnea.ui.condiciones.colorAptitud
import com.berterogarcia.appnea.ui.condiciones.direccion
import com.berterogarcia.appnea.ui.condiciones.num

private val AcentoCian = Color(0xFF16F7FB)
private val ColorError = Color(0xFFFF6B6B)

/**
 * Resumen chico del clima para Inicio: temperatura, viento, olas y semáforo para entrar
 * al agua. Toda la tarjeta abre la pantalla completa de Condiciones.
 */
@Composable
fun ClimaCompacto(
    viewModel: ClimaViewModel,
    onVerCondiciones: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val c = uiState.condiciones

    Card(
        onClick = onVerCondiciones,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta, contentColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (uiState.buscandoUbicacion) "📍 Buscando tu ubicación…"
                    else "📍 ${uiState.ubicacion.nombre ?: "Ubicación seleccionada"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextoSecundario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text("Ver más ›", style = MaterialTheme.typography.labelLarge, color = AcentoCian)
            }
            Spacer(Modifier.height(8.dp))

            when {
                c == null && uiState.cargando -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Cargando condiciones…", color = TextoSecundario)
                }
                c == null -> Column {
                    Text(
                        uiState.error ?: "Sin datos del clima.",
                        color = ColorError,
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = { viewModel.cargarClima() }) {
                        Text("Reintentar", color = AcentoCian)
                    }
                }
                else -> {
                    val clima = c.climaActual
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(emojiClima(clima?.codigoClima, clima?.esDeDia ?: true), fontSize = 32.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            num(clima?.temperaturaC, unidad = "°C"),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "💨 ${num(clima?.vientoKmh, unidad = " km/h")} ${direccion(clima?.direccionVientoGrados)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "🌊 " + (c.marActual?.olaM?.let { "%.1f m".format(it) } ?: "-"),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Para entrar al agua: ", style = MaterialTheme.typography.bodyMedium)
                        Semaforo(colorAptitud(c.aptitud.nivel), c.aptitud.nivel.texto)
                    }
                    if (uiState.cargando) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                "Orientativo: evaluá siempre las condiciones reales en el lugar.",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )
        }
    }
}

/**
 * Elegir ubicación (GPS o Comodoro) y actualizar. Antes estaba en Inicio; ahora va arriba
 * de la pantalla de Condiciones.
 */
@Composable
fun SelectorUbicacion(viewModel: ClimaViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val lanzadorPermiso = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        if (resultado.values.any { it }) viewModel.usarMiUbicacion() else viewModel.permisoDenegado()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                enabled = !uiState.cargando,
                onClick = {
                    lanzadorPermiso.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AcentoCian),
                border = BorderStroke(1.dp, AcentoCian),
                modifier = Modifier.weight(1f)
            ) { Text("Mi ubicación", maxLines = 1) }
            OutlinedButton(
                enabled = !uiState.cargando,
                onClick = { viewModel.usarComodoroRivadavia() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AcentoCian),
                border = BorderStroke(1.dp, AcentoCian),
                modifier = Modifier.weight(1f)
            ) { Text("Comodoro", maxLines = 1) }
            IconButton(enabled = !uiState.cargando, onClick = { viewModel.cargarClima() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = AcentoCian)
            }
        }

        // Avisos cuando YA hay datos en pantalla (ej.: falló el GPS y se volvió a Comodoro).
        // Sin datos, el error ya lo muestra la pantalla de Condiciones.
        if (uiState.condiciones != null) {
            uiState.error?.let {
                Text(
                    it,
                    color = ColorError,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (uiState.cargando) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }
        }
    }
}