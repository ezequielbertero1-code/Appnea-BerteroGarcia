package com.berterogarcia.appnea.ui.registro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.data.repository.UsuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface RegistroUiState {
    object Idle : RegistroUiState
    object Cargando : RegistroUiState
    object Exitoso : RegistroUiState
    data class Error(val mensaje: String) : RegistroUiState
}

class RegistroViewModel(private val repository: UsuarioRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<RegistroUiState>(RegistroUiState.Idle)
    val uiState: StateFlow<RegistroUiState> = _uiState

    fun registrar(usuario: String, password: String, confirmarPassword: String) {
        if (usuario.isBlank() || password.isBlank()) {
            _uiState.value = RegistroUiState.Error("Completá todos los campos.")
            return
        }
        if (password != confirmarPassword) {
            _uiState.value = RegistroUiState.Error("Las contraseñas no coinciden.")
            return
        }
        viewModelScope.launch {
            _uiState.value = RegistroUiState.Cargando
            val creado = repository.registrar(usuario.trim(), password)
            _uiState.value = if (creado) RegistroUiState.Exitoso
            else RegistroUiState.Error("Ese usuario ya existe.")
        }
    }

    companion object {
        fun Factory(repository: UsuarioRepository) = viewModelFactory {
            initializer { RegistroViewModel(repository) }
        }
    }
}