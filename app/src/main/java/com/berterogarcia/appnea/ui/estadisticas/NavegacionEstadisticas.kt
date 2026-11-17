package com.berterogarcia.appnea.ui.estadisticas

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.berterogarcia.appnea.AppContainer
import com.berterogarcia.appnea.navigation.AppDestinations
import com.berterogarcia.appnea.navigation.Ruta
import com.berterogarcia.appnea.navigation.abrir
import com.berterogarcia.appnea.navigation.irAPestania
import com.berterogarcia.appnea.navigation.volverDesde

/** Estadísticas de inmersiones y de entrenamiento (se abre desde Inmersión, Entrenar y Cuenta). */
fun NavGraphBuilder.estadisticasGraph(navController: NavHostController, app: AppContainer) {
    composable<Ruta.Estadisticas> { entrada ->
        val estadisticasViewModel: EstadisticasViewModel = viewModel {
            EstadisticasViewModel(
                app.inmersionRepository,
                app.sesionApneaRepository,
                app.intentoApneaRepository
            )
        }
        EstadisticasScreen(
            viewModel = estadisticasViewModel,
            pestaniaInicial = entrada.toRoute<Ruta.Estadisticas>().pestania,
            onVolver = { navController.volverDesde(entrada) },
            onVerInmersion = { id -> navController.abrir(Ruta.DetalleInmersion(id)) },
            onRegistrarInmersion = { navController.abrir(Ruta.NuevaInmersion) },
            onIrAEntrenar = { navController.irAPestania(AppDestinations.ENTRENAMIENTO) }
        )
    }
}
