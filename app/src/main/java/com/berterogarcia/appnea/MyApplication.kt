package com.berterogarcia.appnea

import android.app.Application
import com.berterogarcia.appnea.data.local.AppDatabase
import com.berterogarcia.appnea.data.local.SessionManager
import com.berterogarcia.appnea.data.local.SonidoManager
import com.berterogarcia.appnea.data.repository.InmersionRepository
import com.berterogarcia.appnea.data.repository.SesionApneaRepository
import com.berterogarcia.appnea.data.repository.TablaPersonalizadaRepository
import com.berterogarcia.appnea.data.repository.UsuarioRepository

class MyApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val sessionManager by lazy { SessionManager(this) }
    val sonidoManager by lazy { SonidoManager(this) }
    val usuarioRepository by lazy { UsuarioRepository(database.usuarioDao()) }
    val inmersionRepository by lazy { InmersionRepository(database.inmersionDao()) }
    val tablaPersonalizadaRepository by lazy { TablaPersonalizadaRepository(database.tablaPersonalizadaDao()) }
    val sesionApneaRepository by lazy { SesionApneaRepository(database.sesionApneaDao()) }
}
