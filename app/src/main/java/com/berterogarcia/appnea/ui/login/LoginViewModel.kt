package com.berterogarcia.appnea.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.data.local.SessionManager
import com.berterogarcia.appnea.data.repository.UsuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Cargando : LoginUiState
    object Exitoso : LoginUiState
    data class Error(val mensaje: String) : LoginUiState
}

class LoginViewModel(
    private val repository: UsuarioRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(usuario: String, password: String) {
        // [NUEVO] Evita logins duplicados (doble toque en el botón + Enter, por ejemplo).
        if (_uiState.value is LoginUiState.Cargando || _uiState.value is LoginUiState.Exitoso) return

        // [NUEVO] El teclado del celular suele agregar un espacio al final al autocompletar.
        val nombre = usuario.trim()
        if (nombre.isEmpty() || password.isEmpty()) {
            _uiState.value = LoginUiState.Error("Completá usuario y contraseña.")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Cargando
            // [NUEVO] Si falla la base, se muestra un error en vez de crashear.
            val valido = runCatching { repository.validar(nombre, password) }.getOrElse {
                _uiState.value = LoginUiState.Error("No se pudo verificar el usuario. Intentá de nuevo.")
                return@launch
            }
            if (valido) {
                // Esto es lo que deja la sesión guardada: al reabrir la app, SesionViewModel lo lee.
                sessionManager.guardarUsuario(nombre)
                _uiState.value = LoginUiState.Exitoso
            } else {
                _uiState.value = LoginUiState.Error("Usuario o contraseña incorrectos.")
            }
        }
    }

    companion object {
        fun Factory(repository: UsuarioRepository, sessionManager: SessionManager) = viewModelFactory {
            initializer { LoginViewModel(repository, sessionManager) }
        }
    }
}
