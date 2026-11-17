package com.berterogarcia.appnea.ui.login

import com.berterogarcia.appnea.util.intentar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.dominio.modelo.CuentaGoogle
import com.berterogarcia.appnea.dominio.modelo.ResultadoGoogle
import com.berterogarcia.appnea.dominio.EstadoIntentos
import com.berterogarcia.appnea.dominio.PoliticaIntentos
import com.berterogarcia.appnea.dominio.PreferenciasLogin
import com.berterogarcia.appnea.dominio.GestorSesion
import com.berterogarcia.appnea.dominio.repositorios.RepositorioCuentaGoogle
import com.berterogarcia.appnea.dominio.repositorios.RepositorioUsuarios
import com.berterogarcia.appnea.ui.components.TextoUi
import com.berterogarcia.appnea.ui.components.pluralUi
import com.berterogarcia.appnea.ui.components.textoUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

data class LoginUiState(
    val usuario: String = "",
    val password: String = "",
    /** true si el usuario se completó solo con el último que entró. */
    val usuarioRecordado: Boolean = false,
    val errorUsuario: TextoUi? = null,
    val errorPassword: TextoUi? = null,
    val errorGeneral: TextoUi? = null,
    /** Aviso naranja, ej: "Te quedan 2 intentos". */
    val aviso: TextoUi? = null,
    val cargando: Boolean = false,
    /** Esperando a Google (selector de cuentas) o guardando la cuenta. */
    val cargandoGoogle: Boolean = false,
    /** Cuenta de Google que entra por primera vez: la pantalla la manda al registro. */
    val cuentaParaRegistro: CuentaGoogle? = null,
    val exitoso: Boolean = false,
    /** Segundos que faltan para poder intentar de nuevo (0 = no bloqueado). */
    val bloqueadoSegundos: Int = 0
) {
    val bloqueado: Boolean get() = bloqueadoSegundos > 0
    /** Campos y botón deshabilitados mientras verifica, después de entrar o durante un bloqueo. */
    val habilitado: Boolean get() = !cargando && !cargandoGoogle && !exitoso && !bloqueado
    /** El bloqueo por intentos es solo de contraseña: con Google se puede entrar igual. */
    val googleHabilitado: Boolean get() = !cargando && !cargandoGoogle && !exitoso
}

