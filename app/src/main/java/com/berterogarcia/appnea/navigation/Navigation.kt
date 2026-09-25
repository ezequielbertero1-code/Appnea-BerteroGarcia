package com.berterogarcia.appnea.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.berterogarcia.appnea.AppDestinations
import com.berterogarcia.appnea.MyApplication
import com.berterogarcia.appnea.ui.clima.ClimaViewModel
import com.berterogarcia.appnea.ui.condiciones.CondicionesScreen
import com.berterogarcia.appnea.ui.cronometro.CronometroScreen
import com.berterogarcia.appnea.ui.cronometro.EntrenamientoApneaViewModel
import com.berterogarcia.appnea.ui.cuenta.CuentaScreen
import com.berterogarcia.appnea.ui.cuenta.CuentaViewModel
import com.berterogarcia.appnea.ui.historial.HistorialViewModel
import com.berterogarcia.appnea.ui.informacion.InformacionScreen
import com.berterogarcia.appnea.ui.inicio.InicioScreen
import com.berterogarcia.appnea.ui.inmersion.InmersionScreen
import com.berterogarcia.appnea.ui.login.LoginScreen
import com.berterogarcia.appnea.ui.login.LoginViewModel
import com.berterogarcia.appnea.ui.nuevainmersion.NuevaInmersionScreen
import com.berterogarcia.appnea.ui.nuevainmersion.NuevaInmersionViewModel
import com.berterogarcia.appnea.ui.pesca.NuevaSalidaPescaScreen
import com.berterogarcia.appnea.ui.pesca.NuevaSalidaPescaViewModel
import com.berterogarcia.appnea.ui.pesca.PescaSubmarinaViewModel
import com.berterogarcia.appnea.ui.registro.RegistroScreen
import com.berterogarcia.appnea.ui.registro.RegistroViewModel

//Estas son pantallas que no aparecen en la barra de la app.
const val RUTA_REGISTRO = "registro"
const val RUTA_LOGIN = "login"
private const val RUTA_NUEVA_INMERSION = "nueva_inmersion"
private const val RUTA_NUEVA_SALIDA_PESCA = "nueva_salida_pesca"
private const val RUTA_CONDICIONES = "condiciones"

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as MyApplication

    NavHost(
        navController = navController,
        startDestination = RUTA_LOGIN,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {

        //================================LOGIN==========================================
        composable(RUTA_LOGIN) {
            val viewModel: LoginViewModel = viewModel(
                factory = LoginViewModel.Factory(app.usuarioRepository, app.sessionManager)
            )
            LoginScreen(
                viewModel = viewModel,
                onLoginExitoso = {
                    navController.navigate(AppDestinations.INICIO.name) {
                        popUpTo(RUTA_LOGIN) { inclusive = true } // Esto hace que una vez iniciado sesion no pueda volver al login
                    }
                },
                onRegistroClick = { navController.navigate(RUTA_REGISTRO) }
            )
        }
        composable(RUTA_REGISTRO) {
            val viewModel: RegistroViewModel = viewModel(
                factory = RegistroViewModel.Factory(app.usuarioRepository)
            )
            RegistroScreen(
                viewModel = viewModel,
                onRegistroExitoso = { navController.popBackStack() } // vuelve al login para que inicie sesión
            )
        }

        //================================INICIO================================================
        // Clima del día + explicación de cada ambiente de la app.
        composable(AppDestinations.INICIO.name) {
            val climaViewModel: ClimaViewModel = viewModel(
                factory = ClimaViewModel.Factory(app.applicationContext)
            )
            InicioScreen(
                nombreUsuario = app.sessionManager.obtenerUsuario(),
                climaViewModel = climaViewModel,
                onVerCondiciones = { navController.navigate(RUTA_CONDICIONES) },
                onIrA = { destino ->
                    // Misma navegación que la barra inferior
                    navController.navigate(destino.name) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        //================================CONDICIONES (clima, mareas, sol, luna, pesca)=========
        composable(RUTA_CONDICIONES) { entrada ->
            // Reutiliza el mismo ViewModel de Inicio (así no vuelve a descargar todo)
            val duenio = remember(entrada) {
                runCatching { navController.getBackStackEntry(AppDestinations.INICIO.name) }.getOrNull()
            } ?: entrada
            val climaViewModel: ClimaViewModel = viewModel(
                viewModelStoreOwner = duenio,
                factory = ClimaViewModel.Factory(app.applicationContext)
            )
            CondicionesScreen(viewModel = climaViewModel)
        }

        //==============================ENTRENAMIENTO (antes Cronometro)=========================
        composable(AppDestinations.ENTRENAMIENTO.name) {
            val entrenamientoViewModel: EntrenamientoApneaViewModel = viewModel(
                factory = EntrenamientoApneaViewModel.Factory(
                    app.tablaPersonalizadaRepository,
                    app.sesionApneaRepository,
                    app.sonidoManager
                )
            )
            CronometroScreen(entrenamientoViewModel = entrenamientoViewModel)
        }

        //==============================INMERSION (buceo, apnea y pesca submarina)===============
        composable(AppDestinations.INMERSION.name) {
            val historialViewModel: HistorialViewModel = viewModel(
                factory = HistorialViewModel.Factory(app.inmersionRepository)
            )
            val pescaViewModel: PescaSubmarinaViewModel = viewModel(
                factory = PescaSubmarinaViewModel.Factory(app.salidaPescaRepository)
            )
            InmersionScreen(
                historialViewModel = historialViewModel,
                pescaViewModel = pescaViewModel,
                onAgregarInmersion = { navController.navigate(RUTA_NUEVA_INMERSION) },
                onAgregarSalidaPesca = { navController.navigate(RUTA_NUEVA_SALIDA_PESCA) }
            )
        }
        composable(RUTA_NUEVA_INMERSION) {
            val viewModel: NuevaInmersionViewModel = viewModel(
                factory = NuevaInmersionViewModel.Factory(app.inmersionRepository, app.applicationContext)
            )
            NuevaInmersionScreen(
                viewModel = viewModel,
                onGuardado = { navController.popBackStack() }
            )
        }
        composable(RUTA_NUEVA_SALIDA_PESCA) {
            val viewModel: NuevaSalidaPescaViewModel = viewModel(
                factory = NuevaSalidaPescaViewModel.Factory(app.salidaPescaRepository, app.applicationContext)
            )
            NuevaSalidaPescaScreen(
                viewModel = viewModel,
                onGuardado = { navController.popBackStack() }
            )
        }

        //=============================APRENDER (antes Informacion)==========================
        composable(AppDestinations.APRENDER.name) {
            InformacionScreen()
        }

        //===========================CUENTA-USUARIO============================================
        composable(AppDestinations.CUENTA.name) {
            val viewModel: CuentaViewModel = viewModel(
                factory = CuentaViewModel.Factory(
                    app.sessionManager,
                    app.inmersionRepository,
                    app.sesionApneaRepository,
                    app.salidaPescaRepository
                )
            )
            CuentaScreen(
                viewModel = viewModel,
                onCerrarSesion = {
                    navController.navigate(RUTA_LOGIN) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
    }
}
