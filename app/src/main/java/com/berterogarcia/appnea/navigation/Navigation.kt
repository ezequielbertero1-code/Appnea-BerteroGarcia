package com.berterogarcia.appnea.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.berterogarcia.appnea.contenedor
import com.berterogarcia.appnea.ui.entrenamiento.entrenamientoGraph
import com.berterogarcia.appnea.ui.estadisticas.estadisticasGraph
import com.berterogarcia.appnea.ui.informacion.aprenderGraph
import com.berterogarcia.appnea.ui.inicio.inicioGraph
import com.berterogarcia.appnea.ui.inmersion.inmersionesGraph
import com.berterogarcia.appnea.ui.login.accesoGraph
import com.berterogarcia.appnea.ui.perfil.perfilGraph

/**
 * El grafo de la app. Cada sección declara sus pantallas (y arma sus ViewModels) en su propio
 * paquete: acá solo se juntan.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: Ruta, // lo decide SesionViewModel: Inicio si hay sesión, si no Login
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.contenedor()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        accesoGraph(navController, app)
        inicioGraph(navController, app)
        entrenamientoGraph(navController, app)
        inmersionesGraph(navController, app)
        estadisticasGraph(navController, app)
        aprenderGraph()
        perfilGraph(navController, app)
    }
}
