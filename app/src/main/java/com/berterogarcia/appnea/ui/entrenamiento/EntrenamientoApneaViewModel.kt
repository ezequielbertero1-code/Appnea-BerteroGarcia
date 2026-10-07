package com.berterogarcia.appnea.ui.entrenamiento

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.data.local.Fase
import com.berterogarcia.appnea.data.local.SesionApneaEntity
import com.berterogarcia.appnea.data.local.SonidoManager
import com.berterogarcia.appnea.data.local.TablaApnea
import com.berterogarcia.appnea.data.local.TablaPersonalizadaEntity
import com.berterogarcia.appnea.data.local.TablasPredefinidas
import com.berterogarcia.appnea.data.local.TipoFase
import com.berterogarcia.appnea.data.repository.SesionApneaRepository
import com.berterogarcia.appnea.data.repository.TablaPersonalizadaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

enum class EtapaEntrenamiento { SELECCION, EN_CURSO, GUARDANDO }

val FACTORES_DISPONIBLES = listOf(
    "Dormí bien",
    "Comí liviano",
    "Buena hidratación",
    "Estrés / ansiedad",
    "Cansancio físico",
    "Entrené acompañado"
)

data class EntrenamientoUiState(
    val tablasPredefinidas: List<TablaApnea> = TablasPredefinidas.listaBase(),
    val tablasPersonalizadas: List<TablaPersonalizadaEntity> = emptyList(),
    val tablaSeleccionada: TablaApnea? = null,
    val etapa: EtapaEntrenamiento = EtapaEntrenamiento.SELECCION,
    val indiceFase: Int = 0,
    val segundosRestantes: Int = 0,
    val corriendo: Boolean = false,
    val completada: Boolean = false,
    val faseAlcanzada: Int = 0,
    val marcaSegundos: Int = 0,
    val factoresSeleccionados: Set<String> = emptySet(),
    val sensaciones: String = "",
    val guardadoOk: Boolean = false,
    val sesiones: List<SesionApneaEntity> = emptyList(),
    val mostrarHistorial: Boolean = false
) {
    val faseActual: Fase? get() = tablaSeleccionada?.fases?.getOrNull(indiceFase)
    val totalFases: Int get() = tablaSeleccionada?.fases?.size ?: 0
}

