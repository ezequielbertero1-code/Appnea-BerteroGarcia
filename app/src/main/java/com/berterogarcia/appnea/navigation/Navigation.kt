package com.berterogarcia.appnea.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.berterogarcia.appnea.AppDestinations
import com.berterogarcia.appnea.MyApplication
import com.berterogarcia.appnea.ui.clima.ClimaViewModel
import com.berterogarcia.appnea.ui.condiciones.CondicionesScreen
import com.berterogarcia.appnea.ui.cronometro.CronometroScreen
import com.berterogarcia.appnea.ui.cronometro.EntrenamientoApneaViewModel
import com.berterogarcia.appnea.ui.cuenta.CuentaScreen
import com.berterogarcia.appnea.ui.cuenta.CuentaViewModel
import com.berterogarcia.appnea.ui.detalleinmersion.DetalleInmersionScreen
import com.berterogarcia.appnea.ui.detalleinmersion.DetalleInmersionViewModel
import com.berterogarcia.appnea.ui.historial.HistorialViewModel
import com.berterogarcia.appnea.ui.informacion.InformacionScreen
import com.berterogarcia.appnea.ui.inicio.InicioScreen
import com.berterogarcia.appnea.ui.inmersion.InmersionScreen
import com.berterogarcia.appnea.ui.login.LoginScreen
import com.berterogarcia.appnea.ui.login.LoginViewModel
import com.berterogarcia.appnea.ui.nuevainmersion.NuevaInmersionScreen
import com.berterogarcia.appnea.ui.nuevainmersion.NuevaInmersionViewModel
import com.berterogarcia.appnea.ui.perfil.certificados.CertificadoFormScreen
import com.berterogarcia.appnea.ui.perfil.certificados.CertificadoFormViewModel
import com.berterogarcia.appnea.ui.perfil.certificados.CertificadosScreen
import com.berterogarcia.appnea.ui.perfil.certificados.CertificadosViewModel
import com.berterogarcia.appnea.ui.perfil.contactos.ContactoFormScreen
import com.berterogarcia.appnea.ui.perfil.contactos.ContactoFormViewModel
import com.berterogarcia.appnea.ui.perfil.contactos.ContactosScreen
import com.berterogarcia.appnea.ui.perfil.contactos.ContactosViewModel
import com.berterogarcia.appnea.ui.perfil.datos.DatosPersonalesScreen
import com.berterogarcia.appnea.ui.perfil.datos.DatosPersonalesViewModel
import com.berterogarcia.appnea.ui.perfil.equipamiento.EquipamientoScreen
import com.berterogarcia.appnea.ui.perfil.equipamiento.EquipamientoViewModel
import com.berterogarcia.appnea.ui.perfil.equipamiento.EquipoFormScreen
import com.berterogarcia.appnea.ui.perfil.equipamiento.EquipoFormViewModel
import com.berterogarcia.appnea.ui.pesca.NuevaSalidaPescaScreen
import com.berterogarcia.appnea.ui.pesca.NuevaSalidaPescaViewModel
import com.berterogarcia.appnea.ui.pesca.PescaSubmarinaViewModel
import com.berterogarcia.appnea.ui.registro.RegistroScreen
import com.berterogarcia.appnea.ui.registro.RegistroViewModel

//Estas son pantallas que no aparecen en la barra de la app.
const val RUTA_REGISTRO = "registro"
const val RUTA_LOGIN = "login"
const val RUTA_NUEVA_INMERSION = "nueva_inmersion"
const val RUTA_DETALLE_INMERSION = "detalle_inmersion/{id}"
const val RUTA_CONDICIONES = "condiciones"
const val RUTA_NUEVA_SALIDA_PESCA = "nueva_salida_pesca"

