package com.berterogarcia.appnea.ui.perfil.contactos

import com.berterogarcia.appnea.util.intentar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.ContactoEmergenciaEntity
import com.berterogarcia.appnea.data.repository.PerfilRepository
import com.berterogarcia.appnea.ui.components.TextoUi
import com.berterogarcia.appnea.ui.components.textoUi
import com.berterogarcia.appnea.ui.perfil.comun.EstadoLista
import com.berterogarcia.appnea.ui.perfil.comun.MENSAJE_ERROR_ELIMINAR
import com.berterogarcia.appnea.ui.perfil.comun.MENSAJE_ERROR_GUARDAR
import com.berterogarcia.appnea.ui.perfil.comun.MENSAJE_SIN_SESION
import com.berterogarcia.appnea.ui.perfil.comun.comoEstadoLista
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactosViewModel(
    private val repository: PerfilRepository
) : ViewModel() {
    /** Usuario logueado al abrir la pantalla (de la sesión; el repositorio valida cada operación). */
    private val usuario: String? = repository.usuarioActual

    /** El DAO ya los devuelve con el principal primero. */
    val estado: StateFlow<EstadoLista<ContactoEmergenciaEntity>> =
        if (usuario == null) {
            MutableStateFlow(EstadoLista(cargando = false, error = MENSAJE_SIN_SESION))
        } else {
            repository.observarContactos(usuario).comoEstadoLista(viewModelScope)
        }

    private val _mensaje = MutableStateFlow<TextoUi?>(null)
    val mensaje: StateFlow<TextoUi?> = _mensaje.asStateFlow()

    fun eliminar(id: Long) {
        val u = usuario ?: return
        viewModelScope.launch {
            intentar("ContactosViewModel") { repository.eliminarContacto(id, u) }
                .onFailure { _mensaje.value = MENSAJE_ERROR_ELIMINAR }
        }
    }

    fun hacerPrincipal(id: Long) {
        val u = usuario ?: return
        viewModelScope.launch {
            intentar("ContactosViewModel") { repository.hacerContactoPrincipal(id, u) }
                .onFailure { _mensaje.value = MENSAJE_ERROR_GUARDAR }
        }
    }

    /** Se llama desde la UI si no se pudo abrir el marcador (ej: emulador/tablet sin teléfono). */
    fun noSePudoLlamar() {
        _mensaje.value = textoUi(R.string.perfil_error_sin_telefono)
    }

    fun mensajeMostrado() {
        _mensaje.value = null
    }

    companion object {
        fun Factory(repository: PerfilRepository) = viewModelFactory {
            initializer { ContactosViewModel(repository) }
        }
    }
}
