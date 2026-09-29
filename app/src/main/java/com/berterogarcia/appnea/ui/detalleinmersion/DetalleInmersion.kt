package com.berterogarcia.appnea.ui.detalleinmersion

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.formatearDuracion
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun DetalleInmersionScreen(
    modifier: Modifier = Modifier,
    viewModel: DetalleInmersionViewModel,
    onEliminada: () -> Unit
) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    val inmersion by viewModel.inmersion.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Abre el selector de Android para elegir dónde guardar el archivo
    val exportarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) scope.launch {
            val ok = viewModel.exportarUddf(uri)
            Toast.makeText(
                context,
                if (ok) "Inmersión exportada en UDDF" else "No se pudo exportar",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val i = inmersion ?: return   // todavía cargando desde Room

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                i.lugar,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { mostrarDialogo = true }) {
                Icon(
                    painterResource(R.drawable.ic_borrar),
                    contentDescription = "Eliminar inmersión",
                    tint = Color(0xFFFF6B6B)   // el mismo rojo de error de Reutilizables.kt
                )
            }
        }
        Text(i.fecha.format(FORMATO_FECHA), color = Color.White.copy(alpha = 0.7f))
        Spacer(Modifier.height(16.dp))

        TarjetaDetalle("Datos generales", listOf(
            "Fecha" to i.fecha.format(FORMATO_FECHA),
            "Hora de entrada" to i.horaEntrada.toString(),
            "Hora de salida" to i.horaSalida.toString()
        ))
        TarjetaDetalle("Inmersión", listOf(
            "Profundidad máxima" to "${i.profundidadMetros.limpio()} m",
            "Tiempo de fondo" to formatearDuracion(i.tiempoFondo.inWholeSeconds.toInt()),
            "Parada de seguridad" to "${i.paradaSeguridadMin.limpio()} min"
        ))
        TarjetaDetalle("Condiciones", listOf(
            "Agua" to i.caracteristicasAgua.joinToString().ifBlank { "—" },
            "Temperatura del aire" to "${i.temperaturaAireC.limpio()} °C",
            "Temperatura del agua" to "${i.temperaturaAguaC.limpio()} °C",
            "Visibilidad" to "${i.visibilidadMetros.limpio()} m"
        ))
        TarjetaDetalle("Equipo", listOf(
            "Lastre" to "${i.lastreKg.limpio()} kg",
            "Protección" to "${i.proteccionMm.limpio()} mm",
            "Equipamiento" to i.equipamiento.joinToString().ifBlank { "—" }
        ))
        if (i.actividad.isNotBlank() || i.comentarios.isNotBlank()) {
            TarjetaTexto("Notas", listOfNotNull(
                i.actividad.takeIf { it.isNotBlank() }?.let { "Actividad: $it" },
                i.comentarios.takeIf { it.isNotBlank() }?.let { "Comentarios: $it" }
            ))
        }

        Button(
            onClick = { exportarLauncher.launch("inmersion_${i.id}_${i.fecha}.uddf") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Exportar en formato UDDF")
        }
    }
    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text("Eliminar inmersión") },
            text = { Text("¿Seguro que querés eliminar la inmersión en ${i.lugar}? No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogo = false
                    viewModel.eliminar {
                        Toast.makeText(context, "Inmersión eliminada", Toast.LENGTH_SHORT).show()
                        onEliminada()
                    }
                }) { Text("Eliminar", color = Color(0xFFFF6B6B)) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun TarjetaDetalle(titulo: String, filas: List<Pair<String, String>>) {
    TarjetaBase(titulo) {
        filas.forEach { (etiqueta, valor) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(etiqueta, color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.width(12.dp))
                Text(valor, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TarjetaTexto(titulo: String, lineas: List<String>) {
    TarjetaBase(titulo) {
        lineas.forEach { Text(it, modifier = Modifier.padding(vertical = 3.dp)) }
    }
}

@Composable
private fun TarjetaBase(titulo: String, contenido: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF03141F), contentColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            contenido()
        }
    }
}

// 18.0 -> "18", 18.5 -> "18.5"
private fun Float.limpio(): String = if (this % 1f == 0f) toInt().toString() else toString()