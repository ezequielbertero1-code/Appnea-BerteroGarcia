package com.berterogarcia.appnea.ui.perfil.equipamiento

import com.berterogarcia.appnea.util.intentar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berterogarcia.appnea.dominio.modelo.Equipo
import com.berterogarcia.appnea.dominio.repositorios.RepositorioPerfil
import com.berterogarcia.appnea.ui.components.TextoUi
import com.berterogarcia.appnea.ui.perfil.comun.EstadoLista
import com.berterogarcia.appnea.ui.perfil.comun.MENSAJE_ERROR_ELIMINAR
import com.berterogarcia.appnea.ui.perfil.comun.MENSAJE_SIN_SESION
import com.berterogarcia.appnea.ui.perfil.comun.comoEstadoLista
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EquipamientoViewModel(
    private val repository: RepositorioPerfil
) : ViewModel() {
    /** Si había sesión al abrir la pantalla (el repositorio trabaja siempre sobre el usuario logueado). */
    private val haySesion: Boolean = repository.haySesion

    /** Orden: primero lo que está en uso, después por categoría (orden del enum) y marca. */
    val estado: StateFlow<EstadoLista<Equipo>> =
        if (!haySesion) {
            MutableStateFlow(EstadoLista(cargando = false, error = MENSAJE_SIN_SESION))
        } else {
            repository.equipamientoActual.comoEstadoLista(viewModelScope) { lista ->
                lista.sortedWith(
                    compareByDescending<Equipo> { it.enUso }
                        .thenBy { it.categoria.ordinal }
                        .thenBy { it.marca.lowercase() }
                )
            }
        }

    /** Errores puntuales de acciones (ej: falló un borrado). La UI lo muestra y lo limpia. */
    private val _mensaje = MutableStateFlow<TextoUi?>(null)
    val mensaje: StateFlow<TextoUi?> = _mensaje.asStateFlow()

    fun eliminar(id: Long) {
        if (!haySesion) return
        viewModelScope.launch {
            intentar("EquipamientoViewModel") { repository.eliminarEquipo(id) }
                .onFailure { _mensaje.value = MENSAJE_ERROR_ELIMINAR }
        }
    }

    fun mensajeMostrado() {
        _mensaje.value = null
    }
}
