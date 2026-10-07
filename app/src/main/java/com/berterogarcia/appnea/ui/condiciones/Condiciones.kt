package com.berterogarcia.appnea.ui.condiciones

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.data.condiciones.Condiciones
import com.berterogarcia.appnea.data.remote.descripcionClima
import com.berterogarcia.appnea.data.remote.emojiClima
import com.berterogarcia.appnea.ui.clima.ClimaViewModel
import com.berterogarcia.appnea.ui.clima.SelectorUbicacion
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Pantalla completa de condiciones, en pestañas deslizables:
 * Resumen · Mareas · Sol y Luna · 7 días.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CondicionesScreen(viewModel: ClimaViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val titulos = listOf("Resumen", "Mareas", "Sol y Luna", "7 días")
    val pagerState = rememberPagerState(initialPage = 0) { titulos.size }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = if (uiState.buscandoUbicacion) "📍 Buscando tu ubicación…"
            else "📍 ${uiState.ubicacion.nombre ?: "Ubicación seleccionada"}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp)
        )
        SelectorUbicacion(
            viewModel = viewModel,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        SecondaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            edgePadding = 8.dp
        ) {
            titulos.forEachIndexed { index, titulo ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(titulo) }
                )
            }
        }

        val c = uiState.condiciones
        when {
            c == null && uiState.cargando -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }

            c == null -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(uiState.error ?: "Sin datos", textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                BotonPrincipal(texto = "Reintentar", onClick = { viewModel.cargarClima() })
            }

            else -> HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        "HOY, ${diaLargo(c.ahora.toLocalDate()).uppercase()} · ${hora(c.ahora)} h",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextoSecundario,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    when (page) {
                        0 -> PaginaResumen(c)
                        1 -> PaginaMareas(c)
                        2 -> PaginaSolLuna(c)
                        else -> PaginaSemana(c)
                    }
                    Text(
                        "Fuentes: Open-Meteo (clima y mar, modelo global) y cálculo astronómico propio. " +
                            "Datos orientativos: evaluá siempre las condiciones reales en el lugar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )
                }
            }
        }
    }
}

// ============================== RESUMEN ==============================

