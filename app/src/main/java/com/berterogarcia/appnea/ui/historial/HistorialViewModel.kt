package com.berterogarcia.appnea.ui.historial

import java.time.Clock
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.dominio.modelo.Inmersion
import com.berterogarcia.appnea.dominio.modelo.TipoInmersion
import com.berterogarcia.appnea.dominio.repositorios.RepositorioInmersiones
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.text.Normalizer
import java.time.LocalDate
import java.time.YearMonth

/**
 * Filtros de período de la bitácora. [etiqueta] va en el chip y en su menú; [descripcion]
 * completa el texto "3 inmersiones …".
 */
enum class PeriodoFiltro(@param:StringRes val etiqueta: Int, @param:StringRes val descripcion: Int) {
    TODAS(R.string.periodo_todas, R.string.periodo_todas_desc),
    SIETE_DIAS(R.string.periodo_7_dias, R.string.periodo_7_dias_desc),
    UN_MES(R.string.periodo_1_mes, R.string.periodo_1_mes_desc),
    TRES_MESES(R.string.periodo_3_meses, R.string.periodo_3_meses_desc);

    /** Primera fecha incluida en el período (null = sin límite). */
    fun desde(hoy: LocalDate): LocalDate? = when (this) {
        TODAS -> null
        SIETE_DIAS -> hoy.minusDays(7)
        UN_MES -> hoy.minusMonths(1)
        TRES_MESES -> hoy.minusMonths(3)
    }
}

/** Inmersiones de un mes, para mostrarlas con su título ("Octubre 2026"). */
data class GrupoMes(val mes: YearMonth, val inmersiones: List<Inmersion>)

data class HistorialUiState(
    val cargando: Boolean = true,
    val error: Boolean = false,
    val totalGeneral: Int = 0,        // todas las del usuario, sin filtrar
    val cantidadFiltrada: Int = 0,    // las que pasan tipo + período + búsqueda
    val grupos: List<GrupoMes> = emptyList()
)

class HistorialViewModel(
    repository: RepositorioInmersiones,
    /** Para "este mes", "últimos 3 meses"... (en los tests se fija una fecha). */
    private val reloj: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val _periodo = MutableStateFlow(PeriodoFiltro.TODAS)
    val periodo: StateFlow<PeriodoFiltro> = _periodo.asStateFlow()

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    /** null = Todos */
    private val _tipo = MutableStateFlow<TipoInmersion?>(null)
    val tipo: StateFlow<TipoInmersion?> = _tipo.asStateFlow()

    val uiState: StateFlow<HistorialUiState> =
        combine(repository.todasLasInmersiones, _periodo, _busqueda, _tipo) { lista, periodo, busqueda, tipo ->
            armarEstado(lista, periodo, busqueda, hoy = LocalDate.now(reloj), tipo = tipo)
        }
            .flowOn(Dispatchers.Default) // filtrar y agrupar fuera del hilo principal (no traba la pantalla)
            .catch { emit(HistorialUiState(cargando = false, error = true)) } // Room falló: se avisa, no crashea
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistorialUiState())

    fun onPeriodoChange(periodo: PeriodoFiltro) {
        _periodo.value = periodo
    }

    fun onBusquedaChange(texto: String) {
        _busqueda.value = texto.take(MAX_BUSQUEDA)
    }

    fun onTipoChange(tipo: TipoInmersion?) {
        _tipo.value = tipo
    }

    fun limpiarFiltros() {
        _periodo.value = PeriodoFiltro.TODAS
        _busqueda.value = ""
        _tipo.value = null
    }

    companion object {

        private const val MAX_BUSQUEDA = 60
    }
}

/**
 * Filtra, ordena (fecha y hora, más reciente primero) y agrupa por mes.
 * Es una función pura: se puede probar con un test unitario sin Android.
 */
internal fun armarEstado(
    lista: List<Inmersion>,
    periodo: PeriodoFiltro,
    busqueda: String,
    hoy: LocalDate,
    tipo: TipoInmersion? = null
): HistorialUiState {
    val desde = periodo.desde(hoy)
    val termino = normalizar(busqueda.trim())

    val filtradas = lista
        .asSequence()
        .filter { tipo == null || it.tipo == tipo }
        .filter { desde == null || !it.fecha.isBefore(desde) }
        .filter { termino.isEmpty() || normalizar(it.lugar).contains(termino) }
        .sortedWith(compareByDescending<Inmersion> { it.fecha }.thenByDescending { it.horaEntrada })
        .toList()

    // groupBy respeta el orden de llegada, así que los meses quedan del más nuevo al más viejo
    val grupos = filtradas
        .groupBy { YearMonth.from(it.fecha) }
        .map { (mes, inmersiones) -> GrupoMes(mes, inmersiones) }

    return HistorialUiState(
        cargando = false,
        totalGeneral = lista.size,
        cantidadFiltrada = filtradas.size,
        grupos = grupos
    )
}

private val DIACRITICOS = Regex("\\p{Mn}+")

/** "Punta Marqués" → "punta marques": así buscar "marques" también la encuentra. */
private fun normalizar(texto: String): String =
    Normalizer.normalize(texto, Normalizer.Form.NFD).replace(DIACRITICOS, "").lowercase()