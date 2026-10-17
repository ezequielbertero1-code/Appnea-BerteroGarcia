package com.berterogarcia.appnea.ui.condiciones

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.condiciones.Aptitud
import com.berterogarcia.appnea.data.condiciones.Condiciones
import com.berterogarcia.appnea.ui.clima.ClimaViewModel
import com.berterogarcia.appnea.ui.clima.SelectorUbicacion
import com.berterogarcia.appnea.ui.components.BarraSuperior
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.NavegacionBarra
import com.berterogarcia.appnea.ui.components.descripcionClima
import com.berterogarcia.appnea.ui.components.nombre
import com.berterogarcia.appnea.ui.components.nombreFaseLunar
import com.berterogarcia.appnea.ui.components.resolver
import com.berterogarcia.appnea.ui.components.texto
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorBajamar
import com.berterogarcia.appnea.ui.theme.ColorRegular
import com.berterogarcia.appnea.ui.theme.ContenedorSecundario
import com.berterogarcia.appnea.ui.theme.Divisor
import com.berterogarcia.appnea.ui.theme.SuperficieTonal
import com.berterogarcia.appnea.ui.theme.SuperficieTonalAlta
import com.berterogarcia.appnea.ui.theme.TextoSobreContenedorSecundario
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val PESTANIAS = listOf(
    R.string.condiciones_tab_resumen,
    R.string.condiciones_tab_mareas,
    R.string.condiciones_tab_sol_luna,
    R.string.condiciones_tab_semana
)