@Composable
private fun PaginaResumen(c: Condiciones) {
    val clima = c.climaActual
    val mar = c.marActual

    TarjetaCondicion(resaltada = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emojiClima(clima?.codigoClima, clima?.esDeDia ?: true), fontSize = 48.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(num(clima?.temperaturaC, unidad = "°C"), fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Text(clima?.codigoClima?.let { descripcionClima(it) } ?: "Sin datos")
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Para entrar al agua", style = MaterialTheme.typography.labelMedium, color = TextoSecundario)
                Semaforo(colorAptitud(c.aptitud.nivel), c.aptitud.nivel.texto)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(c.aptitud.motivos.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
    }

    TarjetaCondicion("Atmósfera") {
        val tendencia = c.tendenciaPresionHpa3h
        FilaDato("Sensación térmica", num(clima?.sensacionTermicaC, unidad = "°C"))
        FilaDato("Punto de rocío", num(clima?.puntoRocioC, unidad = "°C"))
        FilaDato("Humedad", num(clima?.humedadPorc?.toDouble(), unidad = "%"))
        FilaDato(
            "Presión",
            num(clima?.presionHpa, unidad = " hPa") +
                (tendencia?.let { " (${if (it >= 0) "+" else ""}${"%.1f".format(it)} en 3 h)" } ?: "")
        )
        FilaDato("Nubosidad", num(clima?.nubosidadPorc?.toDouble(), unidad = "%"))
        FilaDato("Visibilidad", clima?.visibilidadM?.let { if (it >= 1000) "${(it / 1000).roundToInt()} km" else "${it.roundToInt()} m" } ?: "-")
        FilaDato("Precipitación", num(clima?.precipitacionMm, 1, " mm"))
        FilaDato("Prob. de lluvia", num(clima?.probabilidadLluviaPorc?.toDouble(), unidad = "%"))
        FilaDato("Índice UV", num(clima?.indiceUv, 1))
    }

    TarjetaCondicion("Viento") {
        FilaDato("Velocidad", num(clima?.vientoKmh, unidad = " km/h"))
        FilaDato("Dirección", "${direccion(clima?.direccionVientoGrados)} (${num(clima?.direccionVientoGrados, unidad = "°")})")
        FilaDato("Ráfagas", num(clima?.rafagasKmh, unidad = " km/h"))
        FilaDato("En nudos", clima?.vientoKmh?.let { "${(it / 1.852).roundToInt()} kn" } ?: "-")
    }

    TarjetaCondicion("Mar") {
        if (!c.marDisponible || mar == null) {
            Text("No hay datos marinos para este punto (puede ser porque está lejos de la costa).")
        } else {
            FilaDato("Altura de ola", num(mar.olaM, 1, " m"))
            FilaDato("Dirección de ola", direccion(mar.direccionOlaGrados))
            FilaDato("Período", num(mar.periodoOlaS, unidad = " s"))
            FilaDato("Ola de viento", num(mar.olaVientoM, 1, " m"))
            FilaDato("Mar de fondo", "${num(mar.marDeFondoM, 1, " m")} ${direccion(mar.direccionMarDeFondoGrados)} · ${num(mar.periodoMarDeFondoS, unidad = " s")}")
            FilaDato("Temperatura del agua", num(mar.temperaturaAguaC, 1, "°C"))
            FilaDato("Corriente", "${num(mar.corrienteKmh, 1, " km/h")} ${direccion(mar.direccionCorrienteGrados)}")
            c.estadoMarea?.let {
                FilaDato("Marea", "${if (it.subiendo) "Subiendo" else "Bajando"} · ${"%.2f".format(it.alturaM)} m")
            }
        }
    }

    TarjetaCondicion("Próximas 24 horas") {
        val desde = c.ahora.truncatedTo(ChronoUnit.HOURS)
        val horas = c.horasClima.filter { !it.hora.isBefore(desde) }.take(24)
        val marPorHora = c.horasMar.associateBy { it.hora }
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            horas.forEach { h ->
                Column(
                    modifier = Modifier.widthIn(min = 64.dp).padding(end = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(hora(h.hora), style = MaterialTheme.typography.labelMedium, color = TextoSecundario)
                    Text(emojiClima(h.codigoClima, h.esDeDia ?: true), fontSize = 22.sp)
                    Text(num(h.temperaturaC, unidad = "°"), fontWeight = FontWeight.Bold)
                    Text("💨${num(h.vientoKmh)}", style = MaterialTheme.typography.bodySmall)
                    Text(direccion(h.direccionVientoGrados), style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                    marPorHora[h.hora]?.olaM?.let {
                        Text("🌊${"%.1f".format(it)}", style = MaterialTheme.typography.bodySmall)
                    }
                    h.probabilidadLluviaPorc?.takeIf { it > 0 }?.let {
                        Text("☔$it%", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

// ============================== MAREAS ==============================

@Composable
private fun PaginaMareas(c: Condiciones) {
    val estado = c.estadoMarea
    if (estado == null) {
        TarjetaCondicion("Mareas") {
            Text("No hay datos de nivel del mar para este punto. Probá con una ubicación sobre la costa.")
        }
        return
    }

    TarjetaCondicion("Estado actual") {
        val anterior = estado.anterior
        val siguiente = estado.siguiente
        val progreso = if (anterior != null && siguiente != null) {
            val total = ChronoUnit.MINUTES.between(anterior.hora, siguiente.hora).toFloat()
            ChronoUnit.MINUTES.between(anterior.hora, c.ahora) / total
        } else 0.5f
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (estado.subiendo) "subiendo" else "bajando", color = TextoSecundario)
            Text("%.2f m".format(estado.alturaM), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DatoGrande(
                hora(anterior?.hora),
                if (anterior?.esPleamar == true) "Pleamar" else "Bajamar",
                color = if (anterior?.esPleamar == true) Acento else ColorBajamar
            )
            MedioCirculo(progreso, if (estado.subiendo) Acento else ColorBajamar, ancho = 110.dp)
            DatoGrande(
                hora(siguiente?.hora),
                if (siguiente?.esPleamar == true) "Pleamar" else "Bajamar",
                color = if (siguiente?.esPleamar == true) Acento else ColorBajamar
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.15f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            DatoGrande(
                anterior?.let { duracion(ChronoUnit.MINUTES.between(it.hora, c.ahora)) } ?: "-",
                "desde la ${if (anterior?.esPleamar == true) "pleamar" else "bajamar"}"
            )
            DatoGrande(
                siguiente?.let { duracion(ChronoUnit.MINUTES.between(c.ahora, it.hora)) } ?: "-",
                "para la ${if (siguiente?.esPleamar == true) "pleamar" else "bajamar"}"
            )
        }
        if (anterior != null && siguiente != null) {
            Spacer(Modifier.height(8.dp))
            FilaDato("Amplitud de esta marea", "%.2f m".format(kotlin.math.abs(siguiente.alturaM - anterior.alturaM)))
        }
    }

    TarjetaCondicion("Pleamares y bajamares de hoy") {
        GraficoMarea(
            fecha = c.ahora.toLocalDate(),
            horas = c.horasMar,
            extremos = c.mareas,
            amanecer = c.astroHoy.amanecer,
            atardecer = c.astroHoy.atardecer,
            ahora = c.ahora
        )
        Text(
            "Celeste: pleamar · Naranja: bajamar · Franja clara: horas de luz · Línea: ahora",
            style = MaterialTheme.typography.bodySmall,
            color = TextoSecundario
        )
    }

    TarjetaCondicion("Próximos días") {
        c.mareas.filter { !it.hora.toLocalDate().isBefore(c.ahora.toLocalDate()) }
            .groupBy { it.hora.toLocalDate() }
            .forEach { (fecha, extremos) ->
                Text(diaCorto(fecha, c.ahora.toLocalDate()), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    extremos.forEach { e ->
                        Column {
                            Text(
                                "${if (e.esPleamar) "▲" else "▼"} ${hora(e.hora)}",
                                color = if (e.esPleamar) Acento else ColorBajamar,
                                fontWeight = FontWeight.Bold
                            )
                            Text("%.2f m".format(e.alturaM), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
    }

    Text(
        "Las mareas salen del modelo global de Open-Meteo (~8 km). Cerca de la costa pueden " +
            "diferir de la tabla oficial del Servicio de Hidrografía Naval: usalas como referencia.",
        style = MaterialTheme.typography.bodySmall,
        color = ColorRegular,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

// ============================== SOL Y LUNA ==============================

@Composable
private fun PaginaSolLuna(c: Condiciones) {
    val a = c.astroHoy
    TarjetaCondicion("Sol") {
        val amanecer = a.amanecer
        val atardecer = a.atardecer
        val esDeDia = amanecer != null && atardecer != null && c.ahora.isAfter(amanecer) && c.ahora.isBefore(atardecer)
        val progreso = if (amanecer != null && atardecer != null) {
            (ChronoUnit.MINUTES.between(amanecer, c.ahora).toFloat() /
                ChronoUnit.MINUTES.between(amanecer, atardecer).toFloat()).coerceIn(0f, 1f)
        } else 0f
        Text(if (esDeDia) "día" else "noche", color = TextoSecundario, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DatoGrande(hora(amanecer), "Salida", color = ColorRegular)
            MedioCirculo(progreso, ColorRegular, ancho = 110.dp)
            DatoGrande(hora(atardecer), "Puesta", color = ColorRegular)
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.15f))
        if (amanecer != null && atardecer != null) {
            if (esDeDia) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    DatoGrande(duracion(ChronoUnit.MINUTES.between(amanecer, c.ahora)), "desde la salida")
                    DatoGrande(duracion(ChronoUnit.MINUTES.between(c.ahora, atardecer)), "para la puesta")
                }
                Spacer(Modifier.height(8.dp))
            }
            FilaDato("Duración del día", duracion(ChronoUnit.MINUTES.between(amanecer, atardecer)))
        }
        FilaDato("Primera luz (crepúsculo civil)", hora(a.inicioCrepusculo))
        FilaDato("Última luz", hora(a.finCrepusculo))
        FilaDato("Mediodía solar", hora(a.mediodiaSolar))
        FilaDato("Altura máxima del sol", "${a.alturaMaximaSol.roundToInt()}°")
        FilaDato("Índice UV máximo", num(c.dias.firstOrNull()?.uvMax, 1))
    }

    TarjetaCondicion("Luna") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(a.emojiFase, fontSize = 52.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(a.fase, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Iluminada ${a.iluminacionPorc}% · ${"%.1f".format(a.edadLunaDias)} días", color = TextoSecundario)
            }
        }
        Spacer(Modifier.height(8.dp))
        FilaDato("Salida de la luna", hora(a.salidaLuna))
        FilaDato("Luna en lo alto", hora(a.transitoLuna))
        FilaDato("Puesta de la luna", hora(a.puestaLuna))
        FilaDato("Luna en lo bajo", hora(a.antitransitoLuna))
        FilaDato("Próxima luna llena", diaCorto(a.proximaLunaLlena, c.ahora.toLocalDate()))
        FilaDato("Próxima luna nueva", diaCorto(a.proximaLunaNueva, c.ahora.toLocalDate()))
    }

    TarjetaCondicion("Fases de la semana") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            c.astroDias.forEach { d ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(d.emojiFase, fontSize = 22.sp)
                    Text("${d.iluminacionPorc}%", style = MaterialTheme.typography.bodySmall)
                    Text(
                        diaCorto(d.fecha, c.ahora.toLocalDate()).take(3),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextoSecundario
                    )
                }
            }
        }
    }
}

// ============================== 7 DÍAS ==============================

@Composable
private fun PaginaSemana(c: Condiciones) {
    val hoy = c.ahora.toLocalDate()
    c.dias.forEach { d ->
        val astro = c.astroDias.firstOrNull { it.fecha == d.fecha }
        val mareasDia = c.mareas.filter { it.hora.toLocalDate() == d.fecha }
        TarjetaCondicion(resaltada = d.fecha == hoy) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(diaCorto(d.fecha, hoy), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(d.codigoClima?.let { descripcionClima(it) } ?: "-", style = MaterialTheme.typography.bodySmall)
                }
                Text(emojiClima(d.codigoClima), fontSize = 32.sp)
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(num(d.maximaC, unidad = "°"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(num(d.minimaC, unidad = "°"), color = TextoSecundario)
                }
            }
            Spacer(Modifier.height(8.dp))
            FilaDato("Viento máx / ráfagas", "${num(d.vientoMaxKmh)} / ${num(d.rafagasMaxKmh)} km/h ${direccion(d.direccionVientoGrados)}")
            FilaDato("Lluvia", "${num(d.lluviaMm, 1, " mm")} (${num(d.probabilidadLluviaPorc?.toDouble(), unidad = "%")})")
            FilaDato("Ola máxima", num(d.olaMaxM, 1, " m"))
            FilaDato("UV máximo", num(d.uvMax, 1))
            if (astro != null) {
                FilaDato("Sol", "${hora(astro.amanecer)} - ${hora(astro.atardecer)}")
                FilaDato("Luna", "${astro.emojiFase} ${astro.fase} (${astro.iluminacionPorc}%)")
            }
            if (mareasDia.isNotEmpty()) {
                FilaDato("Mareas", mareasDia.joinToString("  ") { "${if (it.esPleamar) "▲" else "▼"}${hora(it.hora)}" })
            }
        }
    }
}
