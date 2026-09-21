package com.berterogarcia.appnea.ui.informacion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.berterogarcia.appnea.R

/**
 * Pantalla de Información: contenido básico de referencia sobre Apnea/Freediving y
 * Buceo autónomo (Scuba Open Water), con imágenes ilustrativas y prevención de
 * accidentes intercalada en cada tema.
 *
 * Es contenido estático (no depende de un ViewModel ni de la base de datos).
 */
@Composable
fun InformacionScreen(modifier: Modifier = Modifier) {
    var pestaniaSeleccionada by rememberSaveable { mutableIntStateOf(0) }
    val titulos = listOf("Apnea", "Buceo")

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pestaniaSeleccionada) {
            titulos.forEachIndexed { index, titulo ->
                Tab(
                    selected = pestaniaSeleccionada == index,
                    onClick = { pestaniaSeleccionada = index },
                    text = { Text(titulo) },
                    icon = {
                        Icon(
                            painter = painterResource(
                                if (index == 0) R.drawable.ic_apnea else R.drawable.ic_informate
                            ),
                            contentDescription = titulo
                        )
                    }
                )
            }
        }
        when (pestaniaSeleccionada) {
            0 -> ContenidoApnea(modifier = Modifier.fillMaxSize())
            1 -> ContenidoBuceo(modifier = Modifier.fillMaxSize())
        }
    }
}
