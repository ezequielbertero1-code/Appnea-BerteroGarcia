package com.berterogarcia.appnea.ui.inmersion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.historial.HistorialScreen
import com.berterogarcia.appnea.ui.historial.HistorialViewModel
import com.berterogarcia.appnea.ui.pesca.PescaSubmarinaScreen
import com.berterogarcia.appnea.ui.pesca.PescaSubmarinaViewModel
import kotlinx.coroutines.launch

/**
 * Ambiente "Inmersión": unifica la bitácora de buceo y apnea con las salidas de
 * pesca submarina, en pestañas deslizables (mismo patrón que Entrenamiento y Aprender).
 */
@Composable
fun InmersionScreen(
    historialViewModel: HistorialViewModel,
    pescaViewModel: PescaSubmarinaViewModel,
    onAgregarInmersion: () -> Unit,
    onAgregarSalidaPesca: () -> Unit,
    onVerDetalleInmersion: (Long) -> Unit,     // NUEVO
    modifier: Modifier = Modifier
) {
    val titulos = listOf("Buceo y apnea", "Pesca submarina")
    val iconos = listOf(R.drawable.ic_apnea, R.drawable.ic_pesca)
    val pagerState = rememberPagerState(initialPage = 0) { titulos.size }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent
        ) {
            titulos.forEachIndexed { index, titulo ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = { Text(titulo) },
                    icon = {
                        Icon(
                            painter = painterResource(iconos[index]),
                            contentDescription = titulo
                        )
                    }
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> HistorialScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = historialViewModel,
                    onAgregarClick = onAgregarInmersion,
                    onVerDetalle = onVerDetalleInmersion   // NUEVO
                )
                1 -> PescaSubmarinaScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = pescaViewModel,
                    onAgregarClick = onAgregarSalidaPesca
                )
            }
        }
    }
}
