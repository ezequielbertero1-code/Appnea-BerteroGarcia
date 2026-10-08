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
import java.time.LocalTime
import kotlin.time.Duration.Companion.milliseconds

/** Un intento del cronómetro libre (de Iniciar a Parar). */
data class IntentoApnea(
    val numero: Int,
    val hora: LocalTime,
    val duracionMs: Long
)

data class CronometroUiState(
    val transcurridoMs: Long = 0L,
    val corriendo: Boolean = false,
    /** Intentos de esta sesión, el más nuevo primero. Por ahora solo en memoria. */
    val intentos: List<IntentoApnea> = emptyList()
) {
    val mejorMs: Long? get() = intentos.maxOfOrNull { it.duracionMs }
}

/**
 * Cronómetro libre para apnea estática. Cada Iniciar → Parar es un intento.
 * Los intentos quedan en memoria; cuando exista la pantalla de Estadísticas se pueden
 * guardar en la base (la pantalla ya recibe un onGuardarIntentos opcional para eso).
 */
class CronometroViewModel : ViewModel() {

    private var inicioMs = 0L      // reloj monotónico: no lo afectan cambios de hora del teléfono
    private var job: Job? = null

    private val _uiState = MutableStateFlow(CronometroUiState())
    val uiState: StateFlow<CronometroUiState> = _uiState

    /** Arranca un intento NUEVO desde 0. */
    fun onIniciar() {
        if (_uiState.value.corriendo) return
        inicioMs = SystemClock.elapsedRealtime()
        _uiState.update { it.copy(corriendo = true, transcurridoMs = 0L) }

        job?.cancel() // nunca dos loops a la vez (Parar + Iniciar muy rápido)
        job = viewModelScope.launch {
            while (isActive) {
                delay(30.milliseconds)
                _uiState.update { it.copy(transcurridoMs = SystemClock.elapsedRealtime() - inicioMs) }
            }
        }
    }

    /** Termina el intento y lo agrega a la lista (si duró al menos 1 s, para ignorar toques sin querer). */
    fun onParar() {
        if (!_uiState.value.corriendo) return
        job?.cancel()
        val total = SystemClock.elapsedRealtime() - inicioMs // valor exacto al tocar, no el del último tick
        _uiState.update {
            val intentos = if (total >= 1_000L) {
                listOf(IntentoApnea(numero = it.intentos.size + 1, hora = LocalTime.now(), duracionMs = total)) + it.intentos
            } else it.intentos
            it.copy(corriendo = false, transcurridoMs = total, intentos = intentos)
        }
    }

    /** Vuelve el reloj a 0. Si estaba corriendo, ese intento se descarta. */
    fun onReiniciar() {
        job?.cancel()
        _uiState.update { it.copy(corriendo = false, transcurridoMs = 0L) }
    }
}
