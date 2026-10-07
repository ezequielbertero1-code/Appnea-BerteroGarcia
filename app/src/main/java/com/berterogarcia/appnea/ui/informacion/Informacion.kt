package com.berterogarcia.appnea.ui.informacion

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
import kotlinx.coroutines.launch

@Composable
fun InformacionScreen(modifier: Modifier = Modifier) {
    val titulos = listOf("Apnea", "Buceo")
    val iconos = listOf(R.drawable.ic_apnea, R.drawable.ic_buceo)
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
                0 -> ContenidoApnea(modifier = Modifier.fillMaxSize())
                1 -> ContenidoBuceo(modifier = Modifier.fillMaxSize())
            }
        }
    }
}