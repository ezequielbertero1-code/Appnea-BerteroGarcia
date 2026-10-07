package com.berterogarcia.appnea

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.berterogarcia.appnea.navigation.AppNavHost
import com.berterogarcia.appnea.navigation.PREFIJO_PERFIL
import com.berterogarcia.appnea.navigation.RUTAS_SIN_BARRA
import com.berterogarcia.appnea.navigation.RUTA_LOGIN
import com.berterogarcia.appnea.navigation.RUTA_REGISTRO
import com.berterogarcia.appnea.navigation.irAPestania
import com.berterogarcia.appnea.ui.sesion.EstadoArranque
import com.berterogarcia.appnea.ui.sesion.SesionViewModel
import com.berterogarcia.appnea.ui.theme.MyApplicationTheme
import com.berterogarcia.appnea.ui.theme.fondoApp

class MainActivity : ComponentActivity() {

    // [NUEVO] Decide si se arranca en Inicio (sesión válida) o en Login.
    private val sesionViewModel: SesionViewModel by viewModels {
        SesionViewModel.Factory((application as MyApplication).sesionRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // El fondo de la app es siempre oscuro (fondoApp), así que forzamos íconos claros
        // en las barras del sistema para que la hora, batería, etc. se lean bien.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            // Legibilidad: como el fondo es siempre oscuro, usamos siempre el esquema oscuro
            // (si el teléfono está en modo claro, los textos quedaban oscuros sobre fondo oscuro)
            // y el color de texto por defecto pasa a ser blanco.
            MyApplicationTheme(darkTheme = true) {
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(fondoApp)
                    ) {
                        // [CAMBIO] El NavHost se crea recién cuando se sabe el destino inicial.
                        val estado by sesionViewModel.estado.collectAsStateWithLifecycle()
                        when (val e = estado) {
                            EstadoArranque.Verificando -> PantallaArranque()
                            is EstadoArranque.Listo -> MyApplicationApp(destinoInicial = e.destinoInicial)
                        }
                    }
                }
            }
        }
    }
}

/** Se ve solo unos milisegundos mientras se verifica la sesión guardada. */
@Composable
private fun PantallaArranque() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(0xFF16F7FB))
    }
}

@PreviewScreenSizes
@Composable
fun MyApplicationApp(destinoInicial: String = RUTA_LOGIN) { // [CAMBIO] recibe el destino inicial
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: destinoInicial

    val mostrarMenu = currentRoute != RUTA_LOGIN && currentRoute != RUTA_REGISTRO
    val esFormulario = currentRoute in RUTAS_SIN_BARRA // [CAMBIO] incluye los formularios del perfil

    if (mostrarMenu) {
        NavigationSuiteScaffold(
            layoutType = if (esFormulario) NavigationSuiteType.None
                        else NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo()),
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
                        label = { Text(destination.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        // [CAMBIO] Las pantallas "perfil/..." cuentan como parte de Cuenta.
                        selected = destination.name == currentRoute ||
                            (destination == AppDestinations.CUENTA && currentRoute.startsWith(PREFIJO_PERFIL)),
                        onClick = {
                            if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                                navController.irAPestania(destination)
                            }
                        }
                    )
                }
            }
        ) {
            Scaffold(modifier = Modifier.fillMaxSize(), containerColor = Color.Transparent) { innerPadding ->
                AppNavHost(
                    navController = navController,
                    startDestination = destinoInicial,
                    modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
                )
            }
        }
    } else {
        Scaffold(modifier = Modifier.fillMaxSize(), containerColor = Color.Transparent) { innerPadding ->
            AppNavHost(
                navController = navController,
                startDestination = destinoInicial,
                modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
            )
        }
    }
}
// Ambientes principales de la app (barra de navegación).
// - INICIO: clima del día + explicación de cada ambiente.
// - ENTRENAMIENTO: tablas de apnea y cronómetro libre (antes "Cronometro").
// - INMERSION: bitácora unificada de buceo, apnea y pesca submarina (antes "Apnea" y "Pesca").
// - APRENDER: contenido teórico de apnea y buceo (antes "Información").
// - CUENTA: perfil del usuario.
enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    INICIO("Inicio", R.drawable.ic_inicio),
    ENTRENAMIENTO("Entrenar", R.drawable.ic_cronometro),
    INMERSION("Inmersión", R.drawable.ic_apnea),
    APRENDER("Aprender", R.drawable.ic_informate),
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
