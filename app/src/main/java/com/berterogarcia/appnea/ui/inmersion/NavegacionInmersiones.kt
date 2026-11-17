package com.berterogarcia.appnea.ui.inmersion

import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.berterogarcia.appnea.AppContainer
import com.berterogarcia.appnea.navigation.Ruta
import com.berterogarcia.appnea.navigation.abrir
import com.berterogarcia.appnea.navigation.volverDesde
import com.berterogarcia.appnea.ui.detalleinmersion.DetalleInmersionScreen
import com.berterogarcia.appnea.ui.detalleinmersion.DetalleInmersionViewModel
import com.berterogarcia.appnea.ui.historial.HistorialViewModel
import com.berterogarcia.appnea.ui.nuevainmersion.NuevaInmersionScreen
import com.berterogarcia.appnea.ui.nuevainmersion.NuevaInmersionViewModel

/** Bitácora de buceo y apnea: historial, alta, edición y detalle de una inmersión. */
fun NavGraphBuilder.inmersionesGraph(navController: NavHostController, app: AppContainer) {
    composable<Ruta.Inmersiones> {
        val historialViewModel: HistorialViewModel = viewModel { HistorialViewModel(app.inmersionRepository) }
        InmersionScreen(
            historialViewModel = historialViewModel,
            onAgregarInmersion = { navController.abrir(Ruta.NuevaInmersion) },
            onVerDetalleInmersion = { id -> navController.abrir(Ruta.DetalleInmersion(id)) },
            onVerEstadisticas = { navController.abrir(Ruta.Estadisticas()) }
        )
    }
    composable<Ruta.NuevaInmersion> {
        val viewModel: NuevaInmersionViewModel = viewModel {
            NuevaInmersionViewModel(
                app.inmersionRepository,
                app.registrarInmersion,
                app.perfilRepository,
                app.ubicacion,
                idEditar = null,
                createSavedStateHandle()
            )
        }
        NuevaInmersionScreen(
            viewModel = viewModel,
            onGuardado = { navController.popBackStack() },
            // Abre el alta de equipo; al guardarlo vuelve acá y la lista se actualiza sola
            onCargarEquipo = { navController.abrir(Ruta.FormEquipo()) }
        )
    }
    // Editar: el mismo formulario, cargado con la inmersión guardada
    composable<Ruta.EditarInmersion> { entrada ->
        val id = entrada.toRoute<Ruta.EditarInmersion>().id
        val viewModel: NuevaInmersionViewModel = viewModel {
            NuevaInmersionViewModel(
                app.inmersionRepository,
                app.registrarInmersion,
                app.perfilRepository,
                app.ubicacion,
                id,
                createSavedStateHandle()
            )
        }
        NuevaInmersionScreen(
            viewModel = viewModel,
            onGuardado = { navController.volverDesde(entrada) }, // vuelve al detalle, que se actualiza solo
            onCargarEquipo = { navController.abrir(Ruta.FormEquipo()) }
        )
    }
    composable<Ruta.DetalleInmersion> { entrada ->
        val id = entrada.toRoute<Ruta.DetalleInmersion>().id
        val viewModel: DetalleInmersionViewModel = viewModel {
            DetalleInmersionViewModel(app.inmersionRepository, id, app.exportador)
        }
        DetalleInmersionScreen(
            viewModel = viewModel,
            onEliminada = { navController.popBackStack() },
            onEditar = { idEditar -> navController.abrir(Ruta.EditarInmersion(idEditar)) },
            onVolver = { navController.volverDesde(entrada) }
        )
    }
}
