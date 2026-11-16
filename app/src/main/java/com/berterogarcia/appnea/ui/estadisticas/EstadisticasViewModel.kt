package com.berterogarcia.appnea.ui.estadisticas

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.InmersionEntity
import com.berterogarcia.appnea.data.local.SesionApneaEntity
import com.berterogarcia.appnea.data.local.TipoInmersion
import com.berterogarcia.appnea.data.repository.InmersionRepository
import com.berterogarcia.appnea.data.repository.SesionApneaRepository
import com.berterogarcia.appnea.dominio.FactorEntrenamiento
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration

/** Período de las estadísticas. [meses] null = todo. */
enum class PeriodoEstadisticas(@param:StringRes val etiqueta: Int, val meses: Long?) {
    UN_MES(R.string.estadisticas_periodo_1_mes, 1),
    TRES_MESES(R.string.estadisticas_periodo_3_meses, 3),
    UN_ANIO(R.string.estadisticas_periodo_1_anio, 12),
    TODO(R.string.estadisticas_periodo_todo, null);

    fun desde(hoy: LocalDate): LocalDate? = meses?.let { hoy.minusMonths(it) }

    /** Cuántos meses muestra el gráfico "por mes" (con 1 o 3 meses quedaría casi vacío). */
    val mesesGrafico: Int get() = if (meses != null && meses <= 3) 6 else 12
}

/** Mínimo de sesiones con un factor para mostrar su porcentaje (con menos, no dice nada). */
const val MIN_SESIONES_FACTOR = 3

data class LugarFrecuente(val lugar: String, val cantidad: Int)
data class InfluenciaFactor(val factor: FactorEntrenamiento, val porcentajeCompletas: Int, val sesiones: Int)

data class ResumenInmersiones(
    val cantidad: Int = 0,
    val record: InmersionEntity? = null,
    val tiempoTotal: Duration = Duration.ZERO,
    val profundidadPromedio: Float = 0f,
    /** Ordenadas de la más vieja a la más nueva (para la línea). */
    val serie: List<InmersionEntity> = emptyList(),
    val porMes: List<Pair<YearMonth, Int>> = emptyList(),
    val lugares: List<LugarFrecuente> = emptyList(),
    /** Inmersiones de apnea con registro de bajadas (las viejas no lo tienen). */
    val inmersionesApnea: Int = 0,
    val bajadasTotales: Int = 0,
    val sincopes: Int = 0
)

data class ResumenEntrenamiento(
    val sesiones: Int = 0,
    val completas: Int = 0,
    val porcentajeCompletas: Int = 0,
    val apneaMasLargaSegundos: Int = 0,
    /** Sesiones con marca > 0, de la más vieja a la más nueva (para la línea). */
    val serie: List<SesionApneaEntity> = emptyList(),
    val factores: List<InfluenciaFactor> = emptyList()
) {
    /** El factor que más se aleja del promedio (para la frase destacada). */
    val factorDestacado: InfluenciaFactor?
        get() = factores.maxByOrNull { abs(it.porcentajeCompletas - porcentajeCompletas) }
            ?.takeIf { it.porcentajeCompletas != porcentajeCompletas }
}

data class EstadisticasUiState(
    val cargando: Boolean = true,
    val error: Boolean = false,
    val periodo: PeriodoEstadisticas = PeriodoEstadisticas.TRES_MESES,
    val tipo: TipoInmersion? = null,          // null = todas (solo afecta a Inmersiones)
    val totalInmersiones: Int = 0,            // sin filtros: para saber si hay algo cargado
    val totalSesiones: Int = 0,
    val inmersiones: ResumenInmersiones = ResumenInmersiones(),
    val entrenamiento: ResumenEntrenamiento = ResumenEntrenamiento()
)