class LoginViewModel(
    private val repository: RepositorioUsuarios,
    private val cuentaGoogleRepository: RepositorioCuentaGoogle,
    private val sessionManager: GestorSesion,
    private val preferencias: PreferenciasLogin,
    private val reloj: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var cuentaRegresiva: Job? = null

    init {
        val ultimo = intentar("LoginViewModel") { preferencias.ultimoUsuario }.getOrNull()
        _uiState.value = LoginUiState(usuario = ultimo.orEmpty(), usuarioRecordado = ultimo != null)
        actualizarBloqueo()
    }

    fun onUsuarioChange(v: String) {
        if (!_uiState.value.habilitado) return
        _uiState.update { it.copy(usuario = v, usuarioRecordado = false, errorUsuario = null, errorGeneral = null) }
    }

    fun onPasswordChange(v: String) {
        if (!_uiState.value.habilitado) return
        _uiState.update { it.copy(password = v, errorPassword = null, errorGeneral = null) }
    }

    fun login() {
        val s = _uiState.value
        if (!s.habilitado) return

        // El teclado del celular suele agregar un espacio al final al autocompletar.
        val usuario = s.usuario.trim()
        val errorUsuario = if (usuario.isEmpty()) textoUi(R.string.login_error_usuario) else null
        val errorPassword = if (s.password.isEmpty()) textoUi(R.string.login_error_password) else null
        if (errorUsuario != null || errorPassword != null) {
            _uiState.update { it.copy(errorUsuario = errorUsuario, errorPassword = errorPassword) }
            return
        }

        _uiState.update { it.copy(cargando = true, errorGeneral = null, aviso = null) }
        viewModelScope.launch {
            val valido = intentar("LoginViewModel") { repository.validar(usuario, s.password) }.getOrElse {
                _uiState.update { it.copy(cargando = false, errorGeneral = textoUi(R.string.login_error_verificar)) }
                return@launch
            }
            if (valido) onLoginCorrecto(usuario) else onLoginIncorrecto()
        }
    }

    /**
     * [pedirCuenta] abre el selector de cuentas de Google (necesita la Activity, por eso lo pone la
     * pantalla y el ViewModel no guarda ningún Context).
     */
    fun loginConGoogle(pedirCuenta: suspend () -> ResultadoGoogle) {
        if (!_uiState.value.googleHabilitado) return
        _uiState.update { it.copy(cargandoGoogle = true, errorGeneral = null, aviso = null) }
        viewModelScope.launch {
            when (val resultado = pedirCuenta()) {
                is ResultadoGoogle.Ok -> {
                    val cuenta = resultado.cuenta
                    val existe = intentar("LoginViewModel") { cuentaGoogleRepository.existe(cuenta.email) }.getOrElse {
                        return@launch errorGoogle(R.string.login_error_google)
                    }
                    if (existe) {
                        // No se guarda como "último usuario": ese campo es para entrar con contraseña.
                        sessionManager.guardarUsuario(cuenta.email)
                        _uiState.update { it.copy(cargandoGoogle = false, exitoso = true) }
                    } else {
                        // Primera vez con esta cuenta: falta completar el registro.
                        _uiState.update { it.copy(cargandoGoogle = false, cuentaParaRegistro = cuenta) }
                    }
                }
                ResultadoGoogle.Cancelado -> _uiState.update { it.copy(cargandoGoogle = false) }
                ResultadoGoogle.SinCuentas -> errorGoogle(R.string.login_error_google_sin_cuentas)
                ResultadoGoogle.SinConfigurar -> errorGoogle(R.string.login_error_google_config)
                ResultadoGoogle.Error -> errorGoogle(R.string.login_error_google)
            }
        }
    }

    /** La pantalla ya abrió el registro: se limpia para no volver a abrirlo al volver al login. */
    fun registroGoogleAbierto() = _uiState.update { it.copy(cuentaParaRegistro = null) }

    private fun errorGoogle(mensaje: Int) =
        _uiState.update { it.copy(cargandoGoogle = false, errorGeneral = textoUi(mensaje)) }

    private fun onLoginCorrecto(usuario: String) {
        intentar("LoginViewModel") {
            preferencias.guardarIntentos(EstadoIntentos())
            preferencias.ultimoUsuario = usuario
        }
        // Esto deja la sesión guardada: al reabrir la app, SesionViewModel la lee.
        sessionManager.guardarUsuario(usuario)
        _uiState.update { it.copy(cargando = false, exitoso = true) }
    }

    private fun onLoginIncorrecto() {
        val estado = PoliticaIntentos.registrarFallo(leerIntentos(), reloj())
        intentar("LoginViewModel") { preferencias.guardarIntentos(estado) }

        val restantes = PoliticaIntentos.intentosRestantes(estado)
        _uiState.update {
            it.copy(
                cargando = false,
                password = "",
                // Mensaje genérico a propósito: no revela si el usuario existe.
                errorGeneral = textoUi(R.string.login_error_credenciales),
                aviso = if (restantes in 1..2) pluralUi(R.plurals.login_intentos_restantes, restantes, restantes) else null
            )
        }
        actualizarBloqueo()
    }

    /** Si hay un bloqueo activo, arranca una cuenta regresiva que actualiza los segundos. */
    private fun actualizarBloqueo() {
        if (PoliticaIntentos.restanteMs(leerIntentos(), reloj()) <= 0L) return
        cuentaRegresiva?.cancel()
        cuentaRegresiva = viewModelScope.launch {
            while (true) {
                val ms = PoliticaIntentos.restanteMs(leerIntentos(), reloj())
                val segundos = ((ms + 999) / 1000).toInt()
                _uiState.update { it.copy(bloqueadoSegundos = segundos, aviso = null) }
                if (segundos <= 0) break
                delay(1.seconds)
            }
        }
    }

    private fun leerIntentos(): EstadoIntentos = intentar("LoginViewModel") { preferencias.leerIntentos() }.getOrDefault(EstadoIntentos())
}