package com.berterogarcia.appnea.data.local

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Quién está logueado ahora. Lo implementa [SessionManager].
 *
 * Los repositorios dependen de esta interfaz (y no de SessionManager directamente) para
 * poder testearlos con un usuario falso, sin Android ni SharedPreferences.
 */
interface ProveedorUsuario {
    /** Usuario logueado, o null si no hay sesión. Emite de nuevo al iniciar/cerrar sesión. */
    val usuarioActual: StateFlow<String?>
}

/**
 * Ejecuta [consulta] para el usuario logueado y se re-suscribe sola cuando cambia la sesión
 * (flatMapLatest cancela la consulta del usuario anterior). Sin sesión emite [siNoHaySesion].
 *
 * Ejemplo: `sesion.porUsuario(emptyList()) { u -> dao.obtenerTodas(u) }`
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> ProveedorUsuario.porUsuario(siNoHaySesion: T, consulta: (String) -> Flow<T>): Flow<T> =
    usuarioActual.flatMapLatest { usuario ->
        if (usuario == null) flowOf(siNoHaySesion) else consulta(usuario)
    }
