package com.berterogarcia.appnea.ui.perfil.contactos

import com.berterogarcia.appnea.util.intentar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.ContactoEmergenciaEntity
import com.berterogarcia.appnea.data.repository.PerfilRepository
import com.berterogarcia.appnea.ui.components.TextoUi
import com.berterogarcia.appnea.ui.components.textoUi
import com.berterogarcia.appnea.ui.perfil.comun.MENSAJE_ERROR_CARGAR
import com.berterogarcia.appnea.ui.perfil.comun.MENSAJE_ERROR_GUARDAR
import com.berterogarcia.appnea.ui.perfil.comun.MENSAJE_SIN_SESION
import com.berterogarcia.appnea.ui.perfil.comun.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CampoContacto { NOMBRE, RELACION, TELEFONO, TELEFONO_ALT, NOTAS }

data class ContactoFormUiState(
    val cargando: Boolean = true,
    val esEdicion: Boolean = false,
    val nombre: String = "",
    val relacion: String = "",
    val telefono: String = "",
    val telefonoAlternativo: String = "",
    val esPrincipal: Boolean = false,
    /** true si es el primer contacto: va a quedar como principal sí o sí. */
    val esPrimerContacto: Boolean = false,
    /** true si al abrir ya era el principal: no se puede "desmarcar" (siempre tiene que haber uno). */
    val eraPrincipal: Boolean = false,
    val notas: String = "",
    val errores: Map<CampoContacto, TextoUi> = emptyMap(),
    val guardando: Boolean = false,
    val guardadoOk: Boolean = false,
    val errorGeneral: TextoUi? = null,
    val bloqueado: Boolean = false
) {
    val puedeGuardar: Boolean get() = !cargando && !bloqueado
    val principalBloqueado: Boolean get() = esPrimerContacto || eraPrincipal
}

class ContactoFormViewModel(
    private val repository: PerfilRepository,
    private val idContacto: Long?
) : ViewModel() {
    /** Usuario logueado al abrir la pantalla (de la sesión; el repositorio valida cada operación). */
    private val usuario: String? = repository.usuarioActual

    private val _uiState = MutableStateFlow(ContactoFormUiState(esEdicion = idContacto != null))
    val uiState: StateFlow<ContactoFormUiState> = _uiState.asStateFlow()

    init {
        if (usuario == null) {
            _uiState.update { it.copy(cargando = false, bloqueado = true, errorGeneral = MENSAJE_SIN_SESION) }
        } else {
            cargar(usuario)
        }
    }

    /** Siempre se lee la base: en alta para saber si es el primer contacto, en edición para traer los datos. */
    private fun cargar(usuario: String) {
        viewModelScope.launch {
            intentar("ContactoFormViewModel") {
                val cantidad = repository.cantidadContactos(usuario)
                val existente = idContacto?.let { repository.obtenerContacto(it, usuario) }
                cantidad to existente
            }.onSuccess { (cantidad, existente) ->
                when {
                    idContacto == null -> _uiState.update {
                        it.copy(cargando = false, esPrimerContacto = cantidad == 0, esPrincipal = cantidad == 0)
                    }
                    existente == null -> _uiState.update {
                        it.copy(cargando = false, bloqueado = true, errorGeneral = textoUi(R.string.perfil_error_contacto_no_existe))
                    }
                    else -> _uiState.update {
                        it.copy(
                            cargando = false,
                            nombre = existente.nombre,
                            relacion = existente.relacion,
                            telefono = existente.telefono,
                            telefonoAlternativo = existente.telefonoAlternativo,
                            esPrincipal = existente.esPrincipal,
                            eraPrincipal = existente.esPrincipal,
                            notas = existente.notas
                        )
                    }
                }
            }.onFailure {
                _uiState.update { it.copy(cargando = false, bloqueado = true, errorGeneral = MENSAJE_ERROR_CARGAR) }
            }
        }
    }

    fun onNombreChange(v: String) = actualizar(CampoContacto.NOMBRE) { copy(nombre = v) }
    fun onRelacionChange(v: String) = actualizar(CampoContacto.RELACION) { copy(relacion = v) }
    fun onTelefonoChange(v: String) = actualizar(CampoContacto.TELEFONO) { copy(telefono = v) }
    fun onTelefonoAltChange(v: String) = actualizar(CampoContacto.TELEFONO_ALT) { copy(telefonoAlternativo = v) }
    fun onNotasChange(v: String) = actualizar(CampoContacto.NOTAS) { copy(notas = v) }

    fun onPrincipalChange(v: Boolean) {
        if (_uiState.value.principalBloqueado) return
        _uiState.update { it.copy(esPrincipal = v) }
    }

    private fun actualizar(campo: CampoContacto, cambio: ContactoFormUiState.() -> ContactoFormUiState) {
        _uiState.update { it.cambio().copy(errores = it.errores - campo) }
    }

    fun guardar() {
        val s = _uiState.value
        if (s.guardando || !s.puedeGuardar || usuario == null) return

        val errores = validar(s)
        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(errores = errores) }
            return
        }

        _uiState.update { it.copy(guardando = true, errorGeneral = null) }
        viewModelScope.launch {
            val contacto = ContactoEmergenciaEntity(
                id = idContacto ?: 0L,
                nombreUsuario = usuario,
                nombre = s.nombre.trim(),
                relacion = s.relacion.trim(),
                telefono = s.telefono.trim(),
                telefonoAlternativo = s.telefonoAlternativo.trim(),
                esPrincipal = s.esPrincipal,
                notas = s.notas.trim()
            )
            intentar("ContactoFormViewModel") { repository.guardarContacto(contacto) }
                .onSuccess { _uiState.update { it.copy(guardando = false, guardadoOk = true) } }
                .onFailure { _uiState.update { it.copy(guardando = false, errorGeneral = MENSAJE_ERROR_GUARDAR) } }
        }
    }

    private fun validar(s: ContactoFormUiState): Map<CampoContacto, TextoUi> = buildMap {
        (Validaciones.obligatorio(s.nombre, textoUi(R.string.campo_el_nombre)) ?: Validaciones.largoMaximo(s.nombre, 60))
            ?.let { put(CampoContacto.NOMBRE, it) }
        Validaciones.largoMaximo(s.relacion, 40)?.let { put(CampoContacto.RELACION, it) }
        Validaciones.telefono(s.telefono, obligatorio = true)?.let { put(CampoContacto.TELEFONO, it) }
        Validaciones.telefono(s.telefonoAlternativo, obligatorio = false)?.let { put(CampoContacto.TELEFONO_ALT, it) }
        Validaciones.largoMaximo(s.notas, 300)?.let { put(CampoContacto.NOTAS, it) }
    }
}
