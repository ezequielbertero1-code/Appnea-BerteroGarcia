package com.berterogarcia.appnea.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.berterogarcia.appnea.AppDestinations
import com.berterogarcia.appnea.Greeting
import com.berterogarcia.appnea.MyApplication
import com.berterogarcia.appnea.ui.cronometro.CronometroScreen
import com.berterogarcia.appnea.ui.historial.HistorialScreen
import com.berterogarcia.appnea.ui.historial.HistorialViewModel
import com.berterogarcia.appnea.ui.nuevainmersion.NuevaInmersionScreen
import com.berterogarcia.appnea.ui.nuevainmersion.NuevaInmersionViewModel

private const val RUTA_NUEVA_INMERSION = "nueva_inmersion"

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as MyApplication

    NavHost(
        navController = navController,
        startDestination = AppDestinations.INICIO.name,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(AppDestinations.INICIO.name) {
            Greeting(name = "Hola")
        }
        composable(AppDestinations.CRONOMETRO.name) {
            CronometroScreen()
        }
        composable(AppDestinations.HISTORIAL.name) {
            val viewModel: HistorialViewModel = viewModel(
                factory = HistorialViewModel.Factory(app.inmersionRepository)
            )
            HistorialScreen(
                viewModel = viewModel,
                onAgregarClick = { navController.navigate(RUTA_NUEVA_INMERSION) },
                name = "Eze" // NOMBRE DEL USUARIO
            )
        }
        composable(AppDestinations.CUENTA.name) {
            Text("Pantalla de cuenta")
        }
        composable(RUTA_NUEVA_INMERSION) {
            val viewModel: NuevaInmersionViewModel = viewModel(
                factory = NuevaInmersionViewModel.Factory(app.inmersionRepository)
            )
            NuevaInmersionScreen(
                viewModel = viewModel,
                onGuardado = { navController.popBackStack() }
            )
        }
    }
}