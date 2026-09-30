package com.berterogarcia.appnea.ui.detalleinmersion

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.condiciones.Aptitud
import com.berterogarcia.appnea.data.condiciones.NivelActividad
import com.berterogarcia.appnea.data.local.InmersionEntity
import com.berterogarcia.appnea.data.local.formatearDuracion
import com.berterogarcia.appnea.data.perfil.PerfilSimulado
import com.berterogarcia.appnea.data.perfil.PuntoPerfil
import com.berterogarcia.appnea.data.remote.descripcionClima
import com.berterogarcia.appnea.data.remote.emojiClima
import com.berterogarcia.appnea.ui.condiciones.FilaDato
import com.berterogarcia.appnea.ui.condiciones.PecesActividad
import com.berterogarcia.appnea.ui.condiciones.Semaforo
import com.berterogarcia.appnea.ui.condiciones.TarjetaCondicion
import com.berterogarcia.appnea.ui.condiciones.TextoSecundario
import com.berterogarcia.appnea.ui.condiciones.colorAptitud
import com.berterogarcia.appnea.ui.condiciones.colorNivel
import com.berterogarcia.appnea.ui.condiciones.direccion
import com.berterogarcia.appnea.ui.condiciones.num
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val TITULOS = listOf("General", "Gráfico", "Equipo", "Condiciones")
private val ICONOS = listOf(R.drawable.ic_info, R.drawable.ic_reloj, R.drawable.ic_apnea, R.drawable.ic_clima)

