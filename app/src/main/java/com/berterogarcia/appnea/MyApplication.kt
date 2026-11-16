package com.berterogarcia.appnea

import com.berterogarcia.appnea.util.intentar
import android.app.Application
import com.berterogarcia.appnea.data.local.AppDatabase
import com.berterogarcia.appnea.data.local.PreferenciasLogin
import com.berterogarcia.appnea.data.local.SeedDatos
import com.berterogarcia.appnea.data.local.SessionManager
import com.berterogarcia.appnea.data.local.SonidoManager
import com.berterogarcia.appnea.data.repository.CuentaGoogleRepository
import com.berterogarcia.appnea.data.trabajos.CompletarCondicionesWorker
import com.berterogarcia.appnea.dominio.RegistrarInmersion
import com.berterogarcia.appnea.data.repository.InmersionRepository
import com.berterogarcia.appnea.data.repository.PerfilRepository
import com.berterogarcia.appnea.data.repository.RegistroRepository
import com.berterogarcia.appnea.data.repository.SesionApneaRepository
import com.berterogarcia.appnea.data.repository.SesionRepository
import com.berterogarcia.appnea.data.repository.TablaPersonalizadaRepository
import com.berterogarcia.appnea.data.repository.UsuarioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import com.berterogarcia.appnea.entrenamiento.MotorEntrenamiento
import com.berterogarcia.appnea.entrenamiento.ServicioEntrenamiento

class MyApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val sessionManager by lazy { SessionManager(this) }
    val sonidoManager by lazy { SonidoManager(this) }
    val usuarioRepository by lazy { UsuarioRepository(database.usuarioDao()) }
    val cuentaGoogleRepository by lazy { CuentaGoogleRepository(database) }
    // Estos repositorios reciben la sesión: trabajan siempre sobre el usuario logueado.
    val preferenciasLogin by lazy { PreferenciasLogin(this) }
    val inmersionRepository by lazy { InmersionRepository(database.inmersionDao(), sessionManager) }
    val tablaPersonalizadaRepository by lazy {
        TablaPersonalizadaRepository(database.tablaPersonalizadaDao(), sessionManager)
    }
    val sesionApneaRepository by lazy { SesionApneaRepository(database.sesionApneaDao(), sessionManager) }
    val registroRepository by lazy { RegistroRepository(database) }
    // Guardar inmersión sin esperar a internet: clima y mar los completa WorkManager después
    val registrarInmersion by lazy {
        RegistrarInmersion(inmersionRepository, CompletarCondicionesWorker.Programador(this))
    }

    // [NUEVO] Perfil (datos personales, equipamiento, certificados, contactos)
    val perfilRepository by lazy {
        PerfilRepository(
            perfilDao = database.perfilUsuarioDao(),
            equipoDao = database.equipoDao(),
            certificadoDao = database.certificadoDao(),
            contactoDao = database.contactoEmergenciaDao()
        )
    }

    // [NUEVO] Valida la sesión guardada al abrir la app
    val sesionRepository by lazy { SesionRepository(sessionManager, database.usuarioDao()) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Timer de las tablas de apnea: vive con el proceso (no con la pantalla). Mientras corre,
    // ServicioEntrenamiento mantiene la app viva con la pantalla bloqueada.
    val motorEntrenamiento by lazy {
        MotorEntrenamiento(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            sonido = sonidoManager,
            alCambiarActividad = { corriendo -> ServicioEntrenamiento.actualizar(this, corriendo) }
        )
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            // v8: se sacó pesca submarina → se borran las fotos de capturas que hayan quedado.
            // Si la carpeta no existe no hace nada; si falla, no afecta al resto de la app.
            intentar("MyApplication") { java.io.File(filesDir, "capturas_pesca").deleteRecursively() }
        }
        // Al cambiar de cuenta, una tabla en curso o sin guardar no puede pasar a la cuenta nueva
        appScope.launch {
            sessionManager.usuarioActual.drop(1).collect { motorEntrenamiento.reiniciar() }
        }
        // Carga datos de ejemplo para el usuario de prueba "Franco" (solo la primera vez).
        appScope.launch {
            intentar("MyApplication") { SeedDatos.sembrarSiHaceFalta(this@MyApplication, database) }
        }
    }
}
