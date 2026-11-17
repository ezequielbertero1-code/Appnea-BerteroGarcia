package com.berterogarcia.appnea.navigation

import com.berterogarcia.appnea.data.auth.GoogleAuth
import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.createSavedStateHandle
import kotlinx.coroutines.flow.map
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.berterogarcia.appnea.AppDestinations
import com.berterogarcia.appnea.dominio.modelo.CuentaGoogle
import com.berterogarcia.appnea.contenedor
import com.berterogarcia.appnea.ui.clima.ClimaViewModel
import com.berterogarcia.appnea.ui.condiciones.CondicionesScreen
import com.berterogarcia.appnea.ui.cuenta.CuentaScreen
import com.berterogarcia.appnea.ui.cuenta.CuentaViewModel
import com.berterogarcia.appnea.ui.detalleinmersion.DetalleInmersionScreen
import com.berterogarcia.appnea.ui.detalleinmersion.DetalleInmersionViewModel
import com.berterogarcia.appnea.ui.entrenamiento.CrearTablaScreen
import com.berterogarcia.appnea.ui.entrenamiento.CrearTablaViewModel
import com.berterogarcia.appnea.ui.entrenamiento.CronometroScreen
import com.berterogarcia.appnea.ui.entrenamiento.CronometroViewModel
import com.berterogarcia.appnea.ui.entrenamiento.EntrenamientoApneaViewModel
import com.berterogarcia.appnea.ui.estadisticas.EstadisticasScreen
import com.berterogarcia.appnea.ui.estadisticas.EstadisticasViewModel
import com.berterogarcia.appnea.ui.estadisticas.PESTANIA_ENTRENAMIENTO
import com.berterogarcia.appnea.ui.historial.HistorialViewModel
import com.berterogarcia.appnea.ui.informacion.InformacionScreen
import com.berterogarcia.appnea.ui.inicio.InicioScreen
import com.berterogarcia.appnea.ui.inicio.InicioViewModel
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
import com.berterogarcia.appnea.ui.registro.RegistroScreen
import com.berterogarcia.appnea.ui.registro.RegistroViewModel

//Estas son pantallas que no aparecen en la barra de la app.
const val RUTA_REGISTRO = "registro"
/** Registro de una cuenta de Google que entra por primera vez (empieza en Datos personales, ya cargados). */
const val RUTA_REGISTRO_GOOGLE = "registro/google?email={email}&nombre={nombre}&apellido={apellido}"
fun rutaRegistroGoogle(cuenta: CuentaGoogle) =
    "registro/google?email=${Uri.encode(cuenta.email)}&nombre=${Uri.encode(cuenta.nombre)}&apellido=${Uri.encode(cuenta.apellido)}"
const val RUTA_LOGIN = "login"
const val RUTA_NUEVA_INMERSION = "nueva_inmersion"
const val RUTA_DETALLE_INMERSION = "detalle_inmersion/{id}"
const val RUTA_EDITAR_INMERSION = "editar_inmersion/{id}"
fun rutaEditarInmersion(id: Long) = "editar_inmersion/$id"
const val RUTA_CONDICIONES = "condiciones"
const val RUTA_CREAR_TABLA = "entrenamiento/crear_tabla"
const val RUTA_ESTADISTICAS = "estadisticas?pestania={pestania}"
private const val ARG_PESTANIA = "pestania"
/** pestania: 0 = Inmersiones, 1 = Entrenamiento (PESTANIA_ENTRENAMIENTO). */
fun rutaEstadisticas(pestania: Int = 0) = "estadisticas?pestania=$pestania"

