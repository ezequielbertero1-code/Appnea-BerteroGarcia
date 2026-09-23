package com.berterogarcia.appnea

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.berterogarcia.appnea.navigation.AppNavHost
import com.berterogarcia.appnea.navigation.RUTA_LOGIN
import com.berterogarcia.appnea.navigation.RUTA_REGISTRO
import com.berterogarcia.appnea.ui.theme.MyApplicationTheme
import com.berterogarcia.appnea.ui.theme.fondoApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            MyApplicationTheme(darkTheme = isSystemInDarkTheme()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(fondoApp)
                ) {
                    MyApplicationApp()
                }
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun MyApplicationApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: RUTA_LOGIN

    val mostrarMenu = currentRoute != RUTA_LOGIN && currentRoute != RUTA_REGISTRO

    if (mostrarMenu) {
        NavigationSuiteScaffold(
            containerColor = Color.Transparent,
            navigationSuiteColors = NavigationSuiteDefaults.colors(
                navigationBarContainerColor = Color.Transparent,
                navigationRailContainerColor = Color.Transparent,
                navigationDrawerContainerColor = Color.Transparent
            ),
            navigationSuiteItems = {
                AppDestinations.entries.forEach { destination ->
                    item(
                        icon = { Icon(painterResource(destination.icon), contentDescription = destination.label, modifier = Modifier.size(28.dp)) },
                        label = { Text(destination.label) },
                        selected = destination.name == currentRoute,
                        onClick = {
                            if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                                navController.navigate(destination.name) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        ) {
            Scaffold(modifier = Modifier.fillMaxSize(), containerColor = Color.Transparent) { innerPadding ->
                AppNavHost(navController = navController, modifier = Modifier.padding(innerPadding))
            }
        }
    } else {
        Scaffold(modifier = Modifier.fillMaxSize(), containerColor = Color.Transparent) { innerPadding ->
            AppNavHost(navController = navController, modifier = Modifier.padding(innerPadding))
        }
    }
}
enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    INICIO("Inicio", R.drawable.ic_inicio),
    CRONOMETRO("Cronometro", R.drawable.ic_cronometro),
    APNEA("Apnea", R.drawable.ic_apnea), // cambiar icono
    INFORMACION("Información", R.drawable.ic_informate),
    PESCA("Pesca", R.drawable.ic_pesca),
    CLIMA("Clima", R.drawable.ic_clima),
    CUENTA("Cuenta", R.drawable.ic_cuenta),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hola $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}