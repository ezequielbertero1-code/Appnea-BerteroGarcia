package com.berterogarcia.appnea.ui.detalleinmersion

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.condiciones.Aptitud
import com.berterogarcia.appnea.data.local.InmersionEntity
import com.berterogarcia.appnea.data.local.formatearDuracion
import com.berterogarcia.appnea.data.perfil.PerfilSimulado
import com.berterogarcia.appnea.data.perfil.PuntoPerfil
import com.berterogarcia.appnea.data.remote.descripcionClima
import com.berterogarcia.appnea.data.remote.emojiClima
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.Chips
import com.berterogarcia.appnea.ui.components.DatoChico
import com.berterogarcia.appnea.ui.components.FilaMosaicos
import com.berterogarcia.appnea.ui.components.Mosaico
import com.berterogarcia.appnea.ui.components.TarjetaAviso
import com.berterogarcia.appnea.ui.components.TarjetaHero
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.condiciones.FilaDato
import com.berterogarcia.appnea.ui.condiciones.Semaforo
import com.berterogarcia.appnea.ui.condiciones.TarjetaCondicion
import com.berterogarcia.appnea.ui.theme.TextoSecundario
import com.berterogarcia.appnea.ui.condiciones.colorAptitud
import com.berterogarcia.appnea.ui.condiciones.direccion
import com.berterogarcia.appnea.ui.condiciones.duracion
import com.berterogarcia.appnea.ui.condiciones.num
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.Duration as JDuration

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val HH_MM = DateTimeFormatter.ofPattern("HH:mm")
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
                        tint = Color(0xFFFF6B6B)   // el mismo rojo de error de Reutilizables.kt
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

        // ---- Exportar, fijo abajo ----
        BotonPrincipal(
            texto = "Exportar en formato UDDF",
            onClick = { exportarLauncher.launch("inmersion_${i.id}_${i.fecha}.uddf") },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
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
    val apnea = PerfilSimulado.esApnea(i)
    val fondo = formatearDuracion(i.tiempoFondo.inWholeSeconds.toInt())

    TarjetaHero(
        emoji = if (apnea) "🫧" else "🤿",
        valor = "${i.profundidadMetros.limpio()} m",
        subtitulo = "Profundidad máxima",
        modifier = Modifier.padding(bottom = 12.dp),
        derecha = {
            Text(if (apnea) "Apnea" else "Buceo", color = Acento, fontWeight = FontWeight.Bold)
            Text("⏱️ $fondo", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
        },
        abajo = {
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DatoChico("🕘 Entrada", i.horaEntrada.format(HH_MM))
                DatoChico("🕙 Salida", i.horaSalida.format(HH_MM))
                DatoChico("⌛ En el agua", duracion(minutosEnAgua(i)))
            }
        }
    )

    FilaMosaicos {
        Mosaico("Tiempo de fondo", fondo, "mm:ss")
        Mosaico(
            "Parada de seguridad",
            if (i.paradaSeguridadMin > 0f) "${i.paradaSeguridadMin.limpio()} min" else "Sin parada",
            if (i.paradaSeguridadMin > 0f) "a 5 m" else ""
        )
    }

    TarjetaCondicion("📝 Notas") {
        Text("Actividad", style = MaterialTheme.typography.labelMedium, color = TextoSecundario)
        Text(i.actividad.ifBlank { "—" }, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
        if (i.comentarios.isNotBlank()) {
            // Comentario como "cita": barrita celeste a la izquierda
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Box(Modifier.width(3.dp).fillMaxHeight().background(Acento))
                Spacer(Modifier.width(10.dp))
                Text("“${i.comentarios}”", fontStyle = FontStyle.Italic)
            }
        }
    }
}

