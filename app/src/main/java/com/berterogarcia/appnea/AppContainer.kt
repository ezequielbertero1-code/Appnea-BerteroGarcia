package com.berterogarcia.appnea

import com.berterogarcia.appnea.data.condiciones.CondicionesRepository
import com.berterogarcia.appnea.dominio.FuenteCondiciones
import com.berterogarcia.appnea.data.export.ExportadorUddf
import com.berterogarcia.appnea.dominio.ExportadorInmersiones
import com.berterogarcia.appnea.dominio.GestorSesion
import com.berterogarcia.appnea.dominio.PreferenciasLogin
import com.berterogarcia.appnea.dominio.repositorios.RepositorioUsuarios
import com.berterogarcia.appnea.dominio.repositorios.RepositorioCuentaGoogle
import com.berterogarcia.appnea.dominio.repositorios.RepositorioInmersiones
import com.berterogarcia.appnea.dominio.repositorios.RepositorioTablas
import com.berterogarcia.appnea.dominio.repositorios.RepositorioSesionesApnea
import com.berterogarcia.appnea.dominio.repositorios.RepositorioIntentos
import com.berterogarcia.appnea.dominio.repositorios.RepositorioRegistro
import com.berterogarcia.appnea.dominio.repositorios.RepositorioPerfil
import com.berterogarcia.appnea.dominio.repositorios.RepositorioSesion
import android.content.Context
import com.berterogarcia.appnea.data.local.AppDatabase
import com.berterogarcia.appnea.data.sesion.PreferenciasLoginAndroid
import com.berterogarcia.appnea.data.sesion.SessionManager
import com.berterogarcia.appnea.data.sonido.SonidoManager
import com.berterogarcia.appnea.data.ubicacion.UbicacionAndroid
import com.berterogarcia.appnea.data.repository.CuentaGoogleRepository
import com.berterogarcia.appnea.data.repository.InmersionRepository
import com.berterogarcia.appnea.data.repository.PerfilRepository
import com.berterogarcia.appnea.data.repository.RegistroRepository
import com.berterogarcia.appnea.data.repository.SesionApneaRepository
import com.berterogarcia.appnea.data.repository.IntentoApneaRepository
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
    val sessionManager: GestorSesion by lazy { SessionManager(contexto) }
    val sonidoManager by lazy { SonidoManager(contexto) }
    val usuarioRepository: RepositorioUsuarios by lazy { UsuarioRepository(database.usuarioDao()) }
    val cuentaGoogleRepository: RepositorioCuentaGoogle by lazy { CuentaGoogleRepository(database) }
    // Estos repositorios reciben la sesión: trabajan siempre sobre el usuario logueado.
    val preferenciasLogin: PreferenciasLogin by lazy { PreferenciasLoginAndroid(contexto) }
    val inmersionRepository: RepositorioInmersiones by lazy { InmersionRepository(database.inmersionDao(), sessionManager) }
    val tablaPersonalizadaRepository: RepositorioTablas by lazy {
        TablaPersonalizadaRepository(database.tablaPersonalizadaDao(), sessionManager)
    }
    val sesionApneaRepository: RepositorioSesionesApnea by lazy { SesionApneaRepository(database.sesionApneaDao(), sessionManager) }
    val intentoApneaRepository: RepositorioIntentos by lazy { IntentoApneaRepository(database.intentoApneaDao(), sessionManager) }
    val registroRepository: RepositorioRegistro by lazy { RegistroRepository(database) }
    // Clima, mar y astronomía (Open-Meteo + cálculos propios)
    val condiciones: FuenteCondiciones = CondicionesRepository
    // Exportar UDDF: el ViewModel no usa Context ni Uri
    val exportador: ExportadorInmersiones by lazy { ExportadorUddf(contexto, sessionManager) }
    // GPS + geocoder detrás de una interfaz (los ViewModels no usan Context)
    val ubicacion: ServicioUbicacion by lazy { UbicacionAndroid(contexto) }
    // Guardar inmersión sin esperar a internet: clima y mar los completa WorkManager después
    val registrarInmersion by lazy {
        RegistrarInmersion(inmersionRepository, condiciones, CompletarCondicionesWorker.Programador(contexto))
    }

    // Perfil (datos personales, equipamiento, certificados, contactos)
    val perfilRepository: RepositorioPerfil by lazy {
        PerfilRepository(
            perfilDao = database.perfilUsuarioDao(),
            equipoDao = database.equipoDao(),
            certificadoDao = database.certificadoDao(),
            contactoDao = database.contactoEmergenciaDao(),
            sesion = sessionManager
        )
    }

    // Valida la sesión guardada al abrir la app
    val sesionRepository: RepositorioSesion by lazy { SesionRepository(sessionManager, database.usuarioDao()) }

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
