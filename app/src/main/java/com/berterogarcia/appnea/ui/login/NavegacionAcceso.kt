package com.berterogarcia.appnea.ui.login

import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.berterogarcia.appnea.AppContainer
import com.berterogarcia.appnea.data.auth.GoogleAuth
import com.berterogarcia.appnea.dominio.modelo.CuentaGoogle
import com.berterogarcia.appnea.navigation.Ruta
import com.berterogarcia.appnea.navigation.abrir
import com.berterogarcia.appnea.navigation.entrarALaApp
import com.berterogarcia.appnea.ui.registro.RegistroScreen
import com.berterogarcia.appnea.ui.registro.RegistroViewModel

/** Login, registro y registro de una cuenta de Google nueva. */
fun NavGraphBuilder.accesoGraph(navController: NavHostController, app: AppContainer) {
    composable<Ruta.Login> {
        val viewModel: LoginViewModel = viewModel {
            LoginViewModel(
                app.usuarioRepository,
                app.cuentaGoogleRepository,
                app.sessionManager,
                app.preferenciasLogin
            )
        }
        LoginScreen(
            viewModel = viewModel,
            onLoginExitoso = { navController.entrarALaApp() },
            onRegistroClick = { navController.abrir(Ruta.Registro) },
            onRegistroGoogle = { c -> navController.abrir(Ruta.RegistroGoogle(c.email, c.nombre, c.apellido)) },
            pedirCuentaGoogle = GoogleAuth::pedirCuenta
        )
    }
    composable<Ruta.Registro> {
        val viewModel: RegistroViewModel = viewModel {
            RegistroViewModel(
                app.registroRepository,
                app.cuentaGoogleRepository,
                app.sessionManager,
                cuentaGoogle = null,
                createSavedStateHandle()
            )
        }
        RegistroScreen(
            viewModel = viewModel,
            onCancelar = { navController.popBackStack() },
            // La sesión ya quedó iniciada: va a Inicio y limpia la pila
            onRegistroExitoso = { navController.entrarALaApp() }
        )
    }
    // Empieza en Datos personales, con lo que devolvió Google ya cargado
    composable<Ruta.RegistroGoogle> { entrada ->
        val ruta = entrada.toRoute<Ruta.RegistroGoogle>()
        val viewModel: RegistroViewModel = viewModel {
            RegistroViewModel(
                app.registroRepository,
                app.cuentaGoogleRepository,
                app.sessionManager,
                CuentaGoogle(ruta.email, ruta.nombre, ruta.apellido),
                createSavedStateHandle()
            )
        }
        RegistroScreen(
            viewModel = viewModel,
            onCancelar = { navController.popBackStack() },
            onRegistroExitoso = { navController.entrarALaApp() }
        )
    }
}
