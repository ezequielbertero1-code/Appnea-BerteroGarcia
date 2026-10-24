package com.berterogarcia.appnea.ui.entrenamiento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.components.BarraSuperior
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.BotonSecundario
import com.berterogarcia.appnea.ui.components.EncabezadoGrupo
import com.berterogarcia.appnea.ui.components.FilaLista
import com.berterogarcia.appnea.ui.components.GrupoLista
import com.berterogarcia.appnea.ui.components.Insignia
import com.berterogarcia.appnea.ui.components.MantenerPantallaEncendida
import com.berterogarcia.appnea.ui.components.SelectorSegmentado
import com.berterogarcia.appnea.ui.components.TipoInsignia
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

private val HH_MM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Modos de la pestaña Entrenar (botones segmentados del encabezado). */
private enum class ModoEntrenar { TABLAS, LIBRE }

/**
 * Pestaña Entrenar.
 * - Elegir tabla / Libre: barra superior + botones segmentados (y se puede deslizar entre los dos).
 * - En curso y Resultado: a pantalla completa (sin barra de navegación), para no salir sin querer.
 *
 * [onVerEstadisticas] y [onGuardarIntentos] son opcionales: mientras sean null no se muestran.
 */
@Composable
fun CronometroScreen(
    entrenamientoViewModel: EntrenamientoApneaViewModel,
    onCrearTabla: () -> Unit,
    modifier: Modifier = Modifier,
    cronometroViewModel: CronometroViewModel = viewModel(),
    onVerEstadisticas: (() -> Unit)? = null,
    onGuardarIntentos: ((List<IntentoApnea>) -> Unit)? = null
) {
    val estado by entrenamientoViewModel.uiState.collectAsStateWithLifecycle()

    when (estado.etapa) {
        EtapaEntrenamiento.EN_CURSO -> EntrenamientoEnCurso(estado, entrenamientoViewModel, modifier)
        EtapaEntrenamiento.GUARDANDO -> ResultadoEntrenamiento(
            estado, entrenamientoViewModel, modifier, onVerEstadisticas = onVerEstadisticas
        )
        EtapaEntrenamiento.SELECCION -> PestaniasEntrenar(
            estado = estado,
            entrenamientoViewModel = entrenamientoViewModel,
            cronometroViewModel = cronometroViewModel,
            onCrearTabla = onCrearTabla,
            onGuardarIntentos = onGuardarIntentos,
            onVerEstadisticas = onVerEstadisticas,
            modifier = modifier
        )
    }
}

