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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

enum class EtapaEntrenamiento { SELECCION, EN_CURSO, GUARDANDO }

/** Factor que se pre-marca en el resultado si al iniciar se tildó "Estoy acompañado". */
const val FACTOR_ACOMPANADO = "Entrené acompañado"

val FACTORES_DISPONIBLES = listOf(
    "Dormí bien",
    "Comí liviano",
    "Buena hidratación",
    "Estrés / ansiedad",
    "Cansancio físico",
    FACTOR_ACOMPANADO
)

data class EntrenamientoUiState(
    val tablasPredefinidas: List<TablaApnea> = TablasPredefinidas.listaBase(),
    val tablasPersonalizadas: List<TablaPersonalizadaEntity> = emptyList(),
    val tablaSeleccionada: TablaApnea? = null,
    /**
     * "Estoy acompañado": si está tildado no se muestra el aviso de seguridad.
     * Vive solo en memoria (no se guarda): al volver a abrir la app arranca destildado.
     */
    val acompanado: Boolean = false,
    val etapa: EtapaEntrenamiento = EtapaEntrenamiento.SELECCION,
    val indiceFase: Int = 0,
    val segundosRestantes: Int = 0,
    /** Instante (SystemClock.elapsedRealtime) en que termina la fase actual: para dibujar el anillo continuo. */
    val finFaseMs: Long = 0L,
    val corriendo: Boolean = false,
    val completada: Boolean = false,
    val faseAlcanzada: Int = 0,
    // La marca se sigue calculando y guardando: la va a usar la pantalla de Estadísticas
    val marcaSegundos: Int = 0,
    val duracionSesionSegundos: Int = 0,
    val factoresSeleccionados: Set<String> = emptySet(),
    val sensaciones: String = "",
    val guardando: Boolean = false,
    val guardadoOk: Boolean = false,
    val errorGuardado: String? = null
) {
    val faseActual: Fase? get() = tablaSeleccionada?.fases?.getOrNull(indiceFase)
    val totalFases: Int get() = tablaSeleccionada?.fases?.size ?: 0
    val duracionTabla: Int get() = tablaSeleccionada?.fases?.sumOf { it.duracionSegundos } ?: 0

    /** Segundos desde que arrancó la tabla (fases terminadas + lo que va de la actual). */
    val segundosTranscurridos: Int
        get() {
            val fases = tablaSeleccionada?.fases ?: return 0
            val previas = fases.take(indiceFase).sumOf { it.duracionSegundos }
            val actual = fases.getOrNull(indiceFase)?.duracionSegundos ?: 0
            return previas + (actual - segundosRestantes).coerceAtLeast(0)
        }
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
    private var inicioTablaMs = 0L
    // Momento (reloj monotónico) en que se tocó "Abortar": para medir exacto la apnea interrumpida
    private var momentoAbortoMs: Long? = null

    init {
        viewModelScope.launch {
            tablaRepository.todasLasTablas
                .catch { emit(emptyList()) } // si falla la base: sin tablas propias, sin crashear
                .collect { tablas -> _uiState.update { it.copy(tablasPersonalizadas = tablas) } }
        }
    }

    fun onSeleccionarTabla(tabla: TablaApnea) {
        if (_uiState.value.corriendo) return
        _uiState.update { it.copy(tablaSeleccionada = tabla) }
    }

    fun onAcompanadoChange(acompanado: Boolean) = _uiState.update { it.copy(acompanado = acompanado) }

    fun onEliminarTablaPersonalizada(tabla: TablaPersonalizadaEntity) {
        // Si era la elegida, se deselecciona para no iniciar una tabla que ya no existe
        if (_uiState.value.tablaSeleccionada == tabla.aTablaApnea()) {
            _uiState.update { it.copy(tablaSeleccionada = null) }
        }
        viewModelScope.launch { runCatching { tablaRepository.eliminar(tabla) } }
    }

    fun onIniciar() {
        val s = _uiState.value
        val tabla = s.tablaSeleccionada ?: return
        if (s.corriendo || tabla.fases.isEmpty()) return

        mejorApneaSegundos = 0
        momentoAbortoMs = null
        inicioTablaMs = SystemClock.elapsedRealtime()
        _uiState.update {
            it.copy(
                etapa = EtapaEntrenamiento.EN_CURSO,
                corriendo = true,
                indiceFase = 0,
                segundosRestantes = tabla.fases.first().duracionSegundos,
                finFaseMs = inicioTablaMs + tabla.fases.first().duracionSegundos * 1000L,
                completada = false,
                faseAlcanzada = 0,
                marcaSegundos = 0,
                duracionSesionSegundos = 0,
                factoresSeleccionados = if (it.acompanado) setOf(FACTOR_ACOMPANADO) else emptySet(),
                sensaciones = "",
                guardando = false,
                guardadoOk = false,
                errorGuardado = null
            )
        }

        job?.cancel() // por si quedó una corrida vieja
        job = viewModelScope.launch {
            // Cada fase termina en un instante FIJO calculado desde el inicio de la tabla.
            // Lo que tarden los sonidos o la UI en cada vuelta no se acumula: el tiempo restante
            // siempre se calcula contra el reloj, no contando vueltas.
            var finFaseMs = inicioTablaMs

            for ((indice, fase) in tabla.fases.withIndex()) {
                val inicioFaseMs = finFaseMs
                finFaseMs = inicioFaseMs + fase.duracionSegundos * 1000L

                _uiState.update {
                    it.copy(indiceFase = indice, segundosRestantes = fase.duracionSegundos, finFaseMs = finFaseMs)
                }
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
                    finalizarSesion(completada = false, faseAlcanzada = indice + 1, finMs = hasta)
                    return@launch
                }

                if (fase.tipo == TipoFase.APNEA) {
                    mejorApneaSegundos = maxOf(mejorApneaSegundos, fase.duracionSegundos)
                }
            }
            sonido.avisarFinTabla()
            finalizarSesion(completada = true, faseAlcanzada = tabla.fases.size, finMs = finFaseMs)
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

    private fun finalizarSesion(completada: Boolean, faseAlcanzada: Int, finMs: Long) {
        val duracion = ((finMs - inicioTablaMs) / 1000L).toInt().coerceAtLeast(0)
        _uiState.update {
            it.copy(
                corriendo = false,
                etapa = EtapaEntrenamiento.GUARDANDO,
                completada = completada,
                faseAlcanzada = faseAlcanzada,
                marcaSegundos = mejorApneaSegundos,
                duracionSesionSegundos = duracion
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
        if (s.guardando || s.guardadoOk) return // que un doble toque no guarde dos veces
        _uiState.update { it.copy(guardando = true, errorGuardado = null) }

        viewModelScope.launch {
            // guardar() devuelve false si no hay sesión iniciada; si la base falla, runCatching da false
            val ok = runCatching {
                sesionRepository.guardar(
                    SesionApneaEntity(
                        fecha = LocalDate.now(),
                        nombreTabla = tabla.nombre,
                        completada = s.completada,
                        faseAlcanzada = s.faseAlcanzada,
                        totalFases = tabla.fases.size,
                        marcaSegundos = s.marcaSegundos,
                        factores = s.factoresSeleccionados,
                        sensaciones = s.sensaciones.trim()
                    )
                )
            }.getOrDefault(false)
            _uiState.update {
                if (ok) it.copy(guardando = false, guardadoOk = true)
                else it.copy(guardando = false, errorGuardado = "No se pudo guardar la sesión. Probá de nuevo.")
            }
        }
    }

    /** Vuelve a la lista de tablas. Mantiene la tabla elegida y el tilde de "Estoy acompañado". */
    fun onVolverASeleccion() {
        job?.cancel()
        _uiState.update {
            it.copy(
                etapa = EtapaEntrenamiento.SELECCION,
                corriendo = false,
                indiceFase = 0,
                segundosRestantes = 0,
                completada = false,
                faseAlcanzada = 0,
                marcaSegundos = 0,
                duracionSesionSegundos = 0,
                factoresSeleccionados = emptySet(),
                sensaciones = "",
                guardando = false,
                guardadoOk = false,
                errorGuardado = null
            )
        }
    }

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