class EstadisticasViewModel(
    inmersionRepository: InmersionRepository,
    sesionRepository: SesionApneaRepository
) : ViewModel() {

    private val _periodo = MutableStateFlow(PeriodoEstadisticas.TRES_MESES)

    private val _tipo = MutableStateFlow<TipoInmersion?>(null)

    val uiState: StateFlow<EstadisticasUiState> = combine(
        inmersionRepository.todasLasInmersiones,
        sesionRepository.todasLasSesiones,
        _periodo,
        _tipo
    ) { inmersiones, sesiones, periodo, tipo ->
        val hoy = LocalDate.now()
        EstadisticasUiState(
            cargando = false,
            periodo = periodo,
            tipo = tipo,
            totalInmersiones = inmersiones.size,
            totalSesiones = sesiones.size,
            inmersiones = resumirInmersiones(inmersiones, periodo, tipo, hoy),
            entrenamiento = resumirEntrenamiento(sesiones, periodo, hoy)
        )
    }
        .flowOn(Dispatchers.Default) // calcular fuera del hilo principal (no traba la pantalla)
        .catch { emit(EstadisticasUiState(cargando = false, error = true)) } // si falla la base: aviso, sin crashear
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadisticasUiState())

    fun onPeriodoChange(periodo: PeriodoEstadisticas) {
        _periodo.value = periodo
    }

    fun onTipoChange(tipo: TipoInmersion?) {
        _tipo.value = tipo
    }

}

// ============ Cálculos: funciones puras (se pueden probar con un test sin Android) ============

internal fun resumirInmersiones(
    todas: List<InmersionEntity>,
    periodo: PeriodoEstadisticas,
    tipo: TipoInmersion?,
    hoy: LocalDate
): ResumenInmersiones {
    val delTipo = todas.filter { tipo == null || it.tipo == tipo }
    val desde = periodo.desde(hoy)
    val filtradas = delTipo
        .filter { desde == null || !it.fecha.isBefore(desde) }
        .sortedWith(compareBy<InmersionEntity> { it.fecha }.thenBy { it.horaEntrada })

    // El gráfico por mes usa sus propios meses (6 o 12 hasta hoy), respetando el tipo
    val mesActual = YearMonth.from(hoy)
    val conteo = delTipo.groupingBy { YearMonth.from(it.fecha) }.eachCount()
    val porMes = (periodo.mesesGrafico - 1 downTo 0).map { atras ->
        val mes = mesActual.minusMonths(atras.toLong())
        mes to (conteo[mes] ?: 0)
    }

    val lugares = filtradas
        .filter { it.lugar.isNotBlank() }
        .groupingBy { it.lugar.trim() }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(3)
        .map { LugarFrecuente(it.key, it.value) }

    return ResumenInmersiones(
        cantidad = filtradas.size,
        record = filtradas.maxByOrNull { it.profundidadMetros },
        tiempoTotal = filtradas.fold(Duration.ZERO) { acc, i -> acc + i.tiempoFondo },
        profundidadPromedio = if (filtradas.isEmpty()) 0f
        else filtradas.map { it.profundidadMetros }.average().toFloat(),
        serie = filtradas,
        porMes = porMes,
        lugares = lugares,
        inmersionesApnea = filtradas.count { it.apnea != null },
        bajadasTotales = filtradas.sumOf { it.apnea?.cantidadBajadas ?: 0 },
        sincopes = filtradas.count { it.apnea?.sincope == true }
    )
}

internal fun resumirEntrenamiento(
    todas: List<SesionApneaEntity>,
    periodo: PeriodoEstadisticas,
    hoy: LocalDate
): ResumenEntrenamiento {
    val desde = periodo.desde(hoy)
    val filtradas = todas
        .filter { desde == null || !it.fecha.isBefore(desde) }
        .sortedWith(compareBy<SesionApneaEntity> { it.fecha }.thenBy { it.id })

    fun porcentaje(lista: List<SesionApneaEntity>): Int =
        if (lista.isEmpty()) 0 else (lista.count { it.completada } * 100f / lista.size).roundToInt()

    val factores = FactorEntrenamiento.entries.mapNotNull { factor ->
        val conFactor = filtradas.filter { factor in it.factores }
        if (conFactor.size < MIN_SESIONES_FACTOR) null
        else InfluenciaFactor(factor, porcentaje(conFactor), conFactor.size)
    }.sortedByDescending { it.porcentajeCompletas }

    return ResumenEntrenamiento(
        sesiones = filtradas.size,
        completas = filtradas.count { it.completada },
        porcentajeCompletas = porcentaje(filtradas),
        apneaMasLargaSegundos = filtradas.maxOfOrNull { it.marcaSegundos } ?: 0,
        serie = filtradas.filter { it.marcaSegundos > 0 },
        factores = factores
    )
}