@Composable
private fun PestaniasEntrenar(
    estado: EntrenamientoUiState,
    entrenamientoViewModel: EntrenamientoApneaViewModel,
    cronometroViewModel: CronometroViewModel,
    onCrearTabla: () -> Unit,
    onGuardarIntentos: ((List<IntentoApnea>) -> Unit)?,
    onVerEstadisticas: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val modos = ModoEntrenar.entries
    val pagerState = rememberPagerState(initialPage = 0) { modos.size }
    val scope = rememberCoroutineScope()
    val etiquetas = mapOf(
        ModoEntrenar.TABLAS to stringResource(R.string.entrenar_modo_tablas),
        ModoEntrenar.LIBRE to stringResource(R.string.entrenar_modo_libre)
    )

    Column(modifier = modifier.fillMaxSize()) {
        BarraSuperior(
            titulo = stringResource(R.string.titulo_entrenar),
            acciones = {
                if (onVerEstadisticas != null) {
                    IconButton(onClick = onVerEstadisticas) {
                        Icon(
                            painterResource(R.drawable.ic_ms_bar_chart),
                            contentDescription = stringResource(R.string.accion_estadisticas)
                        )
                    }
                }
            }
        )
        SelectorSegmentado(
            opciones = modos,
            seleccionada = modos[pagerState.currentPage],
            etiqueta = { etiquetas.getValue(it) },
            onSeleccion = { modo -> modo?.let { scope.launch { pagerState.animateScrollToPage(it.ordinal) } } },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (modos[page]) {
                ModoEntrenar.TABLAS -> SeleccionTablaScreen(
                    estado = estado,
                    viewModel = entrenamientoViewModel,
                    onCrearTabla = onCrearTabla,
                    modifier = Modifier.fillMaxSize()
                )
                ModoEntrenar.LIBRE -> CronometroLibre(
                    viewModel = cronometroViewModel,
                    onGuardarIntentos = onGuardarIntentos,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// ================================ CRONÓMETRO LIBRE ================================

/** Apnea estática libre. El aviso de seguridad se muestra SIEMPRE (acá no hay "acompañado"). */
@Composable
private fun CronometroLibre(
    viewModel: CronometroViewModel,
    onGuardarIntentos: ((List<IntentoApnea>) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    if (estado.corriendo) MantenerPantallaEncendida()

    val mejor = estado.mejorMs

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AvisoSeguridadApnea()

        // El anillo da una vuelta por minuto
        AnilloProgreso(
            progreso = (estado.transcurridoMs % 60_000L) / 60_000f,
            color = Acento,
            grosor = 10.dp,
            modifier = Modifier.size(200.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.entrenar_apnea_estatica).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 0.6.sp,
                    color = TextoSuperficieVariante
                )
                Row {
                    Text(
                        minSeg((estado.transcurridoMs / 1000L).toInt()),
                        fontSize = 46.sp,
                        color = TextoSuperficie,
                        modifier = Modifier.alignByBaseline()
                    )
                    Text(
                        ".${(estado.transcurridoMs % 1000L) / 100L}",
                        fontSize = 26.sp,
                        color = TextoSuperficieVariante,
                        modifier = Modifier.alignByBaseline()
                    )
                }
                if (mejor != null) {
                    Text(
                        stringResource(R.string.entrenar_mejor_de_hoy, minSeg((mejor / 1000L).toInt())),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSuperficieVariante
                    )
                }
            }
        }
        Text(
            stringResource(R.string.entrenar_anillo_minuto),
            style = MaterialTheme.typography.labelSmall,
            color = TextoSuperficieVariante,
            modifier = Modifier.padding(top = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BotonSecundario(
                texto = stringResource(R.string.entrenar_reiniciar),
                onClick = viewModel::onReiniciar,
                enabled = estado.corriendo || estado.transcurridoMs > 0L,
                icono = painterResource(R.drawable.ic_ms_restart_alt),
                modifier = Modifier.weight(1f)
            )
            if (estado.corriendo) {
                BotonPrincipal(
                    texto = stringResource(R.string.entrenar_parar),
                    onClick = viewModel::onParar,
                    icono = painterResource(R.drawable.ic_ms_stop),
                    modifier = Modifier.weight(1f)
                )
            } else {
                BotonPrincipal(
                    texto = stringResource(R.string.entrenar_iniciar_libre),
                    onClick = viewModel::onIniciar,
                    icono = rememberVectorPainter(Icons.Rounded.PlayArrow),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (estado.intentos.isNotEmpty() && mejor != null) {
            EncabezadoGrupo(
                titulo = stringResource(R.string.entrenar_intentos_hoy),
                // Aparece cuando se conecte onGuardarIntentos
                accion = if (onGuardarIntentos != null) stringResource(R.string.accion_guardar) else null,
                onAccion = { onGuardarIntentos?.invoke(estado.intentos) }
            )
            GrupoLista {
                estado.intentos.forEachIndexed { i, intento ->
                    FilaIntento(intento = intento, mejorMs = mejor, indice = i, total = estado.intentos.size)
                }
            }
        }
    }
}

@Composable
private fun FilaIntento(intento: IntentoApnea, mejorMs: Long, indice: Int, total: Int) {
    val esMejor = intento.duracionMs == mejorMs
    val tiempo = "${minSeg((intento.duracionMs / 1000L).toInt())}.${(intento.duracionMs % 1000L) / 100L}"
    FilaLista(
        titulo = "#${intento.numero} · ${intento.hora.format(HH_MM)}",
        indice = indice,
        total = total,
        fin = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (esMejor) {
                    Insignia(
                        texto = stringResource(R.string.entrenar_mejor),
                        tipo = TipoInsignia.ADVERTENCIA,
                        icono = rememberVectorPainter(Icons.Rounded.Star)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(tiempo, style = MaterialTheme.typography.titleMedium, color = TextoSuperficie)
                    if (!esMejor) {
                        Text(
                            "−${minSeg(((mejorMs - intento.duracionMs) / 1000L).toInt())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoSuperficieVariante
                        )
                    }
                }
            }
        }
    )
}