@Composable
fun DetalleInmersionScreen(
    modifier: Modifier = Modifier,
    viewModel: DetalleInmersionViewModel,
    onEliminada: () -> Unit
) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    val inmersion by viewModel.inmersion.collectAsStateWithLifecycle()
    val perfil by viewModel.perfil.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { TITULOS.size }

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

    Column(modifier = modifier.fillMaxSize()) {
        // ---- Encabezado fijo ----
        Column(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                        tint = Color(0xFFFF6B6B)
                    )
                }
            }
            Text(i.fecha.format(FORMATO_FECHA), color = Color.White.copy(alpha = 0.7f))
        }

        // ---- Pestañas (mismo patrón que Inmersión / Entrenar) ----
        SecondaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            edgePadding = 8.dp
        ) {
            TITULOS.forEachIndexed { index, titulo ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(titulo) },
                    icon = { Icon(painterResource(ICONOS[index]), contentDescription = titulo) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (page) {
                    0 -> PestanaGeneral(i)
                    1 -> PestanaGrafico(i, perfil)
                    2 -> PestanaEquipo(i)
                    3 -> PestanaCondiciones(i)
                }
            }
        }

        Button(
            onClick = { exportarLauncher.launch("inmersion_${i.id}_${i.fecha}.uddf") },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
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

// ============================ Pestañas ============================

@Composable
private fun PestanaGeneral(i: InmersionEntity) {
    TarjetaFilas("Datos generales", listOf(
        "Fecha" to i.fecha.format(FORMATO_FECHA),
        "Hora de entrada" to i.horaEntrada.toString(),
        "Hora de salida" to i.horaSalida.toString()
    ))
    TarjetaFilas("Inmersión", listOf(
        "Profundidad máxima" to "${i.profundidadMetros.limpio()} m",
        "Tiempo de fondo" to formatearDuracion(i.tiempoFondo.inWholeSeconds.toInt()),
        "Parada de seguridad" to "${i.paradaSeguridadMin.limpio()} min"
    ))
    TarjetaCondicion("Notas y comentarios") {
        BloqueTexto("Notas (actividad)", i.actividad)
        BloqueTexto("Comentarios", i.comentarios)
    }
}

@Composable
private fun PestanaGrafico(i: InmersionEntity, perfil: List<PuntoPerfil>) {
    TarjetaCondicion("Perfil de la inmersión") {
        if (perfil.size < 2) {
            Text(
                "No hay datos suficientes para armar el perfil (revisá profundidad y tiempo de fondo).",
                color = TextoSecundario
            )
        } else {
            GraficoPerfil(perfil)
            Spacer(Modifier.height(8.dp))
            FilaDato("Tipo de perfil", if (PerfilSimulado.esApnea(i)) "Apnea" else "Buceo con parada")
            FilaDato("Duración del perfil", formatearDuracion(perfil.last().segundo))
            FilaDato("Profundidad máxima", "${i.profundidadMetros.limpio()} m")
        }
        Text(
            "Perfil estimado a partir de los datos cargados; no es un registro de computadora de buceo.",
            style = MaterialTheme.typography.bodySmall,
            color = TextoSecundario,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun PestanaEquipo(i: InmersionEntity) {
    TarjetaFilas("Equipo", listOf(
        "Lastre" to "${i.lastreKg.limpio()} kg",
        "Protección" to "${i.proteccionMm.limpio()} mm",
        "Equipamiento" to i.equipamiento.joinToString().ifBlank { "—" }
    ))
}

@Composable
private fun PestanaCondiciones(i: InmersionEntity) {
    // Lo que cargó el usuario en el formulario
    TarjetaFilas("Datos cargados", listOf(
        "Agua" to i.caracteristicasAgua.joinToString().ifBlank { "—" },
        "Temperatura del aire" to "${i.temperaturaAireC.limpio()} °C",
        "Temperatura del agua" to "${i.temperaturaAguaC.limpio()} °C",
        "Visibilidad" to "${i.visibilidadMetros.limpio()} m"
    ))

    // Lo guardado automáticamente al registrar (null en inmersiones viejas)
    val c = i.condiciones
    if (c == null) {
        TarjetaCondicion {
            Text(
                "Esta inmersión no tiene condiciones registradas automáticamente.",
                color = TextoSecundario
            )
        }
        return
    }

    TarjetaFilas("Clima", listOf(
        "Estado" to c.codigoClima?.let { "${emojiClima(it)} ${descripcionClima(it)}" },
        "Temperatura" to c.temperaturaC?.let { num(it, 0, " °C") },
        "Sensación térmica" to c.sensacionTermicaC?.let { num(it, 0, " °C") },
        "Humedad" to c.humedadPorc?.let { "$it %" },
        "Presión" to c.presionHpa?.let { num(it, 0, " hPa") },
        "Nubosidad" to c.nubosidadPorc?.let { "$it %" },
        "Viento" to c.vientoKmh?.let { "${num(it)} km/h ${direccion(c.direccionVientoGrados)}" },
        "Ráfagas" to c.rafagasKmh?.let { num(it, 0, " km/h") }
    ))

    val aptitud = Aptitud.entries.firstOrNull { it.texto == c.aptitudAgua }
    TarjetaFilas("Mar", listOf(
        "Olas" to c.olaM?.let { num(it, 1, " m") },
        "Período" to c.periodoOlaS?.let { num(it, 0, " s") },
        "Dirección de olas" to c.direccionOlaGrados?.let { direccion(it) },
        "Mar de fondo" to c.marDeFondoM?.let { num(it, 1, " m") },
        "Temperatura del agua" to c.temperaturaAguaC?.let { num(it, 1, " °C") },
        "Corriente" to c.corrienteKmh?.let { num(it, 1, " km/h") }
    )) {
        if (aptitud != null) {
            Semaforo(colorAptitud(aptitud), "Condiciones ${aptitud.texto.lowercase()}")
            Spacer(Modifier.height(6.dp))
        }
    }

    TarjetaFilas("Marea", listOf(
        "Estado" to c.estadoMarea,
        "Altura" to c.alturaMareaM?.let { num(it, 2, " m") }
    ))

    val nivel = NivelActividad.entries.firstOrNull { it.texto == c.nivelActividad }
        ?: NivelActividad.desde(c.actividadPesca)
    TarjetaCondicion("Luna y pesca") {
        FilaDato("Fase lunar", c.faseLunar.ifBlank { "—" })
        FilaDato("Iluminación", "${c.iluminacionLunaPorc} %")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            PecesActividad(nivel)
            Spacer(Modifier.width(8.dp))
            Text(
                "${nivel.texto} (${c.actividadPesca}/100)",
                color = colorNivel(nivel),
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (!c.conDatosOnline) {
        Text(
            "Sin conexión al guardar: solo hay datos astronómicos (luna y actividad).",
            style = MaterialTheme.typography.bodySmall,
            color = TextoSecundario
        )
    }
}

// ============================ Helpers ============================

/** Tarjeta con filas; oculta las filas con valor null y no dibuja nada si quedan todas vacías. */
@Composable
private fun TarjetaFilas(
    titulo: String,
    filas: List<Pair<String, String?>>,
    encabezado: @Composable ColumnScope.() -> Unit = {}
) {
    val visibles = filas.mapNotNull { (etiqueta, valor) -> valor?.let { etiqueta to it } }
    if (visibles.isEmpty()) return
    TarjetaCondicion(titulo) {
        encabezado()
        visibles.forEach { (etiqueta, valor) -> FilaDato(etiqueta, valor) }
    }
}

@Composable
private fun BloqueTexto(etiqueta: String, texto: String) {
    Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = TextoSecundario)
    Text(texto.ifBlank { "—" }, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
}

// 18.0 -> "18", 18.5 -> "18.5"
private fun Float.limpio(): String = if (this % 1f == 0f) toInt().toString() else toString()