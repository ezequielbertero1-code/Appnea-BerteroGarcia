package com.berterogarcia.appnea.ui.perfil.equipamiento

import com.berterogarcia.appnea.util.intentar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.CategoriaEquipo
import com.berterogarcia.appnea.data.local.EquipoEntity
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
import java.time.LocalDate

enum class CampoEquipo { CATEGORIA, MARCA_MODELO, TALLE, FECHA_COMPRA, MANTENIMIENTO, NOTAS }

data class EquipoFormUiState(
    val cargando: Boolean = false,
    val esEdicion: Boolean = false,
    val categoria: CategoriaEquipo? = null,
    val marca: String = "",
    val modelo: String = "",
    val talle: String = "",
    val fechaCompra: LocalDate? = null,
    val proximoMantenimiento: LocalDate? = null,
    val enUso: Boolean = true,
    val notas: String = "",
    val errores: Map<CampoEquipo, TextoUi> = emptyMap(),
    val guardando: Boolean = false,
    val guardadoOk: Boolean = false,
    val errorGeneral: TextoUi? = null,
    val bloqueado: Boolean = false,
    /** Se incrementa cada vez que se intenta guardar con errores: la UI lo usa para scrollear al error. */
    val intentoGuardado: Int = 0
) {
    val puedeGuardar: Boolean get() = !cargando && !bloqueado
}

/**
 * Alta y edición de un equipo.
 * @param idEquipo null = alta; si viene un id se carga ese equipo para editarlo.
 */
class EquipoFormViewModel(
    private val repository: PerfilRepository,
    private val idEquipo: Long?
) : ViewModel() {
    /** Usuario logueado al abrir la pantalla (de la sesión; el repositorio valida cada operación). */
    private val usuario: String? = repository.usuarioActual

    private val _uiState = MutableStateFlow(EquipoFormUiState(esEdicion = idEquipo != null))
    val uiState: StateFlow<EquipoFormUiState> = _uiState.asStateFlow()

    init {
        when {
            usuario == null ->
                _uiState.update { it.copy(bloqueado = true, errorGeneral = MENSAJE_SIN_SESION) }
            idEquipo != null -> cargar(usuario, idEquipo)
        }
    }

    private fun cargar(usuario: String, id: Long) {
        _uiState.update { it.copy(cargando = true) }
        viewModelScope.launch {
            val equipo = intentar("EquipoFormViewModel") { repository.obtenerEquipo(id, usuario) }.getOrElse {
                _uiState.update { it.copy(cargando = false, bloqueado = true, errorGeneral = MENSAJE_ERROR_CARGAR) }
                return@launch
            }
            if (equipo == null) {
                _uiState.update { it.copy(cargando = false, bloqueado = true, errorGeneral = textoUi(R.string.perfil_error_equipo_no_existe)) }
                return@launch
            }
            _uiState.update {
                it.copy(
                    cargando = false,
                    categoria = equipo.categoria,
                    marca = equipo.marca,
                    modelo = equipo.modelo,
                    talle = equipo.talle,
                    fechaCompra = equipo.fechaCompra,
                    proximoMantenimiento = equipo.proximoMantenimiento,
                    enUso = equipo.enUso,
                    notas = equipo.notas
                )
            }
        }
    }

    fun onCategoriaChange(v: CategoriaEquipo) = actualizar(CampoEquipo.CATEGORIA) { copy(categoria = v) }
    fun onMarcaChange(v: String) = actualizar(CampoEquipo.MARCA_MODELO) { copy(marca = v) }
    fun onModeloChange(v: String) = actualizar(CampoEquipo.MARCA_MODELO) { copy(modelo = v) }
    fun onTalleChange(v: String) = actualizar(CampoEquipo.TALLE) { copy(talle = v) }
    fun onFechaCompraChange(v: LocalDate?) = actualizar(CampoEquipo.FECHA_COMPRA) { copy(fechaCompra = v) }
    fun onMantenimientoChange(v: LocalDate?) = actualizar(CampoEquipo.MANTENIMIENTO) { copy(proximoMantenimiento = v) }
    fun onEnUsoChange(v: Boolean) = _uiState.update { it.copy(enUso = v) }
    fun onNotasChange(v: String) = actualizar(CampoEquipo.NOTAS) { copy(notas = v) }

    private fun actualizar(campo: CampoEquipo, cambio: EquipoFormUiState.() -> EquipoFormUiState) {
        _uiState.update { it.cambio().copy(errores = it.errores - campo) }
    }

    fun guardar() {
        val s = _uiState.value
        if (s.guardando || !s.puedeGuardar || usuario == null) return

        val errores = validar(s)
        val categoria = s.categoria
        if (errores.isNotEmpty() || categoria == null) {
            _uiState.update { it.copy(errores = errores, intentoGuardado = it.intentoGuardado + 1) }
            return
        }

        _uiState.update { it.copy(guardando = true, errorGeneral = null) }
        viewModelScope.launch {
            val equipo = EquipoEntity(
                id = idEquipo ?: 0L,
                nombreUsuario = usuario,
                categoria = categoria,
                marca = s.marca.trim(),
                modelo = s.modelo.trim(),
                talle = s.talle.trim(),
                fechaCompra = s.fechaCompra,
                proximoMantenimiento = s.proximoMantenimiento,
                enUso = s.enUso,
                notas = s.notas.trim()
            )
            intentar("EquipoFormViewModel") { repository.guardarEquipo(equipo) }
                .onSuccess { _uiState.update { it.copy(guardando = false, guardadoOk = true) } }
                .onFailure { _uiState.update { it.copy(guardando = false, errorGeneral = MENSAJE_ERROR_GUARDAR) } }
        }
    }

    private fun validar(s: EquipoFormUiState): Map<CampoEquipo, TextoUi> = buildMap {
        if (s.categoria == null) put(CampoEquipo.CATEGORIA, textoUi(R.string.perfil_error_equipo_categoria))
        when {
            s.marca.isBlank() && s.modelo.isBlank() ->
                put(CampoEquipo.MARCA_MODELO, textoUi(R.string.perfil_error_equipo_marca))
            else -> (Validaciones.largoMaximo(s.marca, 40) ?: Validaciones.largoMaximo(s.modelo, 60))
                ?.let { put(CampoEquipo.MARCA_MODELO, it) }
        }
        Validaciones.largoMaximo(s.talle, 30)?.let { put(CampoEquipo.TALLE, it) }
        Validaciones.noFutura(s.fechaCompra)?.let { put(CampoEquipo.FECHA_COMPRA, it) }
        Validaciones.noAnterior(
            s.proximoMantenimiento, s.fechaCompra, textoUi(R.string.perfil_error_equipo_service)
        )?.let { put(CampoEquipo.MANTENIMIENTO, it) }
        Validaciones.largoMaximo(s.notas, 300)?.let { put(CampoEquipo.NOTAS, it) }
    }

    companion object {
        fun Factory(repository: PerfilRepository, idEquipo: Long?) = viewModelFactory {
            initializer { EquipoFormViewModel(repository, idEquipo) }
        }
    }
}
