package com.berterogarcia.appnea.entrenamiento

import android.os.SystemClock
import com.berterogarcia.appnea.data.local.SonidoManager
import com.berterogarcia.appnea.data.local.TablaApnea
import com.berterogarcia.appnea.data.local.TipoFase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/** Cómo terminó una tabla: lo que después se guarda como sesión. */
data class ResultadoTabla(
    val completada: Boolean,
    val faseAlcanzada: Int,
    val marcaSegundos: Int,
    val duracionSegundos: Int
)

data class EstadoMotor(
    val tabla: TablaApnea? = null,
    val corriendo: Boolean = false,
    val indiceFase: Int = 0,
    val segundosRestantes: Int = 0,
    /** Instante (reloj monotónico) en que termina la fase actual: para el anillo continuo. */
    val finFaseMs: Long = 0L,
    /** No null cuando la tabla terminó (completa o abortada) y falta guardar o descartar. */
    val resultado: ResultadoTabla? = null
)

/**
 * Corre una tabla de apnea: fases, cuenta regresiva y avisos sonoros.
 *
 * Por qué no vive en el ViewModel: el ViewModel muere con la pantalla y su corrutina se frena
 * si Android suspende la app. El motor vive en el AppContainer (toda la vida del proceso) y,
 * mientras corre, [alCambiarActividad] arranca un foreground service que mantiene vivo el proceso
 * con la pantalla bloqueada (ver [ServicioEntrenamiento]).
 *
 * No depende de Context: el reloj y el aviso al servicio se inyectan, así se puede testear.
 */
class MotorEntrenamiento(
    private val scope: CoroutineScope,
    private val sonido: SonidoManager?,
    private val reloj: () -> Long = SystemClock::elapsedRealtime,
    private val alCambiarActividad: (corriendo: Boolean) -> Unit = {}
) {
    private val _estado = MutableStateFlow(EstadoMotor())
    val estado: StateFlow<EstadoMotor> = _estado.asStateFlow()

    private var job: Job? = null
    private var momentoAbortoMs: Long? = null

    fun iniciar(tabla: TablaApnea) {
        if (_estado.value.corriendo || tabla.fases.isEmpty()) return
        val inicioMs = reloj()
        momentoAbortoMs = null
        _estado.value = EstadoMotor(
            tabla = tabla,
            corriendo = true,
            segundosRestantes = tabla.fases.first().duracionSegundos,
            finFaseMs = inicioMs + tabla.fases.first().duracionSegundos * 1000L
        )
        alCambiarActividad(true)
        job?.cancel()
        job = scope.launch { correr(tabla, inicioMs) }
    }

    /** Corta la tabla: se mide hasta el momento exacto del toque. */
    fun abortar() {
        if (!_estado.value.corriendo) return
        momentoAbortoMs = reloj()
        _estado.update { it.copy(corriendo = false) }
    }

    /** Vuelve a cero (después de guardar o descartar, o al cambiar de cuenta). */
    fun reiniciar() {
        job?.cancel()
        val estabaCorriendo = _estado.value.corriendo
        _estado.value = EstadoMotor()
        if (estabaCorriendo) alCambiarActividad(false)
    }

    private suspend fun correr(tabla: TablaApnea, inicioMs: Long) {
        // Cada fase termina en un instante FIJO calculado desde el inicio de la tabla:
        // lo que tarden los sonidos o la UI en cada vuelta no se acumula.
        var mejorApnea = 0
        var finFaseMs = inicioMs
        for ((indice, fase) in tabla.fases.withIndex()) {
            val inicioFaseMs = finFaseMs
            finFaseMs = inicioFaseMs + fase.duracionSegundos * 1000L
            _estado.update { it.copy(indiceFase = indice, segundosRestantes = fase.duracionSegundos, finFaseMs = finFaseMs) }
            sonido?.avisarInicioFase(fase.tipo)
            val mitad = fase.duracionSegundos / 2
            var avisoMitadHecho = false
            var aviso15Hecho = false
            var ultimoBeep = Int.MAX_VALUE // cada beep de 3-2-1 suena una sola vez
            var restante = segundosRestantes(finFaseMs)
            while (restante > 0 && _estado.value.corriendo) {
                if (!aviso15Hecho && restante <= 15 && fase.duracionSegundos > 20) {
                    sonido?.avisarQuedan15Segundos()
                    aviso15Hecho = true
                } else if (!avisoMitadHecho && restante <= mitad && fase.duracionSegundos >= 6) {
                    sonido?.avisarMitadDeFase()
                    avisoMitadHecho = true
                }
                if (restante in 1..3 && restante < ultimoBeep) {
                    sonido?.beepCuentaAtras()
                    ultimoBeep = restante
                }
                // Espera justo hasta que el reloj cambie de segundo (no "1 segundo desde ahora")
                val msRestantes = finFaseMs - reloj()
                delay((msRestantes - (restante - 1) * 1000L).coerceAtLeast(0L).milliseconds)
                restante = segundosRestantes(finFaseMs)
                _estado.update { it.copy(segundosRestantes = restante) }
            }
            if (!_estado.value.corriendo) {
                val hasta = momentoAbortoMs ?: reloj()
                val transcurridos = ((hasta - inicioFaseMs) / 1000L).toInt().coerceIn(0, fase.duracionSegundos)
                if (fase.tipo == TipoFase.APNEA) mejorApnea = maxOf(mejorApnea, transcurridos)
                terminar(ResultadoTabla(false, indice + 1, mejorApnea, duracion(inicioMs, hasta)))
                return
            }
            if (fase.tipo == TipoFase.APNEA) mejorApnea = maxOf(mejorApnea, fase.duracionSegundos)
        }
        sonido?.avisarFinTabla()
        terminar(ResultadoTabla(true, tabla.fases.size, mejorApnea, duracion(inicioMs, finFaseMs)))
    }

    private fun terminar(resultado: ResultadoTabla) {
        _estado.update { it.copy(corriendo = false, resultado = resultado) }
        alCambiarActividad(false)
    }

    private fun duracion(desdeMs: Long, hastaMs: Long) = ((hastaMs - desdeMs) / 1000L).toInt().coerceAtLeast(0)

    /** Segundos que faltan hasta [finMs], redondeado hacia arriba (2,3 s restantes → muestra 3). */
    private fun segundosRestantes(finMs: Long): Int =
        ((finMs - reloj() + 999L) / 1000L).toInt().coerceAtLeast(0)
}
