package com.berterogarcia.appnea.ui.estadisticas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.TipoInmersion
import com.berterogarcia.appnea.ui.components.BarraHorizontal
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.ChipsSeleccionUnica
import com.berterogarcia.appnea.ui.components.FilaMosaicos
import com.berterogarcia.appnea.ui.components.GraficoBarras
import com.berterogarcia.appnea.ui.components.GraficoLinea
import com.berterogarcia.appnea.ui.components.Mosaico
import com.berterogarcia.appnea.ui.components.rememberProgresoDibujo
import com.berterogarcia.appnea.ui.condiciones.TarjetaCondicion
import com.berterogarcia.appnea.ui.entrenamiento.minSeg
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.BordeSuave
import com.berterogarcia.appnea.ui.theme.TextoSecundario
import com.berterogarcia.appnea.ui.theme.TextoTerciario
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration

private val LOCALE_ES: Locale = Locale.forLanguageTag("es-AR")
private val FORMATO_DIA_MES: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", LOCALE_ES)
private val FORMATO_MES: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", LOCALE_ES)

private const val PESTANIA_INMERSIONES = 0
const val PESTANIA_ENTRENAMIENTO = 1

/**
 * Estadísticas de inmersiones y de entrenamiento. Se abre desde Cuenta, Inmersiones y Entrenar.
 * [pestaniaInicial]: 0 = Inmersiones, 1 = Entrenamiento.
 */
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
    val titulos = listOf("Inmersiones", "Entrenamiento")
    val pagerState = rememberPagerState(initialPage = pestaniaInicial.coerceIn(0, 1)) { titulos.size }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text("Estadísticas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Normal)
        }
        SecondaryTabRow(selectedTabIndex = pagerState.currentPage, containerColor = Color.Transparent) {
            titulos.forEachIndexed { index, titulo ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(titulo) },
                    icon = {
                        Icon(
                            painterResource(if (index == PESTANIA_INMERSIONES) R.drawable.ic_apnea else R.drawable.ic_cronometro),
                            contentDescription = titulo
                        )
                    }
                )
            }
        }

        when {
            estado.cargando -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Acento)
            }
            estado.error -> SinDatos(
                emoji = "⚠️",
                titulo = "No se pudieron cargar las estadísticas",
                detalle = "Probá cerrar y volver a abrir la app."
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
            emoji = "📊",
            titulo = "Todavía no hay datos",
            detalle = "Registrá tu primera inmersión y acá vas a ver tu progreso.",
            accion = "+ Registrar inmersión",
            onAccion = onRegistrarInmersion
        )
        return
    }

    FiltroPeriodo(estado.periodo, viewModel::onPeriodoChange)
    ChipsSeleccionUnica(
        opciones = listOf(null) + TipoInmersion.entries,
        seleccionada = estado.tipo,
        etiqueta = { t -> t?.let { "${it.emoji} ${it.etiqueta}" } ?: "Todas" },
        onSeleccion = viewModel::onTipoChange,
        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
    )

    val r = estado.inmersiones
    if (r.cantidad == 0) {
        SinResultados("No hay inmersiones en este período.") { viewModel.onPeriodoChange(PeriodoEstadisticas.TODO) }
        return
    }

    FilaMosaicos {
        Mosaico("Inmersiones", r.cantidad.toString())
        Mosaico(
            "Profundidad máx.",
            r.record?.let { "${numero(it.profundidadMetros)} m" } ?: "-",
            detalle = r.record?.let { "${it.lugar} · ${it.fecha.format(FORMATO_DIA_MES)}" }.orEmpty(),
            colorValor = Acento,
            onClick = r.record?.let { rec -> { onVerInmersion(rec.id) } }
        )
    }
    FilaMosaicos {
        Mosaico("Tiempo bajo el agua", duracionLarga(r.tiempoTotal))
        Mosaico("Profundidad promedio", "${numero(r.profundidadPromedio)} m")
    }

    val progresoLinea = rememberProgresoDibujo(r.serie)
    TarjetaCondicion("Profundidad máxima por inmersión", modifier = Modifier.padding(top = 4.dp)) {
        Subtitulo("metros · tocá un punto para ver la inmersión")
        GraficoLinea(
            valores = r.serie.map { it.profundidadMetros },
            etiquetasX = r.serie.map { it.fecha.format(FORMATO_DIA_MES) },
            formato = { "${numero(it)} m" },
            progreso = progresoLinea,
            onPuntoClick = { i -> r.serie.getOrNull(i)?.let { onVerInmersion(it.id) } }
        )
    }

    val progresoBarras = rememberProgresoDibujo(r.porMes)
    TarjetaCondicion("Inmersiones por mes") {
        Subtitulo("últimos ${estado.periodo.mesesGrafico} meses")
        GraficoBarras(
            valores = r.porMes.map { it.second.toFloat() },
            etiquetas = r.porMes.map { it.first.format(FORMATO_MES).removeSuffix(".") },
            formato = { it.toInt().toString() },
            progreso = progresoBarras
        )
    }

    if (r.lugares.isNotEmpty()) {
        TarjetaCondicion("Lugares más visitados") {
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
            emoji = "⏱️",
            titulo = "Todavía no hay entrenamientos",
            detalle = "Completá una tabla en Entrenar y guardala para ver tu progreso.",
            accion = "Ir a Entrenar",
            onAccion = onIrAEntrenar
        )
        return
    }

    FiltroPeriodo(estado.periodo, viewModel::onPeriodoChange, Modifier.padding(bottom = 12.dp))

    val r = estado.entrenamiento
    if (r.sesiones == 0) {
        SinResultados("No hay entrenamientos en este período.") { viewModel.onPeriodoChange(PeriodoEstadisticas.TODO) }
        return
    }

    FilaMosaicos {
        Mosaico("Sesiones", r.sesiones.toString(), detalle = "${r.completas} completas")
        Mosaico("Completadas", "${r.porcentajeCompletas} %")
        Mosaico("Apnea más larga", minSeg(r.apneaMasLargaSegundos), colorValor = Acento)
    }

    if (r.serie.isNotEmpty()) {
        val progreso = rememberProgresoDibujo(r.serie)
        TarjetaCondicion("Apnea más larga por sesión", modifier = Modifier.padding(top = 4.dp)) {
            Subtitulo("minutos:segundos")
            GraficoLinea(
                valores = r.serie.map { it.marcaSegundos.toFloat() },
                etiquetasX = r.serie.map { it.fecha.format(FORMATO_DIA_MES) },
                formato = { minSeg(it.toInt()) },
                progreso = progreso
            )
        }
    }

    TarjetaCondicion("Qué influye en completar la tabla") {
        if (r.factores.isEmpty()) {
            Text(
                "Todavía no hay suficientes datos. Marcá los factores al terminar cada tabla: " +
                    "un factor aparece cuando lo marcaste en $MIN_SESIONES_FACTOR sesiones o más.",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )
        } else {
            Subtitulo("% de tablas completas cuando marcaste el factor · la rayita es tu promedio (${r.porcentajeCompletas} %)")
            val promedio = r.porcentajeCompletas / 100f
            r.factores.forEach { f ->
                BarraHorizontal(
                    etiqueta = f.factor,
                    valor = "${f.porcentajeCompletas} % · ${f.sesiones} ses.",
                    fraccion = f.porcentajeCompletas / 100f,
                    marca = promedio,
                    // Por debajo del promedio: gris, para que resalten los que ayudan
                    color = if (f.porcentajeCompletas >= r.porcentajeCompletas) Acento else BordeSuave
                )
            }
            r.factorDestacado?.let { f ->
                Text(
                    "Con «${f.factor}» completaste el ${f.porcentajeCompletas} % de las tablas " +
                        "(tu promedio es ${r.porcentajeCompletas} %).",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

// ================================ Piezas comunes ================================

@Composable
private fun FiltroPeriodo(
    seleccionado: PeriodoEstadisticas,
    onSeleccion: (PeriodoEstadisticas) -> Unit,
    modifier: Modifier = Modifier
) {
    ChipsSeleccionUnica(
        opciones = PeriodoEstadisticas.entries,
        seleccionada = seleccionado,
        etiqueta = { it.etiqueta },
        onSeleccion = onSeleccion,
        modifier = modifier
    )
}

@Composable
private fun Subtitulo(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.bodySmall,
        color = TextoTerciario,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

/** Pantalla vacía: no hay nada cargado todavía. */
@Composable
private fun SinDatos(
    emoji: String,
    titulo: String,
    detalle: String,
    accion: String? = null,
    onAccion: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 44.sp)
        Spacer(Modifier.height(8.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Normal, textAlign = TextAlign.Center)
        Text(
            detalle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario,
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
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario, textAlign = TextAlign.Center)
        TextButton(onClick = onVerTodo) { Text("Ver todo", color = Acento) }
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