class EntrenamientoApneaViewModel(
    private val tablaRepository: TablaPersonalizadaRepository,
    private val sesionRepository: SesionApneaRepository,
    private val sonido: SonidoManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EntrenamientoUiState())
    val uiState: StateFlow<EntrenamientoUiState> = _uiState

    private var job: Job? = null
    private var mejorApneaSegundos = 0
    // Momento (reloj monotónico) en que se tocó "Abortar": para medir exacto la apnea interrumpida
    private var momentoAbortoMs: Long? = null

    init {
        viewModelScope.launch {
            tablaRepository.todasLasTablas.collect { tablas ->
                _uiState.update { it.copy(tablasPersonalizadas = tablas) }
            }
        }
        viewModelScope.launch {
            sesionRepository.todasLasSesiones.collect { sesiones ->
                _uiState.update { it.copy(sesiones = sesiones) }
            }
        }
    }

    fun onSeleccionarTabla(tabla: TablaApnea) {
        if (_uiState.value.corriendo) return
        mejorApneaSegundos = 0
        _uiState.update {
            it.copy(
                tablaSeleccionada = tabla,
                etapa = EtapaEntrenamiento.SELECCION,
                indiceFase = 0,
                segundosRestantes = tabla.fases.first().duracionSegundos,
                completada = false,
                faseAlcanzada = 0,
                marcaSegundos = 0,
                factoresSeleccionados = emptySet(),
                sensaciones = "",
                guardadoOk = false
            )
        }
    }

    fun onGuardarTablaPersonalizada(tabla: TablaApnea) {
        viewModelScope.launch {
            tablaRepository.guardar(TablaPersonalizadaEntity(nombre = tabla.nombre, fases = tabla.fases))
        }
    }

    fun onEliminarTablaPersonalizada(tabla: TablaPersonalizadaEntity) {
        viewModelScope.launch { tablaRepository.eliminar(tabla) }
    }

    fun onIniciar() {
        val tabla = _uiState.value.tablaSeleccionada ?: return
        if (_uiState.value.corriendo) return

        mejorApneaSegundos = 0
        momentoAbortoMs = null
        _uiState.update { it.copy(etapa = EtapaEntrenamiento.EN_CURSO, corriendo = true) }

        job?.cancel() // por si quedó una corrida vieja
        job = viewModelScope.launch {
            // Cada fase termina en un instante FIJO calculado desde el inicio de la tabla.
            // Lo que tarden los sonidos o la UI en cada vuelta no se acumula: el tiempo restante
            // siempre se calcula contra el reloj, no contando vueltas.
            var finFaseMs = SystemClock.elapsedRealtime()

            for ((indice, fase) in tabla.fases.withIndex()) {
                val inicioFaseMs = finFaseMs
                finFaseMs = inicioFaseMs + fase.duracionSegundos * 1000L

                _uiState.update { it.copy(indiceFase = indice, segundosRestantes = fase.duracionSegundos) }
                sonido.avisarInicioFase(fase.tipo)

                val mitad = fase.duracionSegundos / 2
                var avisoMitadHecho = false
                var aviso15Hecho = false
                var ultimoBeep = Int.MAX_VALUE // cada beep de 3-2-1 suena una sola vez
                var restante = segundosRestantes(finFaseMs)

                while (restante > 0 && _uiState.value.corriendo) {
                    if (!aviso15Hecho && restante <= 15 && fase.duracionSegundos > 20) {
                        sonido.avisarQuedan15Segundos()
                        aviso15Hecho = true
                    } else if (!avisoMitadHecho && restante <= mitad && fase.duracionSegundos >= 6) {
                        sonido.avisarMitadDeFase()
                        avisoMitadHecho = true
                    }
                    if (restante in 1..3 && restante < ultimoBeep) {
                        sonido.beepCuentaAtras()
                        ultimoBeep = restante
                    }

                    // Espera justo hasta que el reloj cambie de segundo (no "1 segundo desde ahora")
                    val msRestantes = finFaseMs - SystemClock.elapsedRealtime()
                    val hastaProximoSegundo = (msRestantes - (restante - 1) * 1000L).coerceAtLeast(0L)
                    delay(hastaProximoSegundo.milliseconds)

                    restante = segundosRestantes(finFaseMs)
                    _uiState.update { it.copy(segundosRestantes = restante) }
                }

                if (!_uiState.value.corriendo) {
                    // Se abortó durante esta fase: se mide hasta el momento exacto del toque
                    val hasta = momentoAbortoMs ?: SystemClock.elapsedRealtime()
                    val transcurridos = ((hasta - inicioFaseMs) / 1000L).toInt()
                        .coerceIn(0, fase.duracionSegundos)
                    if (fase.tipo == TipoFase.APNEA) {
                        mejorApneaSegundos = maxOf(mejorApneaSegundos, transcurridos)
                    }
                    finalizarSesion(completada = false, faseAlcanzada = indice + 1)
                    return@launch
                }

                if (fase.tipo == TipoFase.APNEA) {
                    mejorApneaSegundos = maxOf(mejorApneaSegundos, fase.duracionSegundos)
                }
            }
            sonido.avisarFinTabla()
            finalizarSesion(completada = true, faseAlcanzada = tabla.fases.size)
        }
    }

    fun onAbortar() {
        if (!_uiState.value.corriendo) return
        momentoAbortoMs = SystemClock.elapsedRealtime()
        _uiState.update { it.copy(corriendo = false) }
    }

    /** Segundos que faltan hasta [finMs], redondeado hacia arriba (2,3 s restantes → muestra 3). */
    private fun segundosRestantes(finMs: Long): Int =
        ((finMs - SystemClock.elapsedRealtime() + 999L) / 1000L).toInt().coerceAtLeast(0)

    private fun finalizarSesion(completada: Boolean, faseAlcanzada: Int) {
        _uiState.update {
            it.copy(
                corriendo = false,
                etapa = EtapaEntrenamiento.GUARDANDO,
                completada = completada,
                faseAlcanzada = faseAlcanzada,
                marcaSegundos = mejorApneaSegundos
            )
        }
    }

    fun onFactorToggle(factor: String, marcado: Boolean) = _uiState.update {
        it.copy(
            factoresSeleccionados = if (marcado) it.factoresSeleccionados + factor
            else it.factoresSeleccionados - factor
        )
    }

    fun onSensacionesChange(v: String) = _uiState.update { it.copy(sensaciones = v) }

    fun onGuardarSesion() {
        val s = _uiState.value
        val tabla = s.tablaSeleccionada ?: return
        viewModelScope.launch {
            sesionRepository.guardar(
                SesionApneaEntity(
                    fecha = LocalDate.now(),
                    nombreTabla = tabla.nombre,
                    completada = s.completada,
                    faseAlcanzada = s.faseAlcanzada,
                    totalFases = tabla.fases.size,
                    marcaSegundos = s.marcaSegundos,
                    factores = s.factoresSeleccionados,
                    sensaciones = s.sensaciones
                )
            )
            _uiState.update { it.copy(guardadoOk = true) }
        }
    }

    fun onVolverASeleccion() {
        _uiState.update {
            it.copy(
                etapa = EtapaEntrenamiento.SELECCION,
                tablaSeleccionada = null,
                indiceFase = 0,
                segundosRestantes = 0,
                completada = false,
                faseAlcanzada = 0,
                marcaSegundos = 0,
                factoresSeleccionados = emptySet(),
                sensaciones = "",
                guardadoOk = false
            )
        }
    }

    fun onMostrarHistorial(mostrar: Boolean) = _uiState.update { it.copy(mostrarHistorial = mostrar) }

    companion object {
        fun Factory(
            tablaRepository: TablaPersonalizadaRepository,
            sesionRepository: SesionApneaRepository,
            sonido: SonidoManager
        ) = viewModelFactory {
            initializer { EntrenamientoApneaViewModel(tablaRepository, sesionRepository, sonido) }
        }
    }
}
