package com.berterogarcia.appnea.ui.clima

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.condiciones.Condiciones
import com.berterogarcia.appnea.data.remote.descripcionClima
import com.berterogarcia.appnea.data.remote.emojiClima
import com.berterogarcia.appnea.ui.condiciones.Acento
import com.berterogarcia.appnea.ui.condiciones.FondoTarjeta
import com.berterogarcia.appnea.ui.condiciones.PecesActividad
import com.berterogarcia.appnea.ui.condiciones.Semaforo
import com.berterogarcia.appnea.ui.condiciones.TextoSecundario
import com.berterogarcia.appnea.ui.condiciones.colorAptitud
import com.berterogarcia.appnea.ui.condiciones.direccion
import com.berterogarcia.appnea.ui.condiciones.hora
import com.berterogarcia.appnea.ui.condiciones.num
import kotlin.math.roundToInt

/**
 * Sección de clima de Inicio: resumen de ahora + accesos a marea, sol/luna, pesca y
 * el botón para ver todas las condiciones. No tiene scroll propio (lo maneja Inicio).
 */
@Composable
fun ClimaSeccion(
    modifier: Modifier = Modifier,
    viewModel: ClimaViewModel,
    onVerCondiciones: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val lanzadorPermiso = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) viewModel.usarMiUbicacion()
    }

    Column(modifier = modifier.fillMaxWidth()) {
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF16F7FB)),
                border = BorderStroke(1.dp, Color(0xFF16F7FB)),
                modifier = Modifier.weight(1f)
            ) { Text("Usar mi ubicación") }
            OutlinedButton(
                onClick = { viewModel.usarComodoroRivadavia() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF16F7FB)),
                border = BorderStroke(1.dp, Color(0xFF16F7FB)),
                modifier = Modifier.weight(1f)
            ) { Text("Comodoro Rivadavia") }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { viewModel.cargarClima() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Actualizar") }

        Spacer(Modifier.height(16.dp))

        val condiciones = uiState.condiciones
        when {
            uiState.cargando && condiciones == null -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) { CircularProgressIndicator() }
            }
            uiState.error != null && condiciones == null -> {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                    Text(
                        text = uiState.error!!,
                        modifier = Modifier.padding(14.dp),
                        color = Color(0xFFB71C1C)
                    )
                }
            }
            condiciones != null -> {
                uiState.error?.let {
                    Text(it, color = Color(0xFFFF6B6B), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                }
                ResumenAhora(condiciones)
                Spacer(Modifier.height(12.dp))
                AccesosRapidos(condiciones, onVerCondiciones)
                Spacer(Modifier.height(4.dp))
                Button(onClick = onVerCondiciones, modifier = Modifier.fillMaxWidth()) {
                    Text("Ver todas las condiciones")
                }
                Spacer(Modifier.height(12.dp))
                AvisoOrientativo()
            }
        }
    }
}

@Composable
private fun ResumenAhora(c: Condiciones) {
    val clima = c.climaActual
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta, contentColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emojiClima(clima?.codigoClima, clima?.esDeDia ?: true), fontSize = 44.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(num(clima?.temperaturaC, unidad = "°C"), fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Text(
                        clima?.codigoClima?.let { descripcionClima(it) } ?: "Sin datos",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Sensación ${num(clima?.sensacionTermicaC, unidad = "°")}", style = MaterialTheme.typography.bodySmall)
                    c.dias.firstOrNull()?.let {
                        Text(
                            "Máx ${num(it.maximaC, unidad = "°")} · Mín ${num(it.minimaC, unidad = "°")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoSecundario
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DatoChico("💨 Viento", "${num(clima?.vientoKmh)} ${direccion(clima?.direccionVientoGrados)}")
                DatoChico("Ráfagas", num(clima?.rafagasKmh, unidad = " km/h"))
                DatoChico("💧 Humedad", num(clima?.humedadPorc?.toDouble(), unidad = "%"))
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val tendencia = c.tendenciaPresionHpa3h
                val flecha = when {
                    tendencia == null -> ""
                    tendencia > 0.5 -> " ↑"
                    tendencia < -0.5 -> " ↓"
                    else -> " →"
                }
                DatoChico("Presión", num(clima?.presionHpa, unidad = " hPa") + flecha)
                DatoChico("🌊 Olas", c.marActual?.olaM?.let { "%.1f m".format(it) } ?: "-")
                DatoChico("🌡️ Agua", c.marActual?.temperaturaAguaC?.let { "%.1f°C".format(it) } ?: "-")
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Para entrar al agua: ", style = MaterialTheme.typography.bodyMedium)
                Semaforo(colorAptitud(c.aptitud.nivel), c.aptitud.nivel.texto)
            }
            Text(c.aptitud.motivos.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
        }
    }
}

@Composable
private fun DatoChico(etiqueta: String, valor: String) {
    Column {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = TextoSecundario)
        Text(valor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AccesosRapidos(c: Condiciones, onClick: () -> Unit) {
    val marea = c.estadoMarea
    val astro = c.astroHoy
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Mosaico(
            titulo = "Marea",
            valor = marea?.let { "${if (it.subiendo) "▲ Subiendo" else "▼ Bajando"} ${"%.1f".format(it.alturaM)} m" } ?: "Sin datos",
            detalle = marea?.siguiente?.let { "${if (it.esPleamar) "Pleamar" else "Bajamar"} ${hora(it.hora)}" } ?: "",
            onClick = onClick
        )
        Mosaico(
            titulo = "Sol",
            valor = "☀️ ${hora(astro.amanecer)} - ${hora(astro.atardecer)}",
            detalle = "Luz del día",
            onClick = onClick
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Mosaico(
            titulo = "Luna",
            valor = "${astro.emojiFase} ${astro.iluminacionPorc}%",
            detalle = astro.fase,
            onClick = onClick
        )
        Mosaico(
            titulo = "Actividad de pesca",
            valor = c.actividadActual.nivel.texto,
            detalle = "${c.actividadActual.puntaje}/100",
            onClick = onClick,
            extra = { PecesActividad(c.actividadActual.nivel, tamanio = 16) }
        )
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun RowScope.Mosaico(
    titulo: String,
    valor: String,
    detalle: String,
    onClick: () -> Unit,
    extra: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta, contentColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelMedium, color = Acento)
            Spacer(Modifier.height(4.dp))
            Text(valor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            extra?.invoke()
            if (detalle.isNotBlank()) {
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            }
        }
    }
}

@Composable
private fun AvisoOrientativo() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = "Advertencia",
                tint = Color(0xFFE65100)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    "Estas condiciones son orientativas",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
                Text(
                    "Evaluá siempre las condiciones reales en el lugar antes de entrar al agua, y nunca te metas al agua solo/a.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5D4037)
                )
            }
        }
    }
}