//  Perfil. Todas empiezan con "perfil/" para que la barra marque "Cuenta" como seleccionada.
const val PREFIJO_PERFIL = "perfil/"
const val RUTA_DATOS_PERSONALES = "perfil/datos"
const val RUTA_EQUIPAMIENTO = "perfil/equipamiento"
const val RUTA_FORM_EQUIPO = "perfil/equipamiento/form?id={id}"
const val RUTA_CERTIFICADOS = "perfil/certificados"
const val RUTA_FORM_CERTIFICADO = "perfil/certificados/form?id={id}"
const val RUTA_CONTACTOS = "perfil/contactos"
const val RUTA_FORM_CONTACTO = "perfil/contactos/form?id={id}"

//  Argumento opcional "id" de los formularios: -1 (o ausente) = alta, > 0 = edición.
private const val ARG_ID = "id"
private const val SIN_ID = -1L

fun rutaFormEquipo(id: Long? = null) = "perfil/equipamiento/form?id=${id ?: SIN_ID}"
fun rutaFormCertificado(id: Long? = null) = "perfil/certificados/form?id=${id ?: SIN_ID}"
fun rutaFormContacto(id: Long? = null) = "perfil/contactos/form?id=${id ?: SIN_ID}"

private fun argumentoIdOpcional(): List<NamedNavArgument> = listOf(
    navArgument(ARG_ID) {
        type = NavType.LongType
        defaultValue = SIN_ID
    }
)

private fun NavBackStackEntry.idOpcional(): Long? =
    arguments?.getLong(ARG_ID, SIN_ID)?.takeIf { it > 0 }

/**
 * Vuelve atrás solo si la pantalla que lo pide sigue ARRIBA de la pila. Evita que un
 * doble toque en "volver" (o volver + guardado al mismo tiempo) saque DOS pantallas.
 */
private fun NavHostController.volverDesde(entrada: NavBackStackEntry) {
    if (currentBackStackEntry?.id == entrada.id) popBackStack()
}

