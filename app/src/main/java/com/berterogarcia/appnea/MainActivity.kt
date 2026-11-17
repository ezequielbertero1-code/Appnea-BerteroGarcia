package com.berterogarcia.appnea

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.berterogarcia.appnea.navigation.AppDestinations
import com.berterogarcia.appnea.navigation.AppNavHost
import com.berterogarcia.appnea.navigation.Ruta
import com.berterogarcia.appnea.navigation.esDeAcceso
import com.berterogarcia.appnea.navigation.esFormulario
import com.berterogarcia.appnea.navigation.irAPestania
import com.berterogarcia.appnea.navigation.pestaniaAbierta
import com.berterogarcia.appnea.ui.components.LocalOcultarBarra
import com.berterogarcia.appnea.ui.sesion.EstadoArranque
import com.berterogarcia.appnea.ui.sesion.SesionViewModel
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.FondoBarraNavegacion
import com.berterogarcia.appnea.ui.theme.AppneaTheme
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante
import com.berterogarcia.appnea.ui.theme.fondoApp

class MainActivity : ComponentActivity() {

    // Decide si se arranca en Inicio (sesión válida) o en Login.
    private val sesionViewModel: SesionViewModel by viewModels {
        viewModelFactory { initializer { SesionViewModel(contenedor().sesionRepository) } }
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
            AppneaTheme {
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(fondoApp)
                    ) {
                        // El NavHost se crea recién cuando se sabe el destino inicial.
                        val estado by sesionViewModel.estado.collectAsStateWithLifecycle()
                        when (val e = estado) {
                            EstadoArranque.Verificando -> PantallaArranque()
                            is EstadoArranque.Listo ->
                                AppneaRaiz(destinoInicial = if (e.haySesion) Ruta.Inicio else Ruta.Login)
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
        CircularProgressIndicator(color = Acento)
    }
}

@PreviewScreenSizes
@Composable
fun AppneaRaiz(destinoInicial: Ruta = Ruta.Login) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destino = backStackEntry?.destination
    // Pestaña a marcar en la barra: la de origen, aunque estés en un detalle o en Condiciones.
    // Se recalcula cada vez que cambia la pantalla (backStackEntry).
    val pestaniaSeleccionada = remember(backStackEntry) { navController.pestaniaAbierta() }

    // Sin barra en login/registro (también mientras arranca, si el destino inicial es el login)
    val mostrarMenu = if (destino == null) destinoInicial != Ruta.Login else !destino.esDeAcceso()
    val ocultarBarra = remember { mutableStateOf(false) }
    // Sin barra: formularios o una pantalla que la pidió ocultar
    val esFormulario = destino?.esFormulario() == true || ocultarBarra.value

    // Las pantallas pueden ocultar la barra (ej. entrenamiento en curso): ver OcultarBarraNavegacion()
    CompositionLocalProvider(LocalOcultarBarra provides ocultarBarra) {
        if (mostrarMenu) {
            // Celular: barra corta de M3 (64 dp); tablet: barra lateral. Sin barra en formularios.
            val tipoNavegacion = if (esFormulario) NavigationSuiteType.None
                                 else NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())
            // Estilo "Appnea circular": sin pastilla detrás del ícono; el elegido va relleno y en cian
            val coloresItem = ShortNavigationBarItemDefaults.colors(
                selectedIconColor = Acento,
                selectedTextColor = Acento,
                selectedIndicatorColor = Color.Transparent,
                unselectedIconColor = TextoSuperficieVariante,
                unselectedTextColor = TextoSuperficieVariante
            )
            NavigationSuiteScaffold(
                navigationSuiteType = tipoNavegacion,
                containerColor = Color.Transparent,
                navigationSuiteColors = NavigationSuiteDefaults.colors(
                    shortNavigationBarContainerColor = FondoBarraNavegacion,
                    shortNavigationBarContentColor = TextoSuperficieVariante,
                    wideNavigationRailColors = WideNavigationRailDefaults.colors(containerColor = Color.Transparent)
                ),
                navigationItems = {
                    AppDestinations.entries.forEach { destination ->
                        // Las pantallas del perfil cuentan como parte de Cuenta (ver pestaniaAbierta).
                        val elegida = destination == pestaniaSeleccionada
                        NavigationSuiteItem(
                            selected = elegida,
                            onClick = {
                                if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                                    navController.irAPestania(destination)
                                }
                            },
                            // La etiqueta ya describe el destino: el ícono no repite la descripción
                            icon = {
                                Icon(
                                    painterResource(if (elegida) destination.iconoElegido else destination.icono),
                                    contentDescription = null
                                )
                            },
                            label = {
                                Text(stringResource(destination.etiqueta), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            navigationSuiteType = tipoNavegacion,
                            colors = coloresItem
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
}
