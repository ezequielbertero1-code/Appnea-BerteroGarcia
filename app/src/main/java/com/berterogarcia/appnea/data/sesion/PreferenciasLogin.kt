package com.berterogarcia.appnea.data.sesion

import com.berterogarcia.appnea.data.seguridad.EstadoIntentos
import android.content.Context
import androidx.core.content.edit

/**
 * Datos del login que se guardan en el dispositivo (SharedPreferences):
 *  - el último usuario que entró, para completar el campo solo;
 *  - los intentos fallidos, para que el bloqueo siga aunque se cierre la app.
 */
class PreferenciasLogin(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var ultimoUsuario: String?
        get() = prefs.getString(CLAVE_ULTIMO_USUARIO, null)?.takeIf { it.isNotBlank() }
        set(valor) = prefs.edit {
            if (valor.isNullOrBlank()) remove(CLAVE_ULTIMO_USUARIO) else putString(CLAVE_ULTIMO_USUARIO, valor)
        }

    fun leerIntentos(): EstadoIntentos = EstadoIntentos(
        fallidos = prefs.getInt(CLAVE_FALLIDOS, 0),
        bloqueos = prefs.getInt(CLAVE_BLOQUEOS, 0),
        bloqueadoHastaMs = prefs.getLong(CLAVE_BLOQUEADO_HASTA, 0L)
    )

    fun guardarIntentos(estado: EstadoIntentos) = prefs.edit {
        putInt(CLAVE_FALLIDOS, estado.fallidos)
        putInt(CLAVE_BLOQUEOS, estado.bloqueos)
        putLong(CLAVE_BLOQUEADO_HASTA, estado.bloqueadoHastaMs)
    }

    private companion object {
        const val PREFS = "login"
        const val CLAVE_ULTIMO_USUARIO = "ultimo_usuario"
        const val CLAVE_FALLIDOS = "intentos_fallidos"
        const val CLAVE_BLOQUEOS = "bloqueos"
        const val CLAVE_BLOQUEADO_HASTA = "bloqueado_hasta"
    }
}