/** Formularios a pantalla completa: se oculta la barra de navegación (ver MainActivity). */
val RUTAS_SIN_BARRA = setOf(
    RUTA_NUEVA_INMERSION,
    RUTA_NUEVA_SALIDA_PESCA,
    RUTA_DATOS_PERSONALES,
    RUTA_FORM_EQUIPO,
    RUTA_FORM_CERTIFICADO,
    RUTA_FORM_CONTACTO
)

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String, //  antes era RUTA_LOGIN fijo; ahora lo decide SesionViewModel
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as MyApplication

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {

        //================================LOGIN==========================================
        composable(RUTA_LOGIN) {
            val viewModel: LoginViewModel = viewModel(
                factory = LoginViewModel.Factory(app.usuarioRepository, app.sessionManager, app.preferenciasLogin)
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
                factory = RegistroViewModel.Factory(app.registroRepository, app.sessionManager)
            )
            RegistroScreen(
                viewModel = viewModel,
                onCancelar = { navController.popBackStack() },
                // La sesión ya quedó iniciada: va a Inicio y limpia la pila (no se vuelve a login/registro).
                onRegistroExitoso = {
                    navController.navigate(AppDestinations.INICIO.name) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
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
        //==============================CONDICIONES==============================================
        composable(RUTA_CONDICIONES) { entrada ->
            // Reutiliza el mismo ViewModel de Inicio (así no vuelve a descargar todoo)
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
                onAgregarSalidaPesca = { navController.navigate(RUTA_NUEVA_SALIDA_PESCA) },
                onVerDetalleInmersion = { id -> navController.navigate("detalle_inmersion/$id") }
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
        composable(
            route = RUTA_DETALLE_INMERSION,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            val viewModel: DetalleInmersionViewModel = viewModel(
                factory = DetalleInmersionViewModel.Factory(app.inmersionRepository, id, app.applicationContext)
            )
            DetalleInmersionScreen(
                viewModel = viewModel,
                onEliminada = { navController.popBackStack() }
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
                    app.perfilRepository, // 
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
                },
                //  Accesos a cada sección del perfil
                onEditarDatos = { navController.navigate(RUTA_DATOS_PERSONALES) },
                onVerEquipamiento = { navController.navigate(RUTA_EQUIPAMIENTO) },
                onVerCertificados = { navController.navigate(RUTA_CERTIFICADOS) },
                onVerContactos = { navController.navigate(RUTA_CONTACTOS) }
            )
        }

        //=========================== PERFIL============================================
        // El usuario se lee de la sesión al crear cada ViewModel. Si por algún motivo es null,
        // las pantallas muestran "no hay sesión activa" y no dejan guardar (no crashean).

        composable(RUTA_DATOS_PERSONALES) { entrada ->
            val viewModel: DatosPersonalesViewModel = viewModel(
                factory = DatosPersonalesViewModel.Factory(app.perfilRepository, app.sessionManager.obtenerUsuario())
            )
            DatosPersonalesScreen(
                viewModel = viewModel,
                onVolver = { navController.volverDesde(entrada) }
            )
        }

        // ---------- Equipamiento ----------
        composable(RUTA_EQUIPAMIENTO) { entrada ->
            val viewModel: EquipamientoViewModel = viewModel(
                factory = EquipamientoViewModel.Factory(app.perfilRepository, app.sessionManager.obtenerUsuario())
            )
            EquipamientoScreen(
                viewModel = viewModel,
                onVolver = { navController.volverDesde(entrada) },
                onAgregar = { navController.navigate(rutaFormEquipo()) },
                onEditar = { id -> navController.navigate(rutaFormEquipo(id)) }
            )
        }
        composable(RUTA_FORM_EQUIPO, arguments = argumentoIdOpcional()) { entrada ->
            val viewModel: EquipoFormViewModel = viewModel(
                factory = EquipoFormViewModel.Factory(
                    app.perfilRepository, app.sessionManager.obtenerUsuario(), entrada.idOpcional()
                )
            )
            EquipoFormScreen(viewModel = viewModel, onVolver = { navController.volverDesde(entrada) })
        }

        // ---------- Certificados ----------
        composable(RUTA_CERTIFICADOS) { entrada ->
            val viewModel: CertificadosViewModel = viewModel(
                factory = CertificadosViewModel.Factory(app.perfilRepository, app.sessionManager.obtenerUsuario())
            )
            CertificadosScreen(
                viewModel = viewModel,
                onVolver = { navController.volverDesde(entrada) },
                onAgregar = { navController.navigate(rutaFormCertificado()) },
                onEditar = { id -> navController.navigate(rutaFormCertificado(id)) }
            )
        }
        composable(RUTA_FORM_CERTIFICADO, arguments = argumentoIdOpcional()) { entrada ->
            val viewModel: CertificadoFormViewModel = viewModel(
                factory = CertificadoFormViewModel.Factory(
                    app.perfilRepository, app.sessionManager.obtenerUsuario(), entrada.idOpcional()
                )
            )
            CertificadoFormScreen(viewModel = viewModel, onVolver = { navController.volverDesde(entrada) })
        }

        // ---------- Contactos de emergencia ----------
        composable(RUTA_CONTACTOS) { entrada ->
            val viewModel: ContactosViewModel = viewModel(
                factory = ContactosViewModel.Factory(app.perfilRepository, app.sessionManager.obtenerUsuario())
            )
            ContactosScreen(
                viewModel = viewModel,
                onVolver = { navController.volverDesde(entrada) },
                onAgregar = { navController.navigate(rutaFormContacto()) },
                onEditar = { id -> navController.navigate(rutaFormContacto(id)) }
            )
        }
        composable(RUTA_FORM_CONTACTO, arguments = argumentoIdOpcional()) { entrada ->
            val viewModel: ContactoFormViewModel = viewModel(
                factory = ContactoFormViewModel.Factory(
                    app.perfilRepository, app.sessionManager.obtenerUsuario(), entrada.idOpcional()
                )
            )
            ContactoFormScreen(viewModel = viewModel, onVolver = { navController.volverDesde(entrada) })
        }
    }
}
