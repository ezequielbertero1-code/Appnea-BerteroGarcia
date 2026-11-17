package com.berterogarcia.appnea

import android.app.Application
import com.berterogarcia.appnea.data.local.SeedDatos
import com.berterogarcia.appnea.util.intentar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/** Punto de entrada del proceso: crea el [AppContainer] y lanza las tareas de arranque. */
class AppneaApp : Application() {

    val container by lazy { AppContainer(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            // v8: se sacó pesca submarina → se borran las fotos de capturas que hayan quedado.
            // Si la carpeta no existe no hace nada; si falla, no afecta al resto de la app.
            intentar("AppneaApp") { java.io.File(filesDir, "capturas_pesca").deleteRecursively() }
        }
        // Al cambiar de cuenta, una tabla en curso o sin guardar no puede pasar a la cuenta nueva
        appScope.launch {
            container.sessionManager.usuarioActual.drop(1).collect { container.motorEntrenamiento.reiniciar() }
        }
        // Datos de ejemplo (Franco y Demo) solo en debug; en release SeedDatos no hace nada.
        appScope.launch {
            intentar("AppneaApp") { SeedDatos.sembrarSiHaceFalta(this@AppneaApp, container.database) }
        }
    }
}
