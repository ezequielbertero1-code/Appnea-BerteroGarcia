package com.berterogarcia.appnea.data.local

import android.content.Context
import androidx.core.content.edit

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("sesion", Context.MODE_PRIVATE)

    fun guardarUsuario(nombreUsuario: String) {
        prefs.edit { putString("usuario_actual", nombreUsuario) }
    }

    fun obtenerUsuario(): String? = prefs.getString("usuario_actual", null)

    fun cerrarSesion() {
        prefs.edit { remove("usuario_actual") }
    }
}