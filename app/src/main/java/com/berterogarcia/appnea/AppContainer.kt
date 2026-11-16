package com.berterogarcia.appnea

import android.content.Context
import com.berterogarcia.appnea.data.local.AppDatabase
import com.berterogarcia.appnea.data.sesion.PreferenciasLogin
import com.berterogarcia.appnea.data.sesion.SessionManager
import com.berterogarcia.appnea.data.sonido.SonidoManager
import com.berterogarcia.appnea.data.ubicacion.UbicacionAndroid
import com.berterogarcia.appnea.data.repository.CuentaGoogleRepository
import com.berterogarcia.appnea.data.repository.InmersionRepository
import com.berterogarcia.appnea.data.repository.PerfilRepository
import com.berterogarcia.appnea.data.repository.RegistroRepository
import com.berterogarcia.appnea.data.repository.SesionApneaRepository
import com.berterogarcia.appnea.data.repository.SesionRepository
import com.berterogarcia.appnea.data.repository.TablaPersonalizadaRepository
import com.berterogarcia.appnea.data.repository.UsuarioRepository
import com.berterogarcia.appnea.data.trabajos.CompletarCondicionesWorker
import com.berterogarcia.appnea.dominio.RegistrarInmersion
import com.berterogarcia.appnea.dominio.ServicioUbicacion
import com.berterogarcia.appnea.entrenamiento.MotorEntrenamiento
import com.berterogarcia.appnea.entrenamiento.ServicioEntrenamiento
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Grafo de dependencias de la app (inyección manual): crea cada repositorio/servicio una sola
 * vez, de forma perezosa, y es el único lugar que sabe qué implementación concreta se usa.
 *
 * Las pantallas lo obtienen con `LocalContext.current.contenedor()` y construyen su ViewModel con
 * `viewModel { MiViewModel(app.repositorio) }`: no hacen falta clases Factory por ViewModel.
 */
class AppContainer(context: Context) {
    /** Context de la aplicación (nunca el de una Activity: el contenedor vive todo el proceso). */
    val contexto: Context = context.applicationContext


    val database by lazy { AppDatabase.getDatabase(contexto) }
    val sessionManager by lazy { SessionManager(contexto) }
    val sonidoManager by lazy { SonidoManager(contexto) }
    val usuarioRepository by lazy { UsuarioRepository(database.usuarioDao()) }
    val cuentaGoogleRepository by lazy { CuentaGoogleRepository(database) }
    // Estos repositorios reciben la sesión: trabajan siempre sobre el usuario logueado.
    val preferenciasLogin by lazy { PreferenciasLogin(contexto) }
    val inmersionRepository by lazy { InmersionRepository(database.inmersionDao(), sessionManager) }
    val tablaPersonalizadaRepository by lazy {
        TablaPersonalizadaRepository(database.tablaPersonalizadaDao(), sessionManager)
    }
    val sesionApneaRepository by lazy { SesionApneaRepository(database.sesionApneaDao(), sessionManager) }
    val registroRepository by lazy { RegistroRepository(database) }
    // GPS + geocoder detrás de una interfaz (los ViewModels no usan Context)
    val ubicacion: ServicioUbicacion by lazy { UbicacionAndroid(contexto) }
    // Guardar inmersión sin esperar a internet: clima y mar los completa WorkManager después
    val registrarInmersion by lazy {
        RegistrarInmersion(inmersionRepository, CompletarCondicionesWorker.Programador(contexto))
    }

    // [NUEVO] Perfil (datos personales, equipamiento, certificados, contactos)
    val perfilRepository by lazy {
        PerfilRepository(
            perfilDao = database.perfilUsuarioDao(),
            equipoDao = database.equipoDao(),
            certificadoDao = database.certificadoDao(),
            contactoDao = database.contactoEmergenciaDao(),
            sesion = sessionManager
        )
    }

    // [NUEVO] Valida la sesión guardada al abrir la app
    val sesionRepository by lazy { SesionRepository(sessionManager, database.usuarioDao()) }

    // Timer de las tablas de apnea: vive con el proceso (no con la pantalla). Mientras corre,
    // ServicioEntrenamiento mantiene la app viva con la pantalla bloqueada.
    val motorEntrenamiento by lazy {
        MotorEntrenamiento(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            sonido = sonidoManager,
            alCambiarActividad = { corriendo -> ServicioEntrenamiento.actualizar(contexto, corriendo) }
        )
    }
}

/** Acceso al contenedor desde cualquier Context de la app. */
fun Context.contenedor(): AppContainer = (applicationContext as MyApplication).container
