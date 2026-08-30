package com.berterogarcia.appnea

import android.app.Application
import com.berterogarcia.appnea.data.local.AppDatabase
import com.berterogarcia.appnea.data.local.SessionManager
import com.berterogarcia.appnea.data.repository.InmersionRepository
import com.berterogarcia.appnea.data.repository.UsuarioRepository

class MyApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val sessionManager by lazy { SessionManager(this) }
    val usuarioRepository by lazy { UsuarioRepository(database.usuarioDao()) }
    val inmersionRepository by lazy { InmersionRepository(database.inmersionDao()) }
}