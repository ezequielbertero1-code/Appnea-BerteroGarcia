package com.berterogarcia.appnea.ui.estadisticas

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.dominio.modelo.TipoInmersion
import com.berterogarcia.appnea.ui.components.BarraHorizontal
import com.berterogarcia.appnea.ui.components.BarraSuperior
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.ChipsSeleccionUnica
import com.berterogarcia.appnea.ui.components.FilaMosaicos
import com.berterogarcia.appnea.ui.components.GraficoBarras
import com.berterogarcia.appnea.ui.components.GraficoLinea
import com.berterogarcia.appnea.ui.components.IconoCirculo
import com.berterogarcia.appnea.ui.components.Mosaico
import com.berterogarcia.appnea.ui.components.NavegacionBarra
import com.berterogarcia.appnea.ui.components.nombre
import com.berterogarcia.appnea.ui.components.rememberProgresoDibujo
import com.berterogarcia.appnea.ui.condiciones.TarjetaCondicion
import com.berterogarcia.appnea.ui.entrenamiento.minSeg
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.ContornoFuerte
import com.berterogarcia.appnea.ui.theme.Divisor
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val LOCALE_ES: Locale = Locale.forLanguageTag("es-AR")
private val FORMATO_DIA_MES: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", LOCALE_ES)
private val FORMATO_MES: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", LOCALE_ES)

private const val PESTANIA_INMERSIONES = 0
const val PESTANIA_ENTRENAMIENTO = 1

