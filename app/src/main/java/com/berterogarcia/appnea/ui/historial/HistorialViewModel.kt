package com.berterogarcia.appnea.ui.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.data.local.InmersionEntity
import com.berterogarcia.appnea.data.repository.InmersionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.Normalizer
import java.time.LocalDate
import java.time.YearMonth

/** Filtros de período de la bitácora. [descripcion] completa el texto "3 inmersiones …". */
enum class PeriodoFiltro(val etiqueta: String, val descripcion: String) {
    TODAS("Todas", "registradas"),
    SIETE_DIAS("7 días", "en los últimos 7 días"),
    UN_MES("1 mes", "en el último mes"),
    TRES_MESES("3 meses", "en los últimos 3 meses");

    /** Primera fecha incluida en el período (null = sin límite). */
    fun desde(hoy: LocalDate): LocalDate? = when (this) {
        TODAS -> null
        SIETE_DIAS -> hoy.minusDays(7)
        UN_MES -> hoy.minusMonths(1)
        TRES_MESES -> hoy.minusMonths(3)
    }
}

/** Inmersiones de un mes, para mostrarlas con su título ("Octubre 2026"). */
data class GrupoMes(val mes: YearMonth, val inmersiones: List<InmersionEntity>)

data class HistorialUiState(
    val cargando: Boolean = true,
    val error: Boolean = false,
    val totalGeneral: Int = 0,        // todas las del usuario, sin filtrar
    val cantidadFiltrada: Int = 0,    // las que pasan período + búsqueda
    val grupos: List<GrupoMes> = emptyList()
)

class HistorialViewModel(repository: InmersionRepository) : ViewModel() {

    private val _periodo = MutableStateFlow(PeriodoFiltro.TODAS)
    val periodo: StateFlow<PeriodoFiltro> = _periodo.asStateFlow()

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    val uiState: StateFlow<HistorialUiState> =
        combine(repository.todasLasInmersiones, _periodo, _busqueda) { lista, periodo, busqueda ->
            armarEstado(lista, periodo, busqueda, hoy = LocalDate.now())
        }
            .catch { emit(HistorialUiState(cargando = false, error = true)) } // Room falló: se avisa, no crashea
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistorialUiState())

    fun onPeriodoChange(periodo: PeriodoFiltro) {
        _periodo.value = periodo
    }

    fun onBusquedaChange(texto: String) {
        _busqueda.value = texto.take(MAX_BUSQUEDA)
    }

    fun limpiarFiltros() {
        _periodo.value = PeriodoFiltro.TODAS
        _busqueda.value = ""
    }

    companion object {
        private const val MAX_BUSQUEDA = 60

        fun Factory(repository: InmersionRepository) = viewModelFactory {
            initializer { HistorialViewModel(repository) }
        }
    }
}

/**
 * Filtra, ordena (fecha y hora, más reciente primero) y agrupa por mes.
 * Es una función pura: se puede probar con un test unitario sin Android.
 */
internal fun armarEstado(
    lista: List<InmersionEntity>,
    periodo: PeriodoFiltro,
    busqueda: String,
    hoy: LocalDate
): HistorialUiState {
    val desde = periodo.desde(hoy)
    val termino = normalizar(busqueda.trim())

    val filtradas = lista
        .asSequence()
        .filter { desde == null || !it.fecha.isBefore(desde) }
        .filter { termino.isEmpty() || normalizar(it.lugar).contains(termino) }
        .sortedWith(compareByDescending<InmersionEntity> { it.fecha }.thenByDescending { it.horaEntrada })
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