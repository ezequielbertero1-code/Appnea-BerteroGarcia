package com.berterogarcia.appnea.ui.condiciones

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.clima.ClimaViewModel
import com.berterogarcia.appnea.ui.clima.SelectorUbicacion
import com.berterogarcia.appnea.ui.components.BarraSuperior
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.NavegacionBarra
import com.berterogarcia.appnea.ui.components.resolver
import com.berterogarcia.appnea.ui.components.texto
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.Divisor
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante
import kotlinx.coroutines.launch

internal val PESTANIAS = listOf(
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
