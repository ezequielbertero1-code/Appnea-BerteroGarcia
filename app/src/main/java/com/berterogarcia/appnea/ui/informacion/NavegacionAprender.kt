package com.berterogarcia.appnea.ui.informacion

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.berterogarcia.appnea.navigation.Ruta

/** Contenido teórico de apnea y buceo (no tiene estado: no necesita ViewModel). */
fun NavGraphBuilder.aprenderGraph() {
    composable<Ruta.Aprender> { InformacionScreen() }
}
