package com.berterogarcia.appnea.dominio

import kotlinx.coroutines.flow.StateFlow

/**
 * Quién está logueado ahora. Lo implementa SessionManager.
 *
 * Los repositorios dependen de esta interfaz (y no de SessionManager directamente) para
 * poder testearlos con un usuario falso, sin Android ni SharedPreferences.
 */
interface ProveedorUsuario {
    /** Usuario logueado, o null si no hay sesión. Emite de nuevo al iniciar/cerrar sesión. */
    val usuarioActual: StateFlow<String?>
}

/** Abrir y cerrar la sesión en este teléfono. */
interface GestorSesion : ProveedorUsuario {
    fun guardarUsuario(nombreUsuario: String)
    fun obtenerUsuario(): String?
    fun cerrarSesion()
}

/**
 * Datos del login que quedan en el teléfono:
 *  - el último usuario que entró, para completar el campo solo;
 *  - los intentos fallidos, para que el bloqueo siga aunque se cierre la app.
 */
interface PreferenciasLogin {
    var ultimoUsuario: String?
    fun leerIntentos(): EstadoIntentos
    fun guardarIntentos(estado: EstadoIntentos)
}
