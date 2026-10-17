package com.berterogarcia.appnea.ui.perfil.datos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.PerfilUsuarioEntity
import com.berterogarcia.appnea.data.repository.PerfilRepository
import com.berterogarcia.appnea.ui.components.TextoUi
import com.berterogarcia.appnea.ui.components.textoUi
import com.berterogarcia.appnea.ui.perfil.comun.MENSAJE_ERROR_CARGAR
import com.berterogarcia.appnea.ui.perfil.comun.MENSAJE_ERROR_GUARDAR
import com.berterogarcia.appnea.ui.perfil.comun.MENSAJE_SIN_SESION
import com.berterogarcia.appnea.ui.perfil.comun.Validaciones
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CampoDatos { NOMBRE, APELLIDO, EMAIL, TELEFONO, FECHA_NACIMIENTO, CIUDAD, COMPANERO }

data class DatosPersonalesUiState(
    val cargando: Boolean = true,
    val nombre: String = "",
    val apellido: String = "",
    val email: String = "",
    val telefono: String = "",
    val fechaNacimiento: LocalDate? = null,
    val ciudad: String = "",
    val companeroHabitual: String = "",
    val errores: Map<CampoDatos, TextoUi> = emptyMap(),
    val guardando: Boolean = false,
    val guardadoOk: Boolean = false,
    val errorGeneral: TextoUi? = null,
    /** true si no hay sesión o falló la carga: no se deja guardar para no pisar datos con un form vacío. */
    val bloqueado: Boolean = false
) {
    val puedeGuardar: Boolean get() = !cargando && !bloqueado
}

class DatosPersonalesViewModel(
    private val repository: PerfilRepository,
    private val usuario: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(DatosPersonalesUiState())
    val uiState: StateFlow<DatosPersonalesUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    private fun cargar() {
        if (usuario == null) {
            _uiState.update { it.copy(cargando = false, bloqueado = true, errorGeneral = MENSAJE_SIN_SESION) }
            return
        }
        viewModelScope.launch {
            runCatching { repository.obtenerPerfil(usuario) }
                .onSuccess { perfil ->
                    _uiState.update {
                        if (perfil == null) {
                            // Primera vez: proponemos el nombre de usuario como nombre.
                            it.copy(cargando = false, nombre = usuario)
                        } else {
                            it.copy(
                                cargando = false,
                                nombre = perfil.nombre,
                                apellido = perfil.apellido,
                                email = perfil.email,
                                telefono = perfil.telefono,
                                fechaNacimiento = perfil.fechaNacimiento,
                                ciudad = perfil.ciudad,
                                companeroHabitual = perfil.companeroHabitual
                            )
                        }
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(cargando = false, bloqueado = true, errorGeneral = MENSAJE_ERROR_CARGAR) }
                }
        }
    }

    // Cada cambio limpia el error de ESE campo para que no quede en rojo mientras se corrige.
    fun onNombreChange(v: String) = actualizar(CampoDatos.NOMBRE) { copy(nombre = v) }
    fun onApellidoChange(v: String) = actualizar(CampoDatos.APELLIDO) { copy(apellido = v) }
    fun onEmailChange(v: String) = actualizar(CampoDatos.EMAIL) { copy(email = v) }
    fun onTelefonoChange(v: String) = actualizar(CampoDatos.TELEFONO) { copy(telefono = v) }
    fun onFechaNacimientoChange(v: LocalDate?) = actualizar(CampoDatos.FECHA_NACIMIENTO) { copy(fechaNacimiento = v) }
    fun onCiudadChange(v: String) = actualizar(CampoDatos.CIUDAD) { copy(ciudad = v) }
    fun onCompaneroChange(v: String) = actualizar(CampoDatos.COMPANERO) { copy(companeroHabitual = v) }

    private fun actualizar(campo: CampoDatos, cambio: DatosPersonalesUiState.() -> DatosPersonalesUiState) {
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
            val perfil = PerfilUsuarioEntity(
                nombreUsuario = usuario,
                nombre = s.nombre.trim(),
                apellido = s.apellido.trim(),
                email = s.email.trim(),
                telefono = s.telefono.trim(),
                fechaNacimiento = s.fechaNacimiento,
                ciudad = s.ciudad.trim(),
                companeroHabitual = s.companeroHabitual.trim()
            )
            runCatching { repository.guardarPerfil(perfil) }
                .onSuccess { _uiState.update { it.copy(guardando = false, guardadoOk = true) } }
                .onFailure { _uiState.update { it.copy(guardando = false, errorGeneral = MENSAJE_ERROR_GUARDAR) } }
        }
    }

    private fun validar(s: DatosPersonalesUiState): Map<CampoDatos, TextoUi> = buildMap {
        (Validaciones.obligatorio(s.nombre, textoUi(R.string.campo_tu_nombre)) ?: Validaciones.largoMaximo(s.nombre, 60))
            ?.let { put(CampoDatos.NOMBRE, it) }
        Validaciones.largoMaximo(s.apellido, 60)?.let { put(CampoDatos.APELLIDO, it) }
        Validaciones.email(s.email)?.let { put(CampoDatos.EMAIL, it) }
        Validaciones.telefono(s.telefono, obligatorio = false)?.let { put(CampoDatos.TELEFONO, it) }
        Validaciones.noFutura(s.fechaNacimiento)?.let { put(CampoDatos.FECHA_NACIMIENTO, it) }
        Validaciones.largoMaximo(s.ciudad, 80)?.let { put(CampoDatos.CIUDAD, it) }
        Validaciones.largoMaximo(s.companeroHabitual, 60)?.let { put(CampoDatos.COMPANERO, it) }
    }

    companion object {
        fun Factory(repository: PerfilRepository, usuario: String?) = viewModelFactory {
            initializer { DatosPersonalesViewModel(repository, usuario) }
        }
    }
}
