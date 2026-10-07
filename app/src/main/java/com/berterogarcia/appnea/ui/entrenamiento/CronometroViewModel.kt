package com.berterogarcia.appnea.ui.entrenamiento

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

data class CronometroUiState(
    val tiempoFormateado: String = "00:00:00",
    val corriendo: Boolean = false
)

class CronometroViewModel : ViewModel() {

    private var elapsedMilis = 0L
    private var inicioMs = 0L      // reloj monotónico: no lo afectan cambios de hora del teléfono
    private var job: Job? = null

    private val _uiState = MutableStateFlow(CronometroUiState())
    val uiState: StateFlow<CronometroUiState> = _uiState

    fun onIniciar() {
        if (_uiState.value.corriendo) return
        inicioMs = SystemClock.elapsedRealtime() - elapsedMilis
        _uiState.update { it.copy(corriendo = true) }

        job?.cancel() // nunca dos loops a la vez (Parar + Iniciar muy rápido)
        job = viewModelScope.launch {
            while (isActive) {
                delay(30.milliseconds)
                elapsedMilis = SystemClock.elapsedRealtime() - inicioMs
                actualizarTexto()
            }
        }
    }

    fun onParar() {
        if (!_uiState.value.corriendo) return
        job?.cancel()
        elapsedMilis = SystemClock.elapsedRealtime() - inicioMs // valor exacto al tocar, no el del último tick
        _uiState.update { it.copy(corriendo = false) }
        actualizarTexto()
    }

    fun onResetear() {
        job?.cancel()
        elapsedMilis = 0L
        _uiState.update { it.copy(corriendo = false) }
        actualizarTexto()
    }

    private fun actualizarTexto() {
        val minutos = (elapsedMilis / 1000) / 60
        val segundos = (elapsedMilis / 1000) % 60
        val centesimas = (elapsedMilis % 1000) / 10
        _uiState.update {
            it.copy(
                tiempoFormateado = String.format(
                    Locale.getDefault(), "%02d:%02d:%02d", minutos, segundos, centesimas
                )
            )
        }
    }
}