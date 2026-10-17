package com.berterogarcia.appnea.ui.informacion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.components.BarraSuperior
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.Divisor
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante
import kotlinx.coroutines.launch

/** Aprender (estilo "Appnea circular"): barra superior, pestañas Apnea / Buceo y temas desplegables. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformacionScreen(modifier: Modifier = Modifier) {
    val titulos = listOf(stringResource(R.string.aprender_tab_apnea), stringResource(R.string.aprender_tab_buceo))
    val pagerState = rememberPagerState(initialPage = 0) { titulos.size }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        BarraSuperior(titulo = stringResource(R.string.nav_aprender))
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
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> ContenidoApnea(modifier = Modifier.fillMaxSize())
                1 -> ContenidoBuceo(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