/**
 * Pantalla completa de condiciones (estilo "Appnea circular"), en pestañas deslizables:
 * Resumen · Mareas · Sol y Luna · 7 días. [onVolver] null = sin flecha (no hay a dónde volver).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CondicionesScreen(viewModel: ClimaViewModel, modifier: Modifier = Modifier, onVolver: (() -> Unit)? = null) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = 0) { PESTANIAS.size }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        BarraSuperior(
            titulo = stringResource(R.string.condiciones_titulo),
            navegacion = if (onVolver != null) NavegacionBarra.VOLVER else NavegacionBarra.NINGUNA,
            onNavegacion = { onVolver?.invoke() },
            acciones = {
                IconButton(onClick = { viewModel.cargarClima() }, enabled = !uiState.cargando) {
                    Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.condiciones_actualizar))
                }
            }
        )
        PrimaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            contentColor = Acento,
            edgePadding = 8.dp,
            divider = { HorizontalDivider(color = Divisor) }
        ) {
            PESTANIAS.forEachIndexed { index, titulo ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(stringResource(titulo)) },
                    selectedContentColor = Acento,
                    unselectedContentColor = TextoSuperficieVariante
                )
            }
        }
        SelectorUbicacion(
            viewModel = viewModel,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
        )

        val c = uiState.condiciones
        when {
            c == null && uiState.cargando -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator(color = Acento) }

            c == null -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    uiState.error?.resolver() ?: stringResource(R.string.detalle_sin_datos),
                    color = TextoSuperficieVariante,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                BotonPrincipal(texto = stringResource(R.string.condiciones_reintentar), onClick = { viewModel.cargarClima() })
            }

            else -> HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
                ) {
                    Text(
                        stringResource(R.string.condiciones_hoy, diaLargo(c.ahora.toLocalDate()), hora(c.ahora)),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSuperficieVariante,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                    when (page) {
                        0 -> PaginaResumen(c)
                        1 -> PaginaMareas(c)
                        2 -> PaginaSolLuna(c)
                        else -> PaginaSemana(c)
                    }
                    Text(
                        stringResource(R.string.condiciones_fuentes),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSuperficieVariante,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 16.dp)
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
    val esDeDia = clima?.esDeDia ?: true

    TarjetaCondicion {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(iconoClima(clima?.codigoClima, esDeDia)),
                contentDescription = null,
                tint = colorIconoClima(clima?.codigoClima, esDeDia),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(num(clima?.temperaturaC, unidad = "°"), fontSize = 40.sp, lineHeight = 44.sp, color = TextoSuperficie)
                Text(
                    listOfNotNull(
                        clima?.codigoClima?.let { descripcionClima(it) },
                        clima?.sensacionTermicaC?.let { stringResource(R.string.detalle_sensacion, num(it, unidad = "°")) }
                    ).joinToString(" · ").ifBlank { stringResource(R.string.detalle_sin_datos) },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuperficieVariante
                )
            }
        }
        // Semáforo como contenedor tonal del color de la aptitud
        AvisoAptitud(c.aptitud.nivel, c.aptitud.motivos.map { it.texto() }.joinToString(" · "), Modifier.padding(top = 14.dp))
    }

    TarjetaCondicion(stringResource(R.string.condiciones_atmosfera)) {
        val tendencia = c.tendenciaPresionHpa3h
        FilaDato(stringResource(R.string.condiciones_sensacion), num(clima?.sensacionTermicaC, unidad = "°C"))
        FilaDato(stringResource(R.string.condiciones_rocio), num(clima?.puntoRocioC, unidad = "°C"))
        FilaDato(stringResource(R.string.detalle_humedad), num(clima?.humedadPorc?.toDouble(), unidad = "%"))
        FilaDato(
            stringResource(R.string.detalle_presion),
            num(clima?.presionHpa, unidad = " hPa") +
                (tendencia?.let { " " + stringResource(R.string.condiciones_tendencia, "${if (it >= 0) "+" else ""}${"%.1f".format(it)}") } ?: "")
        )
        FilaDato(stringResource(R.string.condiciones_nubosidad), num(clima?.nubosidadPorc?.toDouble(), unidad = "%"))
        FilaDato(stringResource(R.string.detalle_visibilidad), clima?.visibilidadM?.let { if (it >= 1000) "${(it / 1000).roundToInt()} km" else "${it.roundToInt()} m" } ?: "-")
        FilaDato(stringResource(R.string.condiciones_precipitacion), num(clima?.precipitacionMm, 1, " mm"))
        FilaDato(stringResource(R.string.condiciones_prob_lluvia), num(clima?.probabilidadLluviaPorc?.toDouble(), unidad = "%"))
        FilaDato(stringResource(R.string.condiciones_uv), num(clima?.indiceUv, 1))
    }

    TarjetaCondicion(stringResource(R.string.detalle_viento)) {
        FilaDato(stringResource(R.string.condiciones_velocidad), num(clima?.vientoKmh, unidad = " km/h"))
        FilaDato(stringResource(R.string.condiciones_direccion), "${direccion(clima?.direccionVientoGrados)} (${num(clima?.direccionVientoGrados, unidad = "°")})")
        FilaDato(stringResource(R.string.detalle_rafagas), num(clima?.rafagasKmh, unidad = " km/h"))
        FilaDato(stringResource(R.string.condiciones_nudos), clima?.vientoKmh?.let { "${(it / 1.852).roundToInt()} kn" } ?: "-")
    }

    TarjetaCondicion(stringResource(R.string.condiciones_mar)) {
        if (!c.marDisponible || mar == null) {
            Text(stringResource(R.string.condiciones_sin_mar), style = MaterialTheme.typography.bodyMedium, color = TextoSuperficieVariante)
        } else {
            FilaDato(stringResource(R.string.condiciones_altura_ola), num(mar.olaM, 1, " m"))
            FilaDato(stringResource(R.string.condiciones_direccion_ola), direccion(mar.direccionOlaGrados))
            FilaDato(stringResource(R.string.condiciones_periodo), num(mar.periodoOlaS, unidad = " s"))
            FilaDato(stringResource(R.string.condiciones_ola_viento), num(mar.olaVientoM, 1, " m"))
            FilaDato(stringResource(R.string.condiciones_mar_fondo), "${num(mar.marDeFondoM, 1, " m")} ${direccion(mar.direccionMarDeFondoGrados)} · ${num(mar.periodoMarDeFondoS, unidad = " s")}")
            FilaDato(stringResource(R.string.nueva_temp_agua), num(mar.temperaturaAguaC, 1, "°C"))
            FilaDato(stringResource(R.string.detalle_corriente), "${num(mar.corrienteKmh, 1, " km/h")} ${direccion(mar.direccionCorrienteGrados)}")
            c.estadoMarea?.let {
                FilaDato(
                    stringResource(R.string.detalle_marea),
                    stringResource(
                        if (it.subiendo) R.string.condiciones_marea_subiendo else R.string.condiciones_marea_bajando,
                        "%.2f".format(it.alturaM)
                    )
                )
            }
        }
    }

    Text(
        stringResource(R.string.condiciones_proximas_24),
        style = MaterialTheme.typography.titleSmall,
        color = Acento,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp)
    )
    val desde = c.ahora.truncatedTo(ChronoUnit.HOURS)
    val horas = c.horasClima.filter { !it.hora.isBefore(desde) }.take(24)
    val marPorHora = c.horasMar.associateBy { it.hora }
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        horas.forEachIndexed { i, h ->
            val ahora = i == 0
            // Pastilla por hora: la de "ahora" con relleno tonal, como en el mockup
            Column(
                modifier = Modifier
                    .widthIn(min = 60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (ahora) ContenedorSecundario else SuperficieTonal)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val colorTexto = if (ahora) TextoSobreContenedorSecundario else TextoSuperficieVariante
                Text(
                    if (ahora) stringResource(R.string.condiciones_ahora) else hora(h.hora),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorTexto
                )
                Icon(
                    painterResource(iconoClima(h.codigoClima, h.esDeDia ?: true)),
                    contentDescription = h.codigoClima?.let { descripcionClima(it) },
                    tint = colorIconoClima(h.codigoClima, h.esDeDia ?: true),
                    modifier = Modifier.size(24.dp)
                )
                Text(num(h.temperaturaC, unidad = "°"), style = MaterialTheme.typography.titleSmall, color = TextoSuperficie)
                DatoHora(R.drawable.ic_ms_air, "${num(h.vientoKmh)} ${direccion(h.direccionVientoGrados)}", colorTexto)
                marPorHora[h.hora]?.olaM?.let { DatoHora(R.drawable.ic_ms_waves, "%.1f".format(it), colorTexto) }
                h.probabilidadLluviaPorc?.takeIf { it > 0 }?.let { DatoHora(R.drawable.ic_ms_water_drop, "$it%", colorTexto) }
            }
        }
    }
}

/** Ícono chico + valor (viento, ola, lluvia) dentro de la pastilla de cada hora. */
@Composable
private fun DatoHora(icono: Int, valor: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icono), contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(2.dp))
        Text(valor, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/** "Buenas para entrar al agua" en un contenedor del color del semáforo, con los motivos debajo. */
@Composable
fun AvisoAptitud(nivel: Aptitud, motivos: String, modifier: Modifier = Modifier) {
    val color = colorAptitud(nivel)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (nivel == Aptitud.BUENA) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
            contentDescription = null,
            tint = color
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                stringResource(R.string.condiciones_aptitud, nivel.nombre()),
                style = MaterialTheme.typography.titleSmall,
                color = color
            )
            if (motivos.isNotBlank()) {
                Text(motivos, style = MaterialTheme.typography.bodySmall, color = TextoSuperficieVariante)
            }
        }
    }
}