/**
 * Estadísticas de inmersiones y de entrenamiento. Se abre desde Cuenta, Inmersiones y Entrenar.
 * [pestaniaInicial]: 0 = Inmersiones, 1 = Entrenamiento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadisticasScreen(
    viewModel: EstadisticasViewModel,
    onVolver: () -> Unit,
    onVerInmersion: (Long) -> Unit,
    onRegistrarInmersion: () -> Unit,
    onIrAEntrenar: () -> Unit,
    modifier: Modifier = Modifier,
    pestaniaInicial: Int = PESTANIA_INMERSIONES
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val titulos = listOf(stringResource(R.string.estadisticas_tab_inmersiones), stringResource(R.string.estadisticas_tab_entrenamiento))
    val pagerState = rememberPagerState(initialPage = pestaniaInicial.coerceIn(0, 1)) { titulos.size }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        BarraSuperior(
            titulo = stringResource(R.string.accion_estadisticas),
            navegacion = NavegacionBarra.VOLVER,
            onNavegacion = onVolver
        )
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            contentColor = Acento,
            divider = { HorizontalDivider(color = Divisor) }
        ) {
            titulos.forEachIndexed { index, titulo ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(titulo) },
                    selectedContentColor = Acento,
                    unselectedContentColor = TextoSuperficieVariante
                )
            }
        }

        when {
            estado.cargando -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Acento)
            }
            estado.error -> SinDatos(
                icono = R.drawable.ic_ms_error,
                titulo = stringResource(R.string.estadisticas_error_titulo),
                detalle = stringResource(R.string.historial_error_detalle)
            )
            else -> HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    when (page) {
                        PESTANIA_INMERSIONES -> PestaniaInmersiones(estado, viewModel, onVerInmersion, onRegistrarInmersion)
                        else -> PestaniaEntrenamiento(estado, viewModel, onIrAEntrenar)
                    }
                }
            }
        }
    }
}

// ================================ Inmersiones ================================

@Composable
private fun PestaniaInmersiones(
    estado: EstadisticasUiState,
    viewModel: EstadisticasViewModel,
    onVerInmersion: (Long) -> Unit,
    onRegistrarInmersion: () -> Unit
) {
    if (estado.totalInmersiones == 0) {
        SinDatos(
            icono = R.drawable.ic_ms_bar_chart,
            titulo = stringResource(R.string.estadisticas_sin_datos_titulo),
            detalle = stringResource(R.string.estadisticas_sin_datos_detalle),
            accion = stringResource(R.string.estadisticas_registrar),
            onAccion = onRegistrarInmersion
        )
        return
    }

    val todas = stringResource(R.string.historial_filtro_todas)
    val tipos = TipoInmersion.entries.associateWith { it.nombre() }
    ChipsSeleccionUnica(
        opciones = listOf(null) + TipoInmersion.entries,
        seleccionada = estado.tipo,
        etiqueta = { t -> t?.let { tipos.getValue(it) } ?: todas },
        onSeleccion = viewModel::onTipoChange
    )
    FiltroPeriodo(estado.periodo, viewModel::onPeriodoChange, Modifier.padding(top = 4.dp, bottom = 12.dp))

    val r = estado.inmersiones
    if (r.cantidad == 0) {
        SinResultados(stringResource(R.string.estadisticas_sin_inmersiones_periodo)) { viewModel.onPeriodoChange(PeriodoEstadisticas.TODO) }
        return
    }

    FilaMosaicos {
        Mosaico(stringResource(R.string.cuenta_kpi_inmersiones), r.cantidad.toString())
        Mosaico(
            stringResource(R.string.estadisticas_profundidad_max),
            r.record?.let { "${numero(it.profundidadMetros)} m" } ?: "-",
            detalle = r.record?.let { "${it.lugar} · ${it.fecha.format(FORMATO_DIA_MES)}" }.orEmpty(),
            colorValor = Acento,
            onClick = r.record?.let { rec -> { onVerInmersion(rec.id) } }
        )
    }
    FilaMosaicos {
        Mosaico(stringResource(R.string.estadisticas_tiempo_bajo_agua), duracionLarga(r.tiempoTotal))
        Mosaico(stringResource(R.string.estadisticas_profundidad_promedio), "${numero(r.profundidadPromedio)} m")
    }
    if (r.inmersionesApnea > 0) {
        FilaMosaicos {
            Mosaico(stringResource(R.string.estadisticas_bajadas), r.bajadasTotales.toString())
            Mosaico(
                stringResource(R.string.estadisticas_sincopes), r.sincopes.toString(),
                colorValor = if (r.sincopes > 0) ColorError else TextoSuperficie
            )
        }
    }

    val progresoLinea = rememberProgresoDibujo(r.serie)
    TarjetaCondicion(stringResource(R.string.estadisticas_grafico_profundidad), modifier = Modifier.padding(top = 4.dp)) {
        Subtitulo(stringResource(R.string.estadisticas_grafico_profundidad_sub))
        GraficoLinea(
            valores = r.serie.map { it.profundidadMetros },
            etiquetasX = r.serie.map { it.fecha.format(FORMATO_DIA_MES) },
            formato = { "${numero(it)} m" },
            progreso = progresoLinea,
            onPuntoClick = { i -> r.serie.getOrNull(i)?.let { onVerInmersion(it.id) } }
        )
    }

    val progresoBarras = rememberProgresoDibujo(r.porMes)
    TarjetaCondicion(stringResource(R.string.estadisticas_por_mes)) {
        Subtitulo(stringResource(R.string.estadisticas_ultimos_meses, estado.periodo.mesesGrafico))
        GraficoBarras(
            valores = r.porMes.map { it.second.toFloat() },
            etiquetas = r.porMes.map { it.first.format(FORMATO_MES).removeSuffix(".") },
            formato = { it.toInt().toString() },
            progreso = progresoBarras
        )
    }

    if (r.lugares.isNotEmpty()) {
        TarjetaCondicion(stringResource(R.string.estadisticas_lugares)) {
            val maximo = r.lugares.first().cantidad.coerceAtLeast(1)
            r.lugares.forEach { lugar ->
                BarraHorizontal(
                    etiqueta = lugar.lugar,
                    valor = lugar.cantidad.toString(),
                    fraccion = lugar.cantidad.toFloat() / maximo
                )
            }
        }
    }
}

// ================================ Entrenamiento ================================

@Composable
private fun PestaniaEntrenamiento(
    estado: EstadisticasUiState,
    viewModel: EstadisticasViewModel,
    onIrAEntrenar: () -> Unit
) {
    if (estado.totalSesiones == 0) {
        SinDatos(
            icono = R.drawable.ic_ms_timer,
            titulo = stringResource(R.string.estadisticas_sin_entrenamientos_titulo),
            detalle = stringResource(R.string.estadisticas_sin_entrenamientos_detalle),
            accion = stringResource(R.string.estadisticas_ir_a_entrenar),
            onAccion = onIrAEntrenar
        )
        return
    }

    FiltroPeriodo(estado.periodo, viewModel::onPeriodoChange, Modifier.padding(bottom = 12.dp))

    val r = estado.entrenamiento
    if (r.vacio) {
        SinResultados(stringResource(R.string.estadisticas_sin_entrenamientos_periodo)) { viewModel.onPeriodoChange(PeriodoEstadisticas.TODO) }
        return
    }

    if (r.sesiones > 0) {
        FilaMosaicos {
            Mosaico(stringResource(R.string.estadisticas_sesiones), r.sesiones.toString(), detalle = stringResource(R.string.estadisticas_completas, r.completas))
            Mosaico(stringResource(R.string.estadisticas_completadas), "${r.porcentajeCompletas} %")
            Mosaico(stringResource(R.string.estadisticas_apnea_mas_larga), minSeg(r.apneaMasLargaSegundos), colorValor = Acento)
        }
    }
    FilaMosaicos {
        Mosaico(stringResource(R.string.estadisticas_tiempo_entrenado), tiempoEntrenado(r.tiempoEntrenadoSegundos))
        if (r.intentosCronometro > 0) {
            Mosaico(stringResource(R.string.estadisticas_intentos_libres), r.intentosCronometro.toString())
            Mosaico(stringResource(R.string.estadisticas_mejor_intento), minSeg(r.mejorIntentoSegundos), colorValor = Acento)
        }
    }

    if (r.serie.isNotEmpty()) {
        val progreso = rememberProgresoDibujo(r.serie)
        TarjetaCondicion(stringResource(R.string.estadisticas_grafico_apnea), modifier = Modifier.padding(top = 4.dp)) {
            Subtitulo(stringResource(R.string.estadisticas_minutos_segundos))
            GraficoLinea(
                valores = r.serie.map { it.marcaSegundos.toFloat() },
                etiquetasX = r.serie.map { it.fecha.format(FORMATO_DIA_MES) },
                formato = { minSeg(it.toInt()) },
                progreso = progreso
            )
            AvisoSinApnea(r.sinApnea)
        }
    } else if (r.sinApnea > 0) {
        AvisoSinApnea(r.sinApnea)
    }

    if (r.serieCronometro.isNotEmpty()) {
        val progreso = rememberProgresoDibujo(r.serieCronometro)
        TarjetaCondicion(stringResource(R.string.estadisticas_grafico_cronometro), modifier = Modifier.padding(top = 4.dp)) {
            Subtitulo(stringResource(R.string.estadisticas_minutos_segundos))
            GraficoLinea(
                valores = r.serieCronometro.map { it.second.toFloat() },
                etiquetasX = r.serieCronometro.map { it.first.format(FORMATO_DIA_MES) },
                formato = { minSeg(it.toInt()) },
                progreso = progreso
            )
        }
    }

    if (r.sesiones == 0) return

    TarjetaCondicion(stringResource(R.string.estadisticas_factores)) {
        if (r.factores.isEmpty()) {
            Text(
                stringResource(R.string.estadisticas_factores_sin_datos, MIN_SESIONES_FACTOR),
                style = MaterialTheme.typography.bodySmall,
                color = TextoSuperficieVariante
            )
        } else {
            Subtitulo(stringResource(R.string.estadisticas_factores_sub, r.porcentajeCompletas))
            val promedio = r.porcentajeCompletas / 100f
            r.factores.forEach { f ->
                BarraHorizontal(
                    etiqueta = stringResource(f.factor.etiqueta),
                    valor = stringResource(R.string.estadisticas_factor_valor, f.porcentajeCompletas, f.sesiones),
                    fraccion = f.porcentajeCompletas / 100f,
                    marca = promedio,
                    // Por debajo del promedio: gris, para que resalten los que ayudan
                    color = if (f.porcentajeCompletas >= r.porcentajeCompletas) Acento else ContornoFuerte
                )
            }
            r.factorDestacado?.let { f ->
                Text(
                    stringResource(R.string.estadisticas_factor_destacado, stringResource(f.factor.etiqueta), f.porcentajeCompletas, r.porcentajeCompletas),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuperficie,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

/** Las sesiones cortadas en la preparación no tienen marca: se avisa para que no parezca que faltan. */
@Composable
private fun AvisoSinApnea(cantidad: Int) {
    if (cantidad == 0) return
    Text(
        pluralStringResource(R.plurals.estadisticas_sin_apnea, cantidad, cantidad),
        style = MaterialTheme.typography.bodySmall,
        color = TextoSuperficieVariante,
        modifier = Modifier.padding(top = 8.dp)
    )
}

