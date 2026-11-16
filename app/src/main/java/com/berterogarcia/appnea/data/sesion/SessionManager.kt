package com.berterogarcia.appnea.data.sesion

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sesión del usuario: se guarda en SharedPreferences (persiste al cerrar la app) y además
 * se expone como StateFlow para que los repositorios reaccionen al login/logout.
 *
 * CAMBIO Ahora implementa [ProveedorUsuario]. La API anterior (guardarUsuario,
 * obtenerUsuario, cerrarSesion) sigue igual, así que el resto del código no cambia.
 */
class SessionManager(context: Context) : ProveedorUsuario {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _usuarioActual = MutableStateFlow(prefs.getString(CLAVE_USUARIO, null))
    override val usuarioActual: StateFlow<String?> = _usuarioActual.asStateFlow()

    fun guardarUsuario(nombreUsuario: String) {
        prefs.edit { putString(CLAVE_USUARIO, nombreUsuario) }
        _usuarioActual.value = nombreUsuario
    }

    fun obtenerUsuario(): String? = _usuarioActual.value

    fun cerrarSesion() {
        prefs.edit { remove(CLAVE_USUARIO) }
        _usuarioActual.value = null
    }

    companion object {
        private const val PREFS = "sesion"
        private const val CLAVE_USUARIO = "usuario_actual"

        /**
         * Lee el usuario guardado sin crear un SessionManager. Lo usa la migración de la base
         * (que corre antes de que exista el resto de la app) para saber de quién son los datos viejos.
         */
        fun leerUsuarioGuardado(context: Context): String? =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CLAVE_USUARIO, null)
    }
}
