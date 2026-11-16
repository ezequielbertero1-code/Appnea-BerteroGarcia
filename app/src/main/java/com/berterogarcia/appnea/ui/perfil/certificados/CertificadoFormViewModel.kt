package com.berterogarcia.appnea.ui.perfil.certificados

import com.berterogarcia.appnea.util.intentar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.CertificadoEntity
import com.berterogarcia.appnea.data.local.TipoCertificado
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

enum class CampoCertificado { TIPO, TITULO, ENTIDAD, ESCUELA, NUMERO, EMISION, VENCIMIENTO, NOTAS }

data class CertificadoFormUiState(
    val cargando: Boolean = false,
    val esEdicion: Boolean = false,
    val tipo: TipoCertificado? = null,
    val titulo: String = "",
    val entidad: String = "",
    val escuelaInstructor: String = "",
    val numero: String = "",
    val fechaEmision: LocalDate? = null,
    val fechaVencimiento: LocalDate? = null,
    val notas: String = "",
    val errores: Map<CampoCertificado, TextoUi> = emptyMap(),
    val guardando: Boolean = false,
    val guardadoOk: Boolean = false,
    val errorGeneral: TextoUi? = null,
    val bloqueado: Boolean = false,
    /** Se incrementa cada vez que se intenta guardar con errores: la UI lo usa para scrollear al error. */
    val intentoGuardado: Int = 0
) {
    val puedeGuardar: Boolean get() = !cargando && !bloqueado

    /** Para el apto médico el vencimiento es lo más importante, así que es obligatorio. */
    val vencimientoObligatorio: Boolean get() = tipo == TipoCertificado.APTO_MEDICO
}