// ============================== MAREAS ==============================

@Composable
private fun PaginaMareas(c: Condiciones) {
    val estado = c.estadoMarea
    if (estado == null) {
        TarjetaCondicion(stringResource(R.string.condiciones_tab_mareas)) {
            Text(stringResource(R.string.condiciones_sin_mareas), color = TextoSuperficieVariante)
        }
        return
    }
    val pleamar = stringResource(R.string.condiciones_pleamar)
    val bajamar = stringResource(R.string.condiciones_bajamar)

    TarjetaCondicion(stringResource(R.string.condiciones_estado_actual)) {
        val anterior = estado.anterior
        val siguiente = estado.siguiente
        val progreso = if (anterior != null && siguiente != null) {
            val total = ChronoUnit.MINUTES.between(anterior.hora, siguiente.hora).toFloat()
            ChronoUnit.MINUTES.between(anterior.hora, c.ahora) / total
        } else 0.5f
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(if (estado.subiendo) R.string.condiciones_subiendo else R.string.condiciones_bajando),
                color = TextoSuperficieVariante
            )
            Text("%.2f m".format(estado.alturaM), fontSize = 22.sp, color = TextoSuperficie)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DatoGrande(
                hora(anterior?.hora),
                if (anterior?.esPleamar == true) pleamar else bajamar,
                color = if (anterior?.esPleamar == true) Acento else ColorBajamar
            )
            MedioCirculo(progreso, if (estado.subiendo) Acento else ColorBajamar, ancho = 110.dp)
            DatoGrande(
                hora(siguiente?.hora),
                if (siguiente?.esPleamar == true) pleamar else bajamar,
                color = if (siguiente?.esPleamar == true) Acento else ColorBajamar
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Divisor)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            DatoGrande(
                anterior?.let { duracion(ChronoUnit.MINUTES.between(it.hora, c.ahora)) } ?: "-",
                stringResource(
                    if (anterior?.esPleamar == true) R.string.condiciones_desde_pleamar else R.string.condiciones_desde_bajamar
                )
            )
            DatoGrande(
                siguiente?.let { duracion(ChronoUnit.MINUTES.between(c.ahora, it.hora)) } ?: "-",
                stringResource(
                    if (siguiente?.esPleamar == true) R.string.condiciones_para_pleamar else R.string.condiciones_para_bajamar
                )
            )
        }
        if (anterior != null && siguiente != null) {
            Spacer(Modifier.height(8.dp))
            FilaDato(stringResource(R.string.condiciones_amplitud), "%.2f m".format(abs(siguiente.alturaM - anterior.alturaM)))
        }
    }

    TarjetaCondicion(stringResource(R.string.condiciones_extremos_hoy)) {
        GraficoMarea(
            fecha = c.ahora.toLocalDate(),
            horas = c.horasMar,
            extremos = c.mareas,
            amanecer = c.astroHoy.amanecer,
            atardecer = c.astroHoy.atardecer,
            ahora = c.ahora
        )
        Text(
            stringResource(R.string.condiciones_leyenda_marea),
            style = MaterialTheme.typography.bodySmall,
            color = TextoSuperficieVariante
        )
    }

    TarjetaCondicion(stringResource(R.string.condiciones_proximos_dias)) {
        c.mareas.filter { !it.hora.toLocalDate().isBefore(c.ahora.toLocalDate()) }
            .groupBy { it.hora.toLocalDate() }
            .forEach { (fecha, extremos) ->
                Text(
                    diaCorto(fecha, c.ahora.toLocalDate()),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextoSuperficie,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    extremos.forEach { e ->
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painterResource(if (e.esPleamar) R.drawable.ic_ms_north else R.drawable.ic_ms_south),
                                    contentDescription = if (e.esPleamar) pleamar else bajamar,
                                    tint = if (e.esPleamar) Acento else ColorBajamar,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    hora(e.hora),
                                    color = if (e.esPleamar) Acento else ColorBajamar,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Text("%.2f m".format(e.alturaM), style = MaterialTheme.typography.bodySmall, color = TextoSuperficieVariante)
                        }
                    }
                }
            }
    }

    Text(
        stringResource(R.string.condiciones_aviso_mareas),
        style = MaterialTheme.typography.bodySmall,
        color = ColorRegular,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

// ============================== SOL Y LUNA ==============================

@Composable
private fun PaginaSolLuna(c: Condiciones) {
    val a = c.astroHoy
    TarjetaCondicion(stringResource(R.string.condiciones_sol)) {
        val amanecer = a.amanecer
        val atardecer = a.atardecer
        val esDeDia = amanecer != null && atardecer != null && c.ahora.isAfter(amanecer) && c.ahora.isBefore(atardecer)
        val progreso = if (amanecer != null && atardecer != null) {
            (ChronoUnit.MINUTES.between(amanecer, c.ahora).toFloat() /
                ChronoUnit.MINUTES.between(amanecer, atardecer).toFloat()).coerceIn(0f, 1f)
        } else 0f
        Text(
            stringResource(if (esDeDia) R.string.condiciones_dia else R.string.condiciones_noche),
            color = TextoSuperficieVariante,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DatoGrande(hora(amanecer), stringResource(R.string.condiciones_salida), color = ColorRegular)
            MedioCirculo(progreso, ColorRegular, ancho = 110.dp)
            DatoGrande(hora(atardecer), stringResource(R.string.condiciones_puesta), color = ColorRegular)
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Divisor)
        if (amanecer != null && atardecer != null) {
            if (esDeDia) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    DatoGrande(duracion(ChronoUnit.MINUTES.between(amanecer, c.ahora)), stringResource(R.string.condiciones_desde_salida))
                    DatoGrande(duracion(ChronoUnit.MINUTES.between(c.ahora, atardecer)), stringResource(R.string.condiciones_para_puesta))
                }
                Spacer(Modifier.height(8.dp))
            }
            FilaDato(stringResource(R.string.condiciones_duracion_dia), duracion(ChronoUnit.MINUTES.between(amanecer, atardecer)))
        }
        FilaDato(stringResource(R.string.condiciones_primera_luz), hora(a.inicioCrepusculo))
        FilaDato(stringResource(R.string.condiciones_ultima_luz), hora(a.finCrepusculo))
        FilaDato(stringResource(R.string.condiciones_mediodia), hora(a.mediodiaSolar))
        FilaDato(stringResource(R.string.condiciones_altura_sol), "${a.alturaMaximaSol.roundToInt()}°")
        FilaDato(stringResource(R.string.condiciones_uv_max), num(c.dias.firstOrNull()?.uvMax, 1))
    }

    TarjetaCondicion(stringResource(R.string.detalle_luna)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FaseLunar(iluminacionPorc = a.iluminacionPorc, fase = a.fase, tamanio = 52.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(nombreFaseLunar(a.fase), fontSize = 22.sp, color = TextoSuperficie)
                Text(
                    stringResource(R.string.condiciones_luna_iluminada, a.iluminacionPorc, "%.1f".format(a.edadLunaDias)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuperficieVariante
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        FilaDato(stringResource(R.string.condiciones_salida_luna), hora(a.salidaLuna))
        FilaDato(stringResource(R.string.condiciones_luna_alto), hora(a.transitoLuna))
        FilaDato(stringResource(R.string.condiciones_puesta_luna), hora(a.puestaLuna))
        FilaDato(stringResource(R.string.condiciones_luna_bajo), hora(a.antitransitoLuna))
        FilaDato(stringResource(R.string.condiciones_proxima_llena), diaCorto(a.proximaLunaLlena, c.ahora.toLocalDate()))
        FilaDato(stringResource(R.string.condiciones_proxima_nueva), diaCorto(a.proximaLunaNueva, c.ahora.toLocalDate()))
    }

    TarjetaCondicion(stringResource(R.string.condiciones_fases_semana)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            c.astroDias.forEach { d ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FaseLunar(iluminacionPorc = d.iluminacionPorc, fase = d.fase, tamanio = 22.dp)
                    Text("${d.iluminacionPorc}%", style = MaterialTheme.typography.bodySmall, color = TextoSuperficie, modifier = Modifier.padding(top = 4.dp))
                    Text(
                        diaCorto(d.fecha, c.ahora.toLocalDate()).take(3),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextoSuperficieVariante
                    )
                }
            }
        }
    }
}

