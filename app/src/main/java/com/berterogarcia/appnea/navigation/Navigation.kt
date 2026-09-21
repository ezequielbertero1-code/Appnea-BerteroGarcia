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
import com.berterogarcia.appnea.MyApplication
import com.berterogarcia.appnea.ui.cronometro.CronometroScreen
import com.berterogarcia.appnea.ui.cronometro.EntrenamientoApneaViewModel
import com.berterogarcia.appnea.ui.cuenta.CuentaScreen
import com.berterogarcia.appnea.ui.cuenta.CuentaViewModel
import com.berterogarcia.appnea.ui.historial.HistorialScreen
import com.berterogarcia.appnea.ui.informacion.InformacionScreen
import com.berterogarcia.appnea.ui.clima.ClimaScreen
import com.berterogarcia.appnea.ui.clima.ClimaViewModel
import com.berterogarcia.appnea.ui.pesca.NuevaSalidaPescaScreen
import com.berterogarcia.appnea.ui.pesca.NuevaSalidaPescaViewModel
import com.berterogarcia.appnea.ui.pesca.PescaSubmarinaScreen
import com.berterogarcia.appnea.ui.pesca.PescaSubmarinaViewModel
import com.berterogarcia.appnea.ui.historial.HistorialViewModel
import com.berterogarcia.appnea.ui.login.LoginScreen
import com.berterogarcia.appnea.ui.login.LoginViewModel
import com.berterogarcia.appnea.ui.nuevainmersion.NuevaInmersionScreen
import com.berterogarcia.appnea.ui.nuevainmersion.NuevaInmersionViewModel
import com.berterogarcia.appnea.ui.registro.RegistroScreen
import com.berterogarcia.appnea.ui.registro.RegistroViewModel


//Estas son pantallas que no aparecen en la barra de la app.
const val RUTA_REGISTRO = "registro"
const val RUTA_LOGIN = "login"
private const val RUTA_NUEVA_INMERSION = "nueva_inmersion"
private const val RUTA_NUEVA_SALIDA_PESCA = "nueva_salida_pesca"

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
                    navController.navigate(AppDestinations.APNEA.name) {
                        popUpTo(RUTA_LOGIN) { inclusive = true } // Esto hace que una vez iniciado sesion no pueda volver al login
                    }
                },
                onRegistroClick = { navController.navigate(RUTA_REGISTRO) }
            )
        }

        //===========================CUENTA-USUARIO============================================
        composable(AppDestinations.CUENTA.name) {
            val viewModel: CuentaViewModel = viewModel(
                factory = CuentaViewModel.Factory(app.sessionManager)
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

        //================================INICIO================================================
        composable(AppDestinations.INICIO.name) {
            Text("Pantalla de inicio") // placeholder, aca iria la primera pagina que ve el usuario.
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

        //==============================CRONOMETRO=============================================
        composable(AppDestinations.CRONOMETRO.name) {
            val entrenamientoViewModel: EntrenamientoApneaViewModel = viewModel(
                factory = EntrenamientoApneaViewModel.Factory(
                    app.tablaPersonalizadaRepository,
                    app.sesionApneaRepository,
                    app.sonidoManager
                )
            )
            CronometroScreen(entrenamientoViewModel = entrenamientoViewModel)
        }

        //=============================INFORMACION==========================================
        composable(AppDestinations.INFORMACION.name) {
            InformacionScreen()
        }

        //=============================PESCA SUBMARINA======================================
        composable(AppDestinations.PESCA.name) {
            val viewModel: PescaSubmarinaViewModel = viewModel(
                factory = PescaSubmarinaViewModel.Factory(app.salidaPescaRepository)
            )
            PescaSubmarinaScreen(
                viewModel = viewModel,
                onAgregarClick = { navController.navigate(RUTA_NUEVA_SALIDA_PESCA) }
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

        //================================CLIMA==============================================
        composable(AppDestinations.CLIMA.name) {
            val viewModel: ClimaViewModel = viewModel(
                factory = ClimaViewModel.Factory(app.applicationContext)
            )
            ClimaScreen(viewModel = viewModel)
        }

        //===============================APNEA=============================================
        composable(AppDestinations.APNEA.name) {
            val viewModel: HistorialViewModel = viewModel(
                factory = HistorialViewModel.Factory(app.inmersionRepository)
            )
            HistorialScreen(
                viewModel = viewModel,
                onAgregarClick = { navController.navigate(RUTA_NUEVA_INMERSION) },
                name = "Eze" // NOMBRE DEL USUARIO
            )
        }

        //==============================NUEVA-INMERSION=========================================
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