@Composable
private fun PestanaGrafico(i: InmersionEntity, perfil: List<PuntoPerfil>) {
    // La curva se dibuja de izquierda a derecha cada vez que se entra a la pestaña
    val progreso = remember { Animatable(0f) }
    LaunchedEffect(perfil) {
        progreso.snapTo(0f)
        progreso.animateTo(1f, tween(durationMillis = 1200, easing = FastOutSlowInEasing))
    }

    TarjetaCondicion("📈 Perfil de la inmersión") {
        if (perfil.size < 2) {
            Text(
                "No hay datos suficientes para armar el perfil (revisá profundidad y tiempo de fondo).",
                color = TextoSecundario
            )
        } else {
            GraficoPerfil(perfil, progreso = progreso.value)
        }
    }

    if (perfil.size >= 2) {
        val apnea = PerfilSimulado.esApnea(i)
        val bajada = if (apnea) "${PerfilSimulado.VELOCIDAD_APNEA.limpio()} m/s"
        else "${(PerfilSimulado.DESCENSO_BUCEO * 60).limpio()} m/min"
        val subida = if (apnea) "${PerfilSimulado.VELOCIDAD_APNEA.limpio()} m/s"
        else "${(PerfilSimulado.ASCENSO_BUCEO * 60).limpio()} m/min"
        FilaMosaicos {
            Mosaico("⬇️ Descenso", bajada, "supuesto")
            Mosaico("⬆️ Ascenso", subida, "supuesto")
            Mosaico("⏱️ Duración", formatearDuracion(perfil.last().segundo), "del perfil")
        }
    }

    TarjetaAviso(
        titulo = "Perfil estimado",
        texto = "Se armó a partir de profundidad, tiempo de fondo y parada. " +
                "No es un registro de computadora de buceo.",
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun PestanaEquipo(i: InmersionEntity) {
    FilaMosaicos {
        Mosaico("⚖️ Lastre", "${i.lastreKg.limpio()} kg", "plomos")
        Mosaico("🧥 Protección", "${i.proteccionMm.limpio()} mm", "espesor del traje")
    }
    TarjetaCondicion("🎒 Equipamiento") {
        Chips(i.equipamiento.map { "${emojiEquipo(it)} $it" }, vacio = "No se cargó equipamiento")
    }
}

@Composable
private fun PestanaCondiciones(i: InmersionEntity) {
    val c = i.condiciones

    // ---- Clima grande, como ResumenAhora de Inicio (solo si se guardó con internet) ----
    if (c != null && c.conDatosOnline) {
        val aptitud = Aptitud.entries.firstOrNull { it.texto == c.aptitudAgua }
        TarjetaHero(
            emoji = emojiClima(c.codigoClima),
            valor = c.temperaturaC?.let { num(it, unidad = "°C") } ?: "${i.temperaturaAireC.limpio()}°C",
            subtitulo = c.codigoClima?.let { descripcionClima(it) } ?: "Sin datos del cielo",
            modifier = Modifier.padding(bottom = 12.dp),
            derecha = {
                c.sensacionTermicaC?.let {
                    Text("Sensación ${num(it, unidad = "°")}", style = MaterialTheme.typography.bodySmall)
                }
            },
            abajo = {
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DatoChico("💨 Viento", c.vientoKmh?.let { "${num(it)} ${direccion(c.direccionVientoGrados)}" } ?: "-")
                    DatoChico("Ráfagas", num(c.rafagasKmh, unidad = " km/h"))
                    DatoChico("💧 Humedad", num(c.humedadPorc?.toDouble(), unidad = "%"))
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DatoChico("Presión", num(c.presionHpa, unidad = " hPa"))
                    DatoChico("🌊 Olas", c.olaM?.let { num(it, 1, " m") } ?: "-")
                    DatoChico("🌡️ Agua", c.temperaturaAguaC?.let { num(it, 1, "°C") } ?: "-")
                }
                if (aptitud != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Para entrar al agua: ", style = MaterialTheme.typography.bodyMedium)
                        Semaforo(colorAptitud(aptitud), aptitud.texto)
                    }
                }
            }
        )
    }

    // ---- Mosaicos: marea, luna, corriente, visibilidad ----
    if (c != null) {
        FilaMosaicos {
            Mosaico("Marea", c.estadoMarea ?: "Sin datos", c.alturaMareaM?.let { num(it, 2, " m") } ?: "")
            Mosaico("Luna", "${emojiLuna(c.faseLunar)} ${c.iluminacionLunaPorc}%", c.faseLunar)
        }
        FilaMosaicos {
            Mosaico("🌀 Corriente", c.corrienteKmh?.let { num(it, 1, " km/h") } ?: "Sin datos", "al entrar")
            Mosaico("👁️ Visibilidad", "${i.visibilidadMetros.limpio()} m", "cargada por vos")
        }
    } else {
        TarjetaCondicion {
            Text("Esta inmersión no tiene condiciones registradas automáticamente.", color = TextoSecundario)
        }
    }

    // ---- Lo que cargó el usuario ----
    TarjetaCondicion("✍️ Tu registro") {
        Chips(i.caracteristicasAgua.map { "${emojiAgua(it)} $it" }, vacio = "Sin características del agua")
        Spacer(Modifier.height(10.dp))
        FilaDato("Temperatura del aire", "${i.temperaturaAireC.limpio()} °C")
        FilaDato("Temperatura del agua", "${i.temperaturaAguaC.limpio()} °C")
    }

    if (c != null && !c.conDatosOnline) {
        Text(
            "Sin conexión al guardar: solo hay datos astronómicos (luna).",
            style = MaterialTheme.typography.bodySmall,
            color = TextoSecundario
        )
    }
}

