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
        viewModelScope.launch {
            _uiState.value = LoginUiState.Cargando
            val valido = repository.validar(usuario, password)
            if (valido) {
                sessionManager.guardarUsuario(usuario)
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