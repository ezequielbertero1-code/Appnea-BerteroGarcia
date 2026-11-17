package com.berterogarcia.appnea.ui.entrenamiento

import com.berterogarcia.appnea.util.intentar
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.Fase
import com.berterogarcia.appnea.data.local.SesionApneaEntity
import com.berterogarcia.appnea.dominio.FactorEntrenamiento
import com.berterogarcia.appnea.entrenamiento.MotorEntrenamiento
import com.berterogarcia.appnea.data.local.TablaApnea
import com.berterogarcia.appnea.data.local.TablaPersonalizadaEntity
import com.berterogarcia.appnea.data.local.TablasPredefinidas
import com.berterogarcia.appnea.data.local.TipoFase
import com.berterogarcia.appnea.data.repository.SesionApneaRepository
import com.berterogarcia.appnea.data.repository.TablaPersonalizadaRepository
import com.berterogarcia.appnea.ui.components.TextoUi
import com.berterogarcia.appnea.ui.components.textoUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EtapaEntrenamiento { SELECCION, EN_CURSO, GUARDANDO }


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
    val factoresSeleccionados: Set<FactorEntrenamiento> = emptySet(),
    val sensaciones: String = "",
    val guardando: Boolean = false,
    val guardadoOk: Boolean = false,
    val errorGuardado: TextoUi? = null
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
    private val motor: MotorEntrenamiento,
    /** Recuerda la tabla elegida (por nombre) si Android cierra la app en segundo plano. */
    private val estadoGuardado: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    /** Lo que es de la pantalla (elección, resultado a guardar). El timer vive en [motor]. */
    private val local = MutableStateFlow(EntrenamientoUiState())

    /**
     * Estado de la pantalla = lo local + lo que informa el motor. Si se vuelve a la pantalla con una
     * tabla corriendo (el ViewModel se recreó pero el motor siguió), se muestra la tabla del motor.
     */
    val uiState: StateFlow<EntrenamientoUiState> = combine(local, motor.estado) { l, m ->
        val resultado = m.resultado
        l.copy(
            tablaSeleccionada = m.tabla ?: l.tablaSeleccionada,
            etapa = when {
                m.corriendo -> EtapaEntrenamiento.EN_CURSO
                resultado != null -> EtapaEntrenamiento.GUARDANDO
                else -> EtapaEntrenamiento.SELECCION
            },
            corriendo = m.corriendo,
            indiceFase = m.indiceFase,
            segundosRestantes = m.segundosRestantes,
            finFaseMs = m.finFaseMs,
            completada = resultado?.completada ?: false,
            faseAlcanzada = resultado?.faseAlcanzada ?: 0,
            marcaSegundos = resultado?.marcaSegundos ?: 0,
            duracionSesionSegundos = resultado?.duracionSegundos ?: 0
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, EntrenamientoUiState())

    init {
        val nombreGuardado = estadoGuardado.get<String>(CLAVE_TABLA)
        local.value.tablasPredefinidas.firstOrNull { it.nombre == nombreGuardado }?.let { tabla ->
            local.update { it.copy(tablaSeleccionada = tabla) }
        }
        viewModelScope.launch {
            tablaRepository.todasLasTablas
                .catch { emit(emptyList()) } // si falla la base: sin tablas propias, sin crashear
                .collect { tablas ->
                    local.update { estado ->
                        // Una tabla propia elegida antes de que Android cerrara la app: se recupera al cargar la lista
                        val recuperada = if (estado.tablaSeleccionada == null && nombreGuardado != null) {
                            tablas.firstOrNull { it.nombre == nombreGuardado }?.aTablaApnea()
                        } else null
                        estado.copy(tablasPersonalizadas = tablas, tablaSeleccionada = recuperada ?: estado.tablaSeleccionada)
                    }
                }
        }
    }

    fun onSeleccionarTabla(tabla: TablaApnea) {
        if (motor.estado.value.corriendo) return
        local.update { it.copy(tablaSeleccionada = tabla) }
        estadoGuardado[CLAVE_TABLA] = tabla.nombre
    }

    fun onAcompanadoChange(acompanado: Boolean) = local.update { it.copy(acompanado = acompanado) }

    fun onEliminarTablaPersonalizada(tabla: TablaPersonalizadaEntity) {
        // Si era la elegida, se deselecciona para no iniciar una tabla que ya no existe
        if (local.value.tablaSeleccionada == tabla.aTablaApnea()) {
            local.update { it.copy(tablaSeleccionada = null) }
            estadoGuardado.remove<String>(CLAVE_TABLA)
        }
        viewModelScope.launch { intentar("EntrenamientoApneaViewModel") { tablaRepository.eliminar(tabla) } }
    }

    fun onIniciar() {
        val tabla = local.value.tablaSeleccionada ?: return
        if (motor.estado.value.corriendo || tabla.fases.isEmpty()) return
        local.update {
            it.copy(
                factoresSeleccionados = if (it.acompanado) setOf(FactorEntrenamiento.ACOMPANADO) else emptySet(),
                sensaciones = "",
                guardando = false,
                guardadoOk = false,
                errorGuardado = null
            )
        }
        motor.iniciar(tabla)
    }

    fun onAbortar() = motor.abortar()

    fun onFactorToggle(factor: FactorEntrenamiento, marcado: Boolean) = local.update {
        it.copy(
            factoresSeleccionados = if (marcado) it.factoresSeleccionados + factor
            else it.factoresSeleccionados - factor
        )
    }

    fun onSensacionesChange(v: String) = local.update { it.copy(sensaciones = v) }

    fun onGuardarSesion() {
        val s = uiState.value
        val tabla = s.tablaSeleccionada ?: return
        val resultado = motor.estado.value.resultado ?: return
        if (s.guardando || s.guardadoOk) return // que un doble toque no guarde dos veces
        local.update { it.copy(guardando = true, errorGuardado = null) }
        viewModelScope.launch {
            // guardar() devuelve false si no hay sesión iniciada; si la base falla, da false
            val ok = intentar("EntrenamientoApneaViewModel") {
                sesionRepository.guardar(
                    SesionApneaEntity(
                        fecha = resultado.inicio.toLocalDate(),
                        horaInicio = resultado.inicio.toLocalTime().withNano(0),
                        duracionSegundos = resultado.duracionSegundos,
                        nombreTabla = tabla.nombre,
                        completada = resultado.completada,
                        faseAlcanzada = resultado.faseAlcanzada,
                        totalFases = tabla.fases.size,
                        marcaSegundos = resultado.marcaSegundos,
                        factores = s.factoresSeleccionados,
                        sensaciones = s.sensaciones.trim()
                    )
                )
            }.getOrDefault(false)
            local.update {
                if (ok) it.copy(guardando = false, guardadoOk = true)
                else it.copy(guardando = false, errorGuardado = textoUi(R.string.entrenar_error_guardar))
            }
        }
    }

    /** Vuelve a la lista de tablas. Mantiene la tabla elegida y el tilde de "Estoy acompañado". */
    fun onVolverASeleccion() {
        motor.reiniciar()
        local.update {
            it.copy(factoresSeleccionados = emptySet(), sensaciones = "", guardando = false, guardadoOk = false, errorGuardado = null)
        }
    }

    private companion object {
        const val CLAVE_TABLA = "tabla_elegida"
    }
}