// ============================ Helpers ============================

/*
PestanaGeneral utilizaa BLoqueTexto pero como era la unica que lo usaba lo saque
@Composable
private fun BloqueTexto(etiqueta: String, texto: String) {
    Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = TextoSecundario)
    Text(texto.ifBlank { "—" }, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
}*/

// Minutos entre entrada y salida (si cruza la medianoche, suma 24 h)
private fun minutosEnAgua(i: InmersionEntity): Long {
    val m = JDuration.between(i.horaEntrada, i.horaSalida).toMinutes()
    return if (m >= 0) m else m + 24 * 60
}

// 18.0 -> "18", 18.5 -> "18.5"
private fun Float.limpio(): String = if (this % 1f == 0f) toInt().toString() else toString()

// 1.0 -> "1", 0.3 -> "0.3" (para las velocidades, que son Double)
private fun Double.limpio(): String = if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)

// ---------- Emojis (si no hay coincidencia devuelven uno genérico: nunca fallan) ----------

private fun emojiEquipo(item: String): String = when (item.lowercase()) {
    "guantes" -> "🧤"
    "botas" -> "🥾"
    "casco" -> "🪖"
    "ninguna" -> "🩳"
    else -> "🧥"   // Corto, Largo, Dos piezas, Semi seco, Seco
}

private fun emojiAgua(item: String): String = when (item.lowercase()) {
    "dulce" -> "💧"
    "salada" -> "🧂"
    "orilla" -> "🏖️"
    "bote" -> "🚤"
    "olas" -> "🌊"
    "corrientes" -> "🌀"
    "resaca" -> "↩️"
    else -> "💦"
}

// CondicionesRegistro guarda solo el nombre de la fase, así que se deduce el emoji del texto
private fun emojiLuna(fase: String): String {
    val f = fase.lowercase()
    return when {
        "nueva" in f -> "🌑"
        "llena" in f -> "🌕"
        "cuarto creciente" in f -> "🌓"
        "cuarto menguante" in f -> "🌗"
        "gibosa creciente" in f -> "🌔"
        "gibosa menguante" in f -> "🌖"
        "creciente" in f -> "🌒"
        "menguante" in f -> "🌘"
        else -> "🌙"
    }
}