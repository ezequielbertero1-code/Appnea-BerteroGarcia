package com.berterogarcia.appnea.ui.perfil.comun

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

const val MENSAJE_SIN_SESION = "No hay una sesión activa. Cerrá sesión y volvé a ingresar."
const val MENSAJE_ERROR_GUARDAR = "No se pudo guardar. Intentá de nuevo."
const val MENSAJE_ERROR_CARGAR = "No se pudieron cargar los datos."
const val MENSAJE_ERROR_ELIMINAR = "No se pudo eliminar. Intentá de nuevo."

/** Estado de las pantallas de lista (equipamiento, certificados, contactos). */
data class EstadoLista<T>(
    val cargando: Boolean = true,
    val items: List<T> = emptyList(),
    val error: String? = null
)

/**
 * Convierte el Flow de Room en un StateFlow listo para la UI:
 *  - mientras no llega el primer dato: cargando = true
 *  - si la consulta falla: error con mensaje (no crashea)
 *  - WhileSubscribed(5s): sigue vivo durante una rotación de pantalla, pero deja de
 *    escuchar la base si el usuario se va de la pantalla.
 */
fun <T> Flow<List<T>>.comoEstadoLista(
    scope: CoroutineScope,
    ordenar: (List<T>) -> List<T> = { it }
): StateFlow<EstadoLista<T>> =
    map { EstadoLista(cargando = false, items = ordenar(it)) }
        .catch { emit(EstadoLista(cargando = false, error = MENSAJE_ERROR_CARGAR)) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), EstadoLista())