/** 45 → "45 s"; 750 → "12 min"; 4000 → "1 h 6 min" */
private fun tiempoEntrenado(segundos: Int): String =
    if (segundos < 60) "$segundos s" else duracionLarga(segundos.toLong().seconds)

// ================================ Piezas comunes ================================

@Composable
private fun FiltroPeriodo(
    seleccionado: PeriodoEstadisticas,
    onSeleccion: (PeriodoEstadisticas) -> Unit,
    modifier: Modifier = Modifier
) {
    val etiquetas = PeriodoEstadisticas.entries.associateWith { stringResource(it.etiqueta) }
    ChipsSeleccionUnica(
        opciones = PeriodoEstadisticas.entries,
        seleccionada = seleccionado,
        etiqueta = { etiquetas.getValue(it) },
        onSeleccion = onSeleccion,
        modifier = modifier
    )
}

@Composable
private fun Subtitulo(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.bodySmall,
        color = TextoSuperficieVariante,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

/** Pantalla vacía: no hay nada cargado todavía. */
@Composable
private fun SinDatos(
    @DrawableRes icono: Int,
    titulo: String,
    detalle: String,
    accion: String? = null,
    onAccion: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconoCirculo(painterResource(icono), modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium, color = TextoSuperficie, textAlign = TextAlign.Center)
        Text(
            detalle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSuperficieVariante,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (accion != null) {
            BotonPrincipal(texto = accion, onClick = onAccion, modifier = Modifier.padding(top = 16.dp))
        }
    }
}

/** Hay datos, pero no en el período elegido. */
@Composable
private fun SinResultados(texto: String, onVerTodo: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = TextoSuperficieVariante, textAlign = TextAlign.Center)
        TextButton(onClick = onVerTodo) { Text(stringResource(R.string.estadisticas_ver_todo), color = Acento) }
    }
}

// ================================ Formatos ================================

/** 18.0 → "18", 16.43 → "16,4" */
private fun numero(v: Float): String =
    if (v % 1f == 0f) v.toInt().toString() else String.format(LOCALE_ES, "%.1f", v)

/** 400 min → "6 h 40 min"; 45 min → "45 min" */
private fun duracionLarga(d: Duration): String {
    val minutos = d.inWholeMinutes
    return when {
        minutos < 60 -> "$minutos min"
        minutos % 60 == 0L -> "${minutos / 60} h"
        else -> "${minutos / 60} h ${minutos % 60} min"
    }
}