//  Perfil. Todas empiezan con "perfil/" para que la barra marque "Cuenta" como seleccionada.
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
    RUTA_EDITAR_INMERSION,
    RUTA_CREAR_TABLA,
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

        //================================LOGIN==========================================
        composable(RUTA_LOGIN) {
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
                onLoginExitoso = {
                    navController.navigate(AppDestinations.INICIO.name) {
                        popUpTo(RUTA_LOGIN) { inclusive = true } // Esto hace que una vez iniciado sesion no pueda volver al login
                    }
                },
                onRegistroClick = { navController.navigate(RUTA_REGISTRO) },
                onRegistroGoogle = { cuenta -> navController.navigate(rutaRegistroGoogle(cuenta)) },
                pedirCuentaGoogle = GoogleAuth::pedirCuenta
            )
        }
        composable(RUTA_REGISTRO) {
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
                // La sesión ya quedó iniciada: va a Inicio y limpia la pila (no se vuelve a login/registro).
                onRegistroExitoso = {
                    navController.navigate(AppDestinations.INICIO.name) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        //================================INICIO================================================
        // Registrar inmersión primero, últimas inmersiones y resumen del clima.
        composable(
            route = RUTA_REGISTRO_GOOGLE,
            arguments = listOf("email", "nombre", "apellido").map {
                navArgument(it) { type = NavType.StringType; defaultValue = "" }
            }
        ) { entrada ->
            val cuenta = CuentaGoogle(
                email = entrada.arguments?.getString("email").orEmpty(),
                nombre = entrada.arguments?.getString("nombre").orEmpty(),
                apellido = entrada.arguments?.getString("apellido").orEmpty()
            )
            val viewModel: RegistroViewModel = viewModel {
                RegistroViewModel(
                    app.registroRepository,
                    app.cuentaGoogleRepository,
                    app.sessionManager,
                    cuenta,
                    createSavedStateHandle()
                )
            }
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
        // Registrar inmersión primero, últimas inmersiones y resumen del clima.
        composable(AppDestinations.INICIO.name) {
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
                // launchSingleTop: un doble toque no abre el formulario dos veces
                onNuevaInmersion = { navController.navigate(RUTA_NUEVA_INMERSION) { launchSingleTop = true } },
                onVerDetalleInmersion = { id -> navController.navigate("detalle_inmersion/$id") { launchSingleTop = true } },
                onVerCondiciones = { navController.navigate(RUTA_CONDICIONES) { launchSingleTop = true } },
                onIrA = { destino -> navController.irAPestania(destino) }
            )
        }
        //==============================CONDICIONES==============================================
        composable(RUTA_CONDICIONES) { entrada ->
            // Reutiliza el mismo ViewModel de Inicio (así no vuelve a descargar todoo)
            val duenio = remember(entrada) {
                runCatching { navController.getBackStackEntry(AppDestinations.INICIO.name) }.getOrNull()
            } ?: entrada
            val climaViewModel: ClimaViewModel = viewModel(viewModelStoreOwner = duenio) {
                ClimaViewModel(
                    app.condiciones,
                    app.ubicacion
                )
            }
            CondicionesScreen(viewModel = climaViewModel, onVolver = { navController.volverDesde(entrada) })
        }
        //==============================ENTRENAMIENTO (antes Cronometro)=========================
        composable(AppDestinations.ENTRENAMIENTO.name) {
            val entrenamientoViewModel: EntrenamientoApneaViewModel = viewModel {
                EntrenamientoApneaViewModel(
                    app.tablaPersonalizadaRepository,
                    app.sesionApneaRepository,
                    app.motorEntrenamiento,
                    createSavedStateHandle()
                )
            }
            val cronometroViewModel: CronometroViewModel = viewModel { CronometroViewModel(app.intentoApneaRepository) }
            CronometroScreen(
                entrenamientoViewModel = entrenamientoViewModel,
                cronometroViewModel = cronometroViewModel,
                onCrearTabla = { navController.navigate(RUTA_CREAR_TABLA) { launchSingleTop = true } },
                onVerEstadisticas = {
                    navController.navigate(rutaEstadisticas(PESTANIA_ENTRENAMIENTO)) { launchSingleTop = true }
                }
            )
        }
        composable(RUTA_CREAR_TABLA) { entrada ->
            val crearTablaViewModel: CrearTablaViewModel = viewModel { CrearTablaViewModel(app.tablaPersonalizadaRepository) }
            CrearTablaScreen(
                viewModel = crearTablaViewModel,
                onCerrar = { navController.volverDesde(entrada) }
            )
        }

        //==============================INMERSION (buceo y apnea)=============================
        composable(AppDestinations.INMERSION.name) {
            val historialViewModel: HistorialViewModel = viewModel { HistorialViewModel(app.inmersionRepository) }
            InmersionScreen(
                historialViewModel = historialViewModel,
                onAgregarInmersion = { navController.navigate(RUTA_NUEVA_INMERSION) { launchSingleTop = true } },
                onVerDetalleInmersion = { id -> navController.navigate("detalle_inmersion/$id") { launchSingleTop = true } },
                onVerEstadisticas = { navController.navigate(rutaEstadisticas()) { launchSingleTop = true } }
            )
        }
        composable(RUTA_NUEVA_INMERSION) {
            val viewModel: NuevaInmersionViewModel = viewModel {
                NuevaInmersionViewModel(
                    app.inmersionRepository,
                    app.registrarInmersion,
                    app.perfilRepository,
                    app.ubicacion,
                    idEditar = null,
                    createSavedStateHandle()
                )
            }
            NuevaInmersionScreen(
                viewModel = viewModel,
                onGuardado = { navController.popBackStack() },
                // Abre el alta de equipo; al guardarlo vuelve acá y la lista se actualiza sola
                onCargarEquipo = { navController.navigate(rutaFormEquipo()) { launchSingleTop = true } }
            )
        }
        // Editar: el mismo formulario, cargado con la inmersión guardada
        composable(
            route = RUTA_EDITAR_INMERSION,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entrada ->
            val id = entrada.arguments?.getLong("id") ?: 0L
            val viewModel: NuevaInmersionViewModel = viewModel {
                NuevaInmersionViewModel(
                    app.inmersionRepository,
                    app.registrarInmersion,
                    app.perfilRepository,
                    app.ubicacion,
                    id,
                    createSavedStateHandle()
                )
            }
            NuevaInmersionScreen(
                viewModel = viewModel,
                onGuardado = { navController.volverDesde(entrada) }, // vuelve al detalle, que se actualiza solo
                onCargarEquipo = { navController.navigate(rutaFormEquipo()) { launchSingleTop = true } }
            )
        }
        composable(
            route = RUTA_DETALLE_INMERSION,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            val viewModel: DetalleInmersionViewModel = viewModel {
                DetalleInmersionViewModel(
                    app.inmersionRepository,
                    id,
                    app.exportador
                )
            }
            DetalleInmersionScreen(
                viewModel = viewModel,
                onEliminada = { navController.popBackStack() },
                onEditar = { id -> navController.navigate(rutaEditarInmersion(id)) { launchSingleTop = true } },
                onVolver = { navController.volverDesde(backStackEntry) }
            )
        }

        //==================================ESTADÍSTICAS=====================================
        composable(
            route = RUTA_ESTADISTICAS,
            arguments = listOf(navArgument(ARG_PESTANIA) {
                type = NavType.IntType
                defaultValue = 0
            })
        ) { entrada ->
            val estadisticasViewModel: EstadisticasViewModel = viewModel {
                EstadisticasViewModel(
                    app.inmersionRepository,
                    app.sesionApneaRepository,
                    app.intentoApneaRepository
                )
            }
            EstadisticasScreen(
                viewModel = estadisticasViewModel,
                pestaniaInicial = entrada.arguments?.getInt(ARG_PESTANIA) ?: 0,
                onVolver = { navController.volverDesde(entrada) },
                onVerInmersion = { id -> navController.navigate("detalle_inmersion/$id") { launchSingleTop = true } },
                onRegistrarInmersion = { navController.navigate(RUTA_NUEVA_INMERSION) { launchSingleTop = true } },
                onIrAEntrenar = { navController.irAPestania(AppDestinations.ENTRENAMIENTO) }
            )
        }

        //=============================APRENDER (antes Informacion)==========================
        composable(AppDestinations.APRENDER.name) {
            InformacionScreen()
        }

        //===========================CUENTA-USUARIO============================================
        composable(AppDestinations.CUENTA.name) {
            val viewModel: CuentaViewModel = viewModel {
                CuentaViewModel(
                    app.sessionManager,
                    app.perfilRepository,
                    app.inmersionRepository,
                    app.sesionApneaRepository,
                    app.motorEntrenamiento.estado.map { it.corriendo }
                )
            }
            CuentaScreen(
                viewModel = viewModel,
                onCerrarSesion = { navController.irAlLoginSinEstadoGuardado() },
                //  Accesos a cada sección del perfil
                onEditarDatos = { navController.navigate(RUTA_DATOS_PERSONALES) },
                onVerEquipamiento = { navController.navigate(RUTA_EQUIPAMIENTO) },
                onVerCertificados = { navController.navigate(RUTA_CERTIFICADOS) },
                onVerContactos = { navController.navigate(RUTA_CONTACTOS) },
                onVerEstadisticas = { navController.navigate(rutaEstadisticas()) { launchSingleTop = true } }
            )
        }

        //=========================== PERFIL============================================
        // El usuario se lee de la sesión al crear cada ViewModel. Si por algún motivo es null,
        // las pantallas muestran "no hay sesión activa" y no dejan guardar (no crashean).

        composable(RUTA_DATOS_PERSONALES) { entrada ->
            val viewModel: DatosPersonalesViewModel = viewModel { DatosPersonalesViewModel(app.perfilRepository) }
            DatosPersonalesScreen(
                viewModel = viewModel,
                onVolver = { navController.volverDesde(entrada) }
            )
        }

        // ---------- Equipamiento ----------
        composable(RUTA_EQUIPAMIENTO) { entrada ->
            val viewModel: EquipamientoViewModel = viewModel { EquipamientoViewModel(app.perfilRepository) }
            EquipamientoScreen(
                viewModel = viewModel,
                onVolver = { navController.volverDesde(entrada) },
                onAgregar = { navController.navigate(rutaFormEquipo()) },
                onEditar = { id -> navController.navigate(rutaFormEquipo(id)) }
            )
        }
        composable(RUTA_FORM_EQUIPO, arguments = argumentoIdOpcional()) { entrada ->
            val viewModel: EquipoFormViewModel = viewModel {
                EquipoFormViewModel(
                    app.perfilRepository,
                    entrada.idOpcional()
                )
            }
            EquipoFormScreen(viewModel = viewModel, onVolver = { navController.volverDesde(entrada) })
        }

        // ---------- Certificados ----------
        composable(RUTA_CERTIFICADOS) { entrada ->
            val viewModel: CertificadosViewModel = viewModel { CertificadosViewModel(app.perfilRepository) }
            CertificadosScreen(
                viewModel = viewModel,
                onVolver = { navController.volverDesde(entrada) },
                onAgregar = { navController.navigate(rutaFormCertificado()) },
                onEditar = { id -> navController.navigate(rutaFormCertificado(id)) }
            )
        }
        composable(RUTA_FORM_CERTIFICADO, arguments = argumentoIdOpcional()) { entrada ->
            val viewModel: CertificadoFormViewModel = viewModel {
                CertificadoFormViewModel(
                    app.perfilRepository,
                    entrada.idOpcional()
                )
            }
            CertificadoFormScreen(viewModel = viewModel, onVolver = { navController.volverDesde(entrada) })
        }

        // ---------- Contactos de emergencia ----------
        composable(RUTA_CONTACTOS) { entrada ->
            val viewModel: ContactosViewModel = viewModel { ContactosViewModel(app.perfilRepository) }
            ContactosScreen(
                viewModel = viewModel,
                onVolver = { navController.volverDesde(entrada) },
                onAgregar = { navController.navigate(rutaFormContacto()) },
                onEditar = { id -> navController.navigate(rutaFormContacto(id)) }
            )
        }
        composable(RUTA_FORM_CONTACTO, arguments = argumentoIdOpcional()) { entrada ->
            val viewModel: ContactoFormViewModel = viewModel {
                ContactoFormViewModel(
                    app.perfilRepository,
                    entrada.idOpcional()
                )
            }
            ContactoFormScreen(viewModel = viewModel, onVolver = { navController.volverDesde(entrada) })
        }
    }
}
/**
 * Pestaña a la que pertenece la pantalla actual: la raíz de pestaña (además de Inicio) que
 * esté en la pila; si no hay ninguna, Inicio. Ej.: detalle abierto desde Inmersión → INMERSION;
 * detalle o Condiciones abiertos desde Inicio → INICIO; perfil/... → CUENTA.
 */
