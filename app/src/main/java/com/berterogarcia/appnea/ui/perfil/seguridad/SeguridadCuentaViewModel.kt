package com.berterogarcia.appnea.ui.perfil.seguridad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.dominio.modelo.TipoCuenta
import com.berterogarcia.appnea.dominio.repositorios.RepositorioCuenta
import com.berterogarcia.appnea.dominio.repositorios.ResultadoCuenta
import com.berterogarcia.appnea.ui.components.TextoUi
import com.berterogarcia.appnea.ui.components.textoUi
import com.berterogarcia.appnea.ui.registro.ReglasRegistro
import com.berterogarcia.appnea.util.intentar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CampoSeguridad { ACTUAL, NUEVA, CONFIRMACION, PASSWORD_BORRAR }

data class SeguridadUiState(
    val cargando: Boolean = true,
    /** null = no hay sesión. */
    val tipo: TipoCuenta? = null,
    val actual: String = "",
    val nueva: String = "",
    val confirmacion: String = "",
    val cambiando: Boolean = false,
    /** Se muestra un aviso una vez y la pantalla lo limpia con avisoMostrado(). */
    val passwordCambiada: Boolean = false,
    val confirmandoBorrado: Boolean = false,
    val passwordBorrado: String = "",
    val borrando: Boolean = false,
    val cuentaBorrada: Boolean = false,
    val errores: Map<CampoSeguridad, TextoUi> = emptyMap(),
    val errorGeneral: TextoUi? = null
) {
    val esLocal: Boolean get() = tipo == TipoCuenta.LOCAL
    val ocupado: Boolean get() = cambiando || borrando
}

/** Cambiar la contraseña (cuentas locales) y borrar la cuenta con todos sus datos. */
class SeguridadCuentaViewModel(private val repositorio: RepositorioCuenta) : ViewModel() {

    private val _uiState = MutableStateFlow(SeguridadUiState())
    val uiState: StateFlow<SeguridadUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val tipo = intentar("SeguridadCuentaViewModel") { repositorio.tipoActual() }.getOrNull()
            _uiState.update { it.copy(cargando = false, tipo = tipo) }
        }
    }

    fun onActualChange(v: String) = _uiState.update { it.copy(actual = v, errores = it.errores - CampoSeguridad.ACTUAL) }
    fun onNuevaChange(v: String) = _uiState.update { it.copy(nueva = v, errores = it.errores - CampoSeguridad.NUEVA) }
    fun onConfirmacionChange(v: String) =
        _uiState.update { it.copy(confirmacion = v, errores = it.errores - CampoSeguridad.CONFIRMACION) }
    fun avisoMostrado() = _uiState.update { it.copy(passwordCambiada = false) }

    fun cambiarPassword() {
        val s = _uiState.value
        if (s.ocupado || !s.esLocal) return
        val errores = buildMap {
            if (s.actual.isEmpty()) put(CampoSeguridad.ACTUAL, textoUi(R.string.seguridad_error_actual_vacia))
            (ReglasRegistro.password(s.nueva)
                ?: textoUi(R.string.seguridad_error_igual).takeIf { s.nueva == s.actual })
                ?.let { put(CampoSeguridad.NUEVA, it) }
            ReglasRegistro.confirmacion(s.nueva, s.confirmacion)?.let { put(CampoSeguridad.CONFIRMACION, it) }
        }
        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(errores = errores) }
            return
        }
        _uiState.update { it.copy(cambiando = true, errorGeneral = null) }
        viewModelScope.launch {
            val r = intentar("SeguridadCuentaViewModel") { repositorio.cambiarPassword(s.actual, s.nueva) }.getOrNull()
            _uiState.update {
                when (r) {
                    ResultadoCuenta.Ok -> it.copy(cambiando = false, passwordCambiada = true, actual = "", nueva = "", confirmacion = "")
                    ResultadoCuenta.PasswordIncorrecta -> it.copy(
                        cambiando = false,
                        errores = mapOf(CampoSeguridad.ACTUAL to textoUi(R.string.seguridad_error_actual_incorrecta))
                    )
                    else -> it.copy(cambiando = false, errorGeneral = textoUi(R.string.seguridad_error_general))
                }
            }
        }
    }

    fun pedirBorrado() = _uiState.update { it.copy(confirmandoBorrado = true, passwordBorrado = "", errores = it.errores - CampoSeguridad.PASSWORD_BORRAR) }
    fun cancelarBorrado() { if (!_uiState.value.borrando) _uiState.update { it.copy(confirmandoBorrado = false, passwordBorrado = "") } }
    fun onPasswordBorradoChange(v: String) =
        _uiState.update { it.copy(passwordBorrado = v, errores = it.errores - CampoSeguridad.PASSWORD_BORRAR) }

    fun borrarCuenta() {
        val s = _uiState.value
        if (s.ocupado || s.tipo == null) return
        if (s.esLocal && s.passwordBorrado.isEmpty()) {
            _uiState.update { it.copy(errores = it.errores + (CampoSeguridad.PASSWORD_BORRAR to textoUi(R.string.seguridad_error_actual_vacia))) }
            return
        }
        _uiState.update { it.copy(borrando = true, errorGeneral = null) }
        viewModelScope.launch {
            val r = intentar("SeguridadCuentaViewModel") {
                repositorio.borrarCuenta(s.passwordBorrado.takeIf { s.esLocal })
            }.getOrNull()
            _uiState.update {
                when (r) {
                    ResultadoCuenta.Ok -> it.copy(borrando = false, confirmandoBorrado = false, cuentaBorrada = true)
                    ResultadoCuenta.PasswordIncorrecta -> it.copy(
                        borrando = false,
                        errores = it.errores + (CampoSeguridad.PASSWORD_BORRAR to textoUi(R.string.seguridad_error_actual_incorrecta))
                    )
                    else -> it.copy(borrando = false, confirmandoBorrado = false, errorGeneral = textoUi(R.string.seguridad_error_general))
                }
            }
        }
    }
}
