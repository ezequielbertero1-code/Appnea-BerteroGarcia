package com.berterogarcia.appnea.data.sesion

import com.berterogarcia.appnea.dominio.ProveedorUsuario
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

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
