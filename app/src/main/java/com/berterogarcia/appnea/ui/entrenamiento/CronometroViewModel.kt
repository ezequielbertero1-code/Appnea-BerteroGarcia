package com.berterogarcia.appnea.ui.entrenamiento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

data class CronometroUiState(
    val tiempoFormateado: String = "00:00:00",
    val corriendo: Boolean = false
)

class CronometroViewModel : ViewModel() {

    private var elapsedMilis = 0L
    private val _uiState = MutableStateFlow(CronometroUiState())
    val uiState: StateFlow<CronometroUiState> = _uiState

    fun onIniciar() {
        if (_uiState.value.corriendo) return
        _uiState.update { it.copy(corriendo = true) }
        val inicio = System.currentTimeMillis() - elapsedMilis
        viewModelScope.launch {
            while (_uiState.value.corriendo) {
                delay(30.milliseconds)
                elapsedMilis = System.currentTimeMillis() - inicio
                actualizarTexto()
            }
        }
    }

    fun onParar() {
        _uiState.update { it.copy(corriendo = false) }
    }

    fun onResetear() {
        elapsedMilis = 0L
        _uiState.update { it.copy(corriendo = false) }
        actualizarTexto()
    }

    private fun actualizarTexto() {
        val minutos = (elapsedMilis / 1000) / 60
        val segundos = (elapsedMilis / 1000) % 60
        val milisegundos = (elapsedMilis % 1000) / 10
        _uiState.update {
            it.copy(
                tiempoFormateado = String.format(
                    Locale.getDefault(), "%02d:%02d:%02d", minutos, segundos, milisegundos
                )
            )
        }
    }
}