/**
 * Dibujo de la fase lunar (reemplaza a los emojis 🌒🌓…): disco oscuro con la parte iluminada.
 * Creciente = iluminada a la derecha (como se ve desde el hemisferio norte y en los íconos
 * habituales); menguante = a la izquierda. TalkBack lee el nombre de la fase.
 */
@Composable
fun FaseLunar(iluminacionPorc: Int, fase: String, modifier: Modifier = Modifier, tamanio: Dp = 24.dp) {
    val fraccion = (iluminacionPorc.coerceIn(0, 100)) / 100f
    val creciente = fase.contains("creciente", ignoreCase = true)
    val descripcionFase = nombreFaseLunar(fase)
    Canvas(modifier.size(tamanio).semantics { contentDescription = descripcionFase }) {
        val r = size.minDimension / 2
        val centro = Offset(size.width / 2, size.height / 2)
        drawCircle(SuperficieTonalAlta, radius = r, center = centro)
        if (fraccion <= 0.01f) return@Canvas
        if (fraccion >= 0.99f) {
            drawCircle(TextoSuperficie, radius = r, center = centro)
            return@Canvas
        }
        // Media luna iluminada (derecha) + elipse del terminador: hacia afuera si es menos de la mitad
        val ancho = r * abs(1 - 2 * fraccion)
        val circulo = Rect(centro.x - r, centro.y - r, centro.x + r, centro.y + r)
        val elipse = Rect(centro.x - ancho, centro.y - r, centro.x + ancho, centro.y + r)
        val iluminada = Path().apply {
            arcTo(circulo, -90f, 180f, forceMoveTo = true)                       // arriba → derecha → abajo
            arcTo(elipse, 90f, if (fraccion < 0.5f) -180f else 180f, forceMoveTo = false)
            close()
        }
        // Menguante: espejo horizontal
        scale(scaleX = if (creciente) 1f else -1f, scaleY = 1f, pivot = centro) {
            drawPath(iluminada, TextoSuperficie)
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
                    Text(diaCorto(d.fecha, hoy), style = MaterialTheme.typography.titleMedium, color = TextoSuperficie)
                    Text(
                        d.codigoClima?.let { descripcionClima(it) } ?: "-",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSuperficieVariante
                    )
                }
                Icon(
                    painterResource(iconoClima(d.codigoClima)),
                    contentDescription = null,
                    tint = colorIconoClima(d.codigoClima),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(num(d.maximaC, unidad = "°"), style = MaterialTheme.typography.titleMedium, color = TextoSuperficie)
                    Text(num(d.minimaC, unidad = "°"), color = TextoSuperficieVariante)
                }
            }
            Spacer(Modifier.height(8.dp))
            FilaDato(stringResource(R.string.condiciones_viento_rafagas), "${num(d.vientoMaxKmh)} / ${num(d.rafagasMaxKmh)} km/h ${direccion(d.direccionVientoGrados)}")
            FilaDato(stringResource(R.string.condiciones_lluvia), "${num(d.lluviaMm, 1, " mm")} (${num(d.probabilidadLluviaPorc?.toDouble(), unidad = "%")})")
            FilaDato(stringResource(R.string.condiciones_ola_max), num(d.olaMaxM, 1, " m"))
            FilaDato(stringResource(R.string.condiciones_uv_max), num(d.uvMax, 1))
            if (astro != null) {
                FilaDato(stringResource(R.string.condiciones_sol), "${hora(astro.amanecer)} - ${hora(astro.atardecer)}")
                FilaDato(stringResource(R.string.detalle_luna), "${nombreFaseLunar(astro.fase)} (${astro.iluminacionPorc}%)")
            }
            if (mareasDia.isNotEmpty()) {
                FilaDato(stringResource(R.string.condiciones_tab_mareas), mareasDia.joinToString("  ") { "${if (it.esPleamar) "▲" else "▼"}${hora(it.hora)}" })
            }
        }
    }
}
