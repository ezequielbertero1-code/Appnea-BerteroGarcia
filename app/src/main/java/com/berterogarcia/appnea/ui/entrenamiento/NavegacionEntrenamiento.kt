package com.berterogarcia.appnea.ui.entrenamiento

import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.berterogarcia.appnea.AppContainer
import com.berterogarcia.appnea.navigation.Ruta
import com.berterogarcia.appnea.navigation.abrir
import com.berterogarcia.appnea.navigation.volverDesde
import com.berterogarcia.appnea.ui.estadisticas.PESTANIA_ENTRENAMIENTO

/** Tablas de apnea, cronómetro libre y creación de tablas propias. */
fun NavGraphBuilder.entrenamientoGraph(navController: NavHostController, app: AppContainer) {
    composable<Ruta.Entrenamiento> {
        val entrenamientoViewModel: EntrenamientoApneaViewModel = viewModel {
            EntrenamientoApneaViewModel(
                app.tablaPersonalizadaRepository,
                app.sesionApneaRepository,
                app.motorEntrenamiento,
                createSavedStateHandle()
            )
        }
        val cronometroViewModel: CronometroViewModel = viewModel { CronometroViewModel(app.intentoApneaRepository) }
        CronometroScreen(
            entrenamientoViewModel = entrenamientoViewModel,
            cronometroViewModel = cronometroViewModel,
            onCrearTabla = { navController.abrir(Ruta.CrearTabla) },
            onVerEstadisticas = { navController.abrir(Ruta.Estadisticas(PESTANIA_ENTRENAMIENTO)) }
        )
    }
    composable<Ruta.CrearTabla> { entrada ->
        val crearTablaViewModel: CrearTablaViewModel = viewModel { CrearTablaViewModel(app.tablaPersonalizadaRepository) }
        CrearTablaScreen(
            viewModel = crearTablaViewModel,
            onCerrar = { navController.volverDesde(entrada) }
        )
    }
}