class CertificadoFormViewModel(
    private val repository: PerfilRepository,
    private val usuario: String?,
    private val idCertificado: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(CertificadoFormUiState(esEdicion = idCertificado != null))
    val uiState: StateFlow<CertificadoFormUiState> = _uiState.asStateFlow()

    init {
        when {
            usuario == null ->
                _uiState.update { it.copy(bloqueado = true, errorGeneral = MENSAJE_SIN_SESION) }
            idCertificado != null -> cargar(usuario, idCertificado)
        }
    }

    private fun cargar(usuario: String, id: Long) {
        _uiState.update { it.copy(cargando = true) }
        viewModelScope.launch {
            val c = intentar("CertificadoFormViewModel") { repository.obtenerCertificado(id, usuario) }.getOrElse {
                _uiState.update { it.copy(cargando = false, bloqueado = true, errorGeneral = MENSAJE_ERROR_CARGAR) }
                return@launch
            }
            if (c == null) {
                _uiState.update { it.copy(cargando = false, bloqueado = true, errorGeneral = textoUi(R.string.perfil_error_cert_no_existe)) }
                return@launch
            }
            _uiState.update {
                it.copy(
                    cargando = false,
                    tipo = c.tipo,
                    titulo = c.titulo,
                    entidad = c.entidad,
                    escuelaInstructor = c.escuelaInstructor,
                    numero = c.numero,
                    fechaEmision = c.fechaEmision,
                    fechaVencimiento = c.fechaVencimiento,
                    notas = c.notas
                )
            }
        }
    }

    fun onTipoChange(v: TipoCertificado) = actualizar(CampoCertificado.TIPO) {
        // Comodidad: si elige "Apto médico" y no escribió título, lo completamos.
        val nuevoTitulo = if (v == TipoCertificado.APTO_MEDICO && titulo.isBlank()) "Apto médico" else titulo
        copy(tipo = v, titulo = nuevoTitulo, errores = errores - CampoCertificado.TITULO - CampoCertificado.VENCIMIENTO)
    }
    fun onTituloChange(v: String) = actualizar(CampoCertificado.TITULO) { copy(titulo = v) }
    fun onEntidadChange(v: String) = actualizar(CampoCertificado.ENTIDAD) { copy(entidad = v) }
    fun onEscuelaChange(v: String) = actualizar(CampoCertificado.ESCUELA) { copy(escuelaInstructor = v) }
    fun onNumeroChange(v: String) = actualizar(CampoCertificado.NUMERO) { copy(numero = v) }
    fun onEmisionChange(v: LocalDate?) = actualizar(CampoCertificado.EMISION) { copy(fechaEmision = v) }
    fun onVencimientoChange(v: LocalDate?) = actualizar(CampoCertificado.VENCIMIENTO) { copy(fechaVencimiento = v) }
    fun onNotasChange(v: String) = actualizar(CampoCertificado.NOTAS) { copy(notas = v) }

    private fun actualizar(campo: CampoCertificado, cambio: CertificadoFormUiState.() -> CertificadoFormUiState) {
        _uiState.update { actual ->
            val nuevo = actual.cambio()
            nuevo.copy(errores = nuevo.errores - campo)
        }
    }

    fun guardar() {
        val s = _uiState.value
        if (s.guardando || !s.puedeGuardar || usuario == null) return

        val errores = validar(s)
        val tipo = s.tipo
        if (errores.isNotEmpty() || tipo == null) {
            _uiState.update { it.copy(errores = errores, intentoGuardado = it.intentoGuardado + 1) }
            return
        }

        _uiState.update { it.copy(guardando = true, errorGeneral = null) }
        viewModelScope.launch {
            val certificado = CertificadoEntity(
                id = idCertificado ?: 0L,
                nombreUsuario = usuario,
                tipo = tipo,
                titulo = s.titulo.trim(),
                entidad = s.entidad.trim(),
                escuelaInstructor = s.escuelaInstructor.trim(),
                numero = s.numero.trim(),
                fechaEmision = s.fechaEmision,
                fechaVencimiento = s.fechaVencimiento,
                notas = s.notas.trim()
            )
            intentar("CertificadoFormViewModel") { repository.guardarCertificado(certificado) }
                .onSuccess { _uiState.update { it.copy(guardando = false, guardadoOk = true) } }
                .onFailure { _uiState.update { it.copy(guardando = false, errorGeneral = MENSAJE_ERROR_GUARDAR) } }
        }
    }

    private fun validar(s: CertificadoFormUiState): Map<CampoCertificado, TextoUi> = buildMap {
        if (s.tipo == null) put(CampoCertificado.TIPO, textoUi(R.string.perfil_error_cert_tipo))
        (Validaciones.obligatorio(s.titulo, textoUi(R.string.campo_nombre_nivel))
            ?: Validaciones.largoMaximo(s.titulo, 80))?.let { put(CampoCertificado.TITULO, it) }
        Validaciones.largoMaximo(s.entidad, 40)?.let { put(CampoCertificado.ENTIDAD, it) }
        Validaciones.largoMaximo(s.escuelaInstructor, 80)?.let { put(CampoCertificado.ESCUELA, it) }
        Validaciones.largoMaximo(s.numero, 40)?.let { put(CampoCertificado.NUMERO, it) }
        Validaciones.noFutura(s.fechaEmision)?.let { put(CampoCertificado.EMISION, it) }
        when {
            s.vencimientoObligatorio && s.fechaVencimiento == null ->
                put(CampoCertificado.VENCIMIENTO, textoUi(R.string.perfil_error_cert_vencimiento))
            else -> Validaciones.noAnterior(
                s.fechaVencimiento, s.fechaEmision, textoUi(R.string.perfil_error_cert_vence_antes)
            )?.let { put(CampoCertificado.VENCIMIENTO, it) }
        }
        Validaciones.largoMaximo(s.notas, 300)?.let { put(CampoCertificado.NOTAS, it) }
    }

    companion object {
        fun Factory(repository: PerfilRepository, usuario: String?, idCertificado: Long?) = viewModelFactory {
            initializer { CertificadoFormViewModel(repository, usuario, idCertificado) }
        }
    }
}
