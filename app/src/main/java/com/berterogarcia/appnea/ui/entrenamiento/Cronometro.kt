package com.berterogarcia.appnea.ui.entrenamiento

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
import androidx.compose.material3.Icon
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.BotonSecundario
import kotlinx.coroutines.launch

@Composable
fun CronometroScreen(
    modifier: Modifier = Modifier,
    cronometroViewModel: CronometroViewModel = viewModel(),
    entrenamientoViewModel: EntrenamientoApneaViewModel
) {
    val titulos = listOf("Tablas", "Libre")
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
                            painter = painterResource(
                                if (index == 0) R.drawable.ic_entrenamiento else R.drawable.ic_cronometro
                            ),
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
                0 -> EntrenamientoApneaScreen(modifier = Modifier.fillMaxSize(), viewModel = entrenamientoViewModel)
                1 -> CronometroLibre(modifier = Modifier.fillMaxSize(), viewModel = cronometroViewModel)
            }
        }
    }
}

@Composable
private fun CronometroLibre(
    modifier: Modifier = Modifier,
    viewModel: CronometroViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = uiState.tiempoFormateado, fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BotonPrincipal(texto = "Iniciar", onClick = viewModel::onIniciar, modifier = Modifier.weight(1f))
            BotonSecundario(texto = "Parar", onClick = viewModel::onParar, modifier = Modifier.weight(1f))
            BotonSecundario(texto = "Resetear", onClick = viewModel::onResetear, modifier = Modifier.weight(1f))
        }
    }
}