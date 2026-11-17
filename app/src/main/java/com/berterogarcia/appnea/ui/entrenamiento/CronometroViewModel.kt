package com.berterogarcia.appnea.ui.entrenamiento

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.IntentoApneaEntity
import com.berterogarcia.appnea.data.repository.IntentoApneaRepository
import com.berterogarcia.appnea.ui.components.TextoUi
import com.berterogarcia.appnea.ui.components.textoUi
import com.berterogarcia.appnea.util.intentar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
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
    /** Intentos de hoy (guardados en la base), el más nuevo primero. */
    val intentos: List<IntentoApnea> = emptyList(),
    val errorGuardado: TextoUi? = null
) {
    val mejorMs: Long? get() = intentos.maxOfOrNull { it.duracionMs }
}

/** Lo que vive solo en la pantalla: el reloj del intento en curso. */
private data class Reloj(
    val transcurridoMs: Long = 0L,
    val corriendo: Boolean = false,
    val errorGuardado: TextoUi? = null
)

/**
 * Cronómetro libre para apnea estática. Cada Iniciar → Parar es un intento y se guarda solo
 * en la base (así no se pierde al salir de la pantalla ni si Android cierra la app).
 */
class CronometroViewModel(
    private val repository: IntentoApneaRepository,
    private val reloj: () -> Long = SystemClock::elapsedRealtime,
    private val ahora: () -> LocalDateTime = LocalDateTime::now
) : ViewModel() {

    private var inicioMs = 0L      // reloj monotónico: no lo afectan cambios de hora del teléfono
    private var job: Job? = null

    private val local = MutableStateFlow(Reloj())

    val uiState: StateFlow<CronometroUiState> = combine(
        local,
        repository.delDia(ahora().toLocalDate()).catch { emit(emptyList()) }
    ) { r, guardados ->
        CronometroUiState(
            transcurridoMs = r.transcurridoMs,
            corriendo = r.corriendo,
            intentos = guardados.mapIndexed { i, g -> IntentoApnea(guardados.size - i, g.hora, g.duracionMs) },
            errorGuardado = r.errorGuardado
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CronometroUiState())

    /** Arranca un intento NUEVO desde 0. */
    fun onIniciar() {
        if (local.value.corriendo) return
        inicioMs = reloj()
        local.update { it.copy(corriendo = true, transcurridoMs = 0L, errorGuardado = null) }
        job?.cancel() // nunca dos loops a la vez (Parar + Iniciar muy rápido)
        job = viewModelScope.launch {
            while (isActive) {
                delay(30.milliseconds)
                local.update { it.copy(transcurridoMs = reloj() - inicioMs) }
            }
        }
    }

    /** Termina el intento y lo guarda (si duró al menos 1 s, para ignorar toques sin querer). */
    fun onParar() {
        if (!local.value.corriendo) return
        job?.cancel()
        val total = reloj() - inicioMs // valor exacto al tocar, no el del último tick
        local.update { it.copy(corriendo = false, transcurridoMs = total) }
        if (total < 1_000L) return
        val fin = ahora()
        val intento = IntentoApneaEntity(
            fecha = fin.toLocalDate(),
            hora = fin.toLocalTime().minusNanos(total * 1_000_000L).withNano(0), // hora de inicio
            duracionMs = total
        )
        viewModelScope.launch {
            val ok = intentar("CronometroViewModel") { repository.guardar(intento) }.getOrDefault(false)
            if (!ok) local.update { it.copy(errorGuardado = textoUi(R.string.entrenar_error_guardar)) }
        }
    }

    /** Vuelve el reloj a 0. Si estaba corriendo, ese intento se descarta. */
    fun onReiniciar() {
        job?.cancel()
        local.update { it.copy(corriendo = false, transcurridoMs = 0L) }
    }
}
