package com.berterogarcia.appnea.ui.inicio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.berterogarcia.appnea.AppContainer
import com.berterogarcia.appnea.navigation.Ruta
import com.berterogarcia.appnea.navigation.abrir
import com.berterogarcia.appnea.navigation.irAPestania
import com.berterogarcia.appnea.navigation.volverDesde
import com.berterogarcia.appnea.ui.clima.ClimaViewModel
import com.berterogarcia.appnea.ui.condiciones.CondicionesScreen

/** Inicio (registrar inmersión, últimas inmersiones, resumen del clima) y Condiciones. */
fun NavGraphBuilder.inicioGraph(navController: NavHostController, app: AppContainer) {
    composable<Ruta.Inicio> {
        val climaViewModel: ClimaViewModel = viewModel { ClimaViewModel(app.condiciones, app.ubicacion) }
        val inicioViewModel: InicioViewModel = viewModel { InicioViewModel(app.inmersionRepository) }
        val usuario by app.sessionManager.usuarioActual.collectAsStateWithLifecycle()
        // En las cuentas de Google el usuario es el email: se saluda con el nombre del perfil
        // (mientras carga queda solo "Hola", para no mostrar el email un instante).
        val perfil by remember { app.perfilRepository.perfilActual }
            .collectAsStateWithLifecycle(initialValue = null)
        val saludo = if (usuario?.contains('@') == true) perfil?.nombre?.takeIf { it.isNotBlank() } else usuario
        InicioScreen(
            nombreUsuario = saludo,
            inicioViewModel = inicioViewModel,
            climaViewModel = climaViewModel,
            onNuevaInmersion = { navController.abrir(Ruta.NuevaInmersion) },
            onVerDetalleInmersion = { id -> navController.abrir(Ruta.DetalleInmersion(id)) },
            onVerCondiciones = { navController.abrir(Ruta.Condiciones) },
            onIrA = { destino -> navController.irAPestania(destino) }
        )
    }
    composable<Ruta.Condiciones> { entrada ->
        // Reutiliza el ViewModel de Inicio (así no vuelve a descargar todo)
        val duenio = remember(entrada) {
            runCatching { navController.getBackStackEntry(Ruta.Inicio) }.getOrNull()
        } ?: entrada
        val climaViewModel: ClimaViewModel = viewModel(viewModelStoreOwner = duenio) {
            ClimaViewModel(app.condiciones, app.ubicacion)
        }
        CondicionesScreen(viewModel = climaViewModel, onVolver = { navController.volverDesde(entrada) })
    }
}
