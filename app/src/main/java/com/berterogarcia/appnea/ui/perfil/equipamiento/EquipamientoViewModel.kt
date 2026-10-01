package com.berterogarcia.appnea.ui.perfil.equipamiento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.data.local.EquipoEntity
import com.berterogarcia.appnea.data.repository.PerfilRepository
import com.berterogarcia.appnea.ui.perfil.comun.EstadoLista
import com.berterogarcia.appnea.ui.perfil.comun.MENSAJE_ERROR_ELIMINAR
import com.berterogarcia.appnea.ui.perfil.comun.MENSAJE_SIN_SESION
import com.berterogarcia.appnea.ui.perfil.comun.comoEstadoLista
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EquipamientoViewModel(
    private val repository: PerfilRepository,
    private val usuario: String?
) : ViewModel() {

    /** Orden: primero lo que está en uso, después por categoría (orden del enum) y marca. */
    val estado: StateFlow<EstadoLista<EquipoEntity>> =
        if (usuario == null) {
            MutableStateFlow(EstadoLista(cargando = false, error = MENSAJE_SIN_SESION))
        } else {
            repository.observarEquipamiento(usuario).comoEstadoLista(viewModelScope) { lista ->
                lista.sortedWith(
                    compareByDescending<EquipoEntity> { it.enUso }
                        .thenBy { it.categoria.ordinal }
                        .thenBy { it.marca.lowercase() }
                )
            }
        }

    /** Errores puntuales de acciones (ej: falló un borrado). La UI lo muestra y lo limpia. */
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    fun eliminar(id: Long) {
        val u = usuario ?: return
        viewModelScope.launch {
            runCatching { repository.eliminarEquipo(id, u) }
                .onFailure { _mensaje.value = MENSAJE_ERROR_ELIMINAR }
        }
    }

    fun mensajeMostrado() {
        _mensaje.value = null
    }

    companion object {
        fun Factory(repository: PerfilRepository, usuario: String?) = viewModelFactory {
            initializer { EquipamientoViewModel(repository, usuario) }
        }
    }
}
