package com.berterogarcia.appnea.ui.entrenamiento

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.components.BotonPeligro
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.BotonSecundario
import com.berterogarcia.appnea.ui.components.MantenerPantallaEncendida
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.FondoTarjeta
import com.berterogarcia.appnea.ui.theme.TextoSecundario
import com.berterogarcia.appnea.ui.theme.TextoTerciario
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

private val HH_MM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Pestaña Entrenar.
 * - Elegir tabla / Libre: con pestañas.
 * - En curso y Resultado: a pantalla completa (sin pestañas ni barra), para no salir sin querer.
 *
 * [onVerEstadisticas] y [onGuardarIntentos] quedan preparados para la pantalla de
 * Estadísticas: mientras sean null no se muestran.
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
    modifier: Modifier = Modifier
) {
    val titulos = listOf("Tablas", "Libre")
    val pagerState = rememberPagerState(initialPage = 0) { titulos.size }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            "Entrenar",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
        )
        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent
        ) {
            titulos.forEachIndexed { index, titulo ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(titulo) },
                    icon = {
                        Icon(
                            painter = painterResource(
                                if (index == 0) R.drawable.ic_entrenamiento else R.drawable.ic_cronometro
                            ),
                            contentDescription = titulo
                        )
                    }
                )
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> SeleccionTablaScreen(
                    estado = estado,
                    viewModel = entrenamientoViewModel,
                    onCrearTabla = onCrearTabla,
                    modifier = Modifier.fillMaxSize()
                )
                1 -> CronometroLibre(
                    viewModel = cronometroViewModel,
                    onGuardarIntentos = onGuardarIntentos,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// ================================ CRONÓMETRO LIBRE ================================

/** Apnea estática libre. El aviso de seguridad se muestra SIEMPRE (acá no hay tilde). */
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
                Text("Apnea estática", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                Row {
                    Text(
                        minSeg((estado.transcurridoMs / 1000L).toInt()),
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.alignByBaseline()
                    )
                    Text(
                        ".${(estado.transcurridoMs % 1000L) / 100L}",
                        fontSize = 22.sp,
                        color = TextoSecundario,
                        modifier = Modifier.alignByBaseline()
                    )
                }
                if (mejor != null) {
                    Text(
                        "mejor de hoy: ${minSeg((mejor / 1000L).toInt())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                }
            }
        }
        Text(
            "El anillo da una vuelta por minuto",
            style = MaterialTheme.typography.labelSmall,
            color = TextoTerciario,
            modifier = Modifier.padding(top = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BotonSecundario(
                texto = "Reiniciar",
                onClick = viewModel::onReiniciar,
                enabled = estado.corriendo || estado.transcurridoMs > 0L,
                modifier = Modifier.weight(1f)
            )
            if (estado.corriendo) {
                BotonPeligro(texto = "Parar", onClick = viewModel::onParar, modifier = Modifier.weight(1f))
            } else {
                BotonPrincipal(texto = "Iniciar", onClick = viewModel::onIniciar, modifier = Modifier.weight(1f))
            }
        }

        if (estado.intentos.isNotEmpty() && mejor != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Intentos de hoy",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                // Preparado para Estadísticas: aparece cuando se conecte onGuardarIntentos
                if (onGuardarIntentos != null) {
                    TextButton(onClick = { onGuardarIntentos(estado.intentos) }) { Text("Guardar", color = Acento) }
                }
            }
            estado.intentos.forEach { intento ->
                FilaIntento(intento = intento, mejorMs = mejor)
            }
        }
    }
}

@Composable
private fun FilaIntento(intento: IntentoApnea, mejorMs: Long) {
    val esMejor = intento.duracionMs == mejorMs
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FondoTarjeta)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "#${intento.numero} · ${intento.hora.format(HH_MM)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextoSecundario
            )
            Text(
                "${minSeg((intento.duracionMs / 1000L).toInt())}.${(intento.duracionMs % 1000L) / 100L}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (esMejor) {
            Text("★ Mejor", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Acento)
        } else {
            Text(
                "−${minSeg(((mejorMs - intento.duracionMs) / 1000L).toInt())}",
                style = MaterialTheme.typography.labelMedium,
                color = TextoSecundario
            )
        }
    }
}