fun NavHostController.pestaniaAbierta(): AppDestinations =
    AppDestinations.entries
        .firstOrNull { it != AppDestinations.INICIO && estaEnLaPila(it.name) }
        ?: AppDestinations.INICIO
/**
 * Navegación entre pestañas (barra inferior y accesos de Inicio).
 *  - Tocar la pestaña en la que ya estás (o una pantalla abierta desde ella, ej. un detalle)
 *    te devuelve a su pantalla principal.
 *  - Cambiar de pestaña guarda la que dejás y restaura la que abrís (scroll, detalle abierto…).
 *  - Inicio siempre vuelve "limpio": nunca restaura lo que estaba abierto arriba suyo.
 */
fun NavHostController.irAPestania(destino: AppDestinations) {
    val inicio = AppDestinations.INICIO.name

    if (destino == pestaniaAbierta()) {
        // Re-seleccionar la pestaña actual: cierra lo que haya arriba de su raíz
        popBackStack(destino.name, inclusive = false)
        return
    }

    if (!estaEnLaPila(inicio)) {
        // No debería pasar (Inicio es siempre la raíz después del login), pero si pasa,
        // se arranca una pila nueva en vez de dejarla rota.
        navigate(destino.name) { popUpTo(graph.id) { inclusive = true } }
        return
    }

    navigate(destino.name) {
        // Se usa INICIO como raíz y no graph.findStartDestination(): si entraste por el login,
        // el destino inicial del grafo es "login", que ya no está en la pila.
        popUpTo(inicio) { saveState = true }
        launchSingleTop = true
        restoreState = destino != AppDestinations.INICIO
    }
}

/**
 * Cierre de sesión: vuelve al login descartando TODO lo de la cuenta anterior.
 *
 * irAPestania guarda las pestañas que dejás (saveState = true), y ese estado guardado incluye
 * sus ViewModels. popUpTo(graph.id) solo saca la pila actual, no lo guardado: al entrar con otra
 * cuenta se restauraban los ViewModels viejos (el cronómetro seguía corriendo y la bitácora
 * mostraba un instante las inmersiones del usuario anterior). clearBackStack borra ese estado
 * y limpia sus ViewModels (onCleared cancela sus corrutinas).
 */
fun NavHostController.irAlLoginSinEstadoGuardado() {
    AppDestinations.entries.forEach { clearBackStack(it.name) }
    navigate(RUTA_LOGIN) {
        popUpTo(graph.id) { inclusive = true }
    }
}

/** getBackStackEntry tira excepción si la ruta no está en la pila; acá se convierte en false. */
private fun NavHostController.estaEnLaPila(ruta: String): Boolean =
    runCatching { getBackStackEntry(ruta) }.isSuccess
