package com.berterogarcia.appnea.ui.nuevainmersion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.berterogarcia.appnea.data.local.InmersionEntity
import com.berterogarcia.appnea.data.repository.InmersionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class Campo { FECHA, LUGAR, PROFUNDIDAD, PARADA, TIEMPO_FONDO, HORA_ENTRADA, HORA_SALIDA, LASTRE, PROTECCION, TEMP_AIRE, TEMP_AGUA, VISIBILIDAD }

private val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.US)
private val REGEX_TIEMPO_FONDO = Regex("^\\d{2}[0-5]\\d\$") // 4 dígitos, segundos 00-59

private fun parseTiempoFondo(v: String): Duration {
    val digitos = v.padStart(4, '0')
    val minutos = digitos.substring(0, 2).toInt()
    val segundos = digitos.substring(2, 4).toInt()
    return (minutos * 60 + segundos).seconds
}

data class NuevaInmersionUiState(
    val fecha: String = "",
    val lugar: String = "",
    val profundidadMetros: String = "",
    val paradaSeguridadMin: String = "",
    val tiempoFondo: String = "",
    val horaEntrada: String = "",
    val horaSalida: String = "",
    val caracteristicasAgua: Set<String> = emptySet(),
    val lastreKg: String = "",
    val proteccionMm: String = "",
    val equipamiento: Set<String> = emptySet(),
    val temperaturaAireC: String = "",
    val temperaturaAguaC: String = "",
    val visibilidadMetros: String = "",
    val actividad: String = "",
    val comentarios: String = "",
    val guardadoOk: Boolean = false,
    val errores: Map<Campo, String> = emptyMap(),
)

class NuevaInmersionViewModel(
    private val repository: InmersionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        NuevaInmersionUiState(fecha = LocalDate.now().format(FORMATO_FECHA)))
    val uiState: StateFlow<NuevaInmersionUiState> = _uiState

    fun onFechaChange(v: String) = _uiState.update { it.copy(fecha = v) }
    fun onLugarChange(v: String) = _uiState.update { it.copy(lugar = v) }
    fun onProfundidadChange(v: String) = _uiState.update { it.copy(profundidadMetros = v) }
    fun onParadaChange(v: String) = _uiState.update { it.copy(paradaSeguridadMin = v.filter { c -> c.isDigit() || c == '.'}) }
    fun onTiempoFondoChange(v: String) = _uiState.update { it.copy(tiempoFondo = v.filter { c -> c.isDigit() }.take(4)) }
    fun onHoraEntradaChange(v: String) = _uiState.update { it.copy(horaEntrada = v) }
    fun onHoraSalidaChange(v: String) = _uiState.update { it.copy(horaSalida = v) }
    fun onLastreChange(v: String) = _uiState.update { it.copy(lastreKg = v.filter { c -> c.isDigit() }) }
    fun onProteccionMmChange(v: String) = _uiState.update { it.copy(proteccionMm = v.filter { c -> c.isDigit() }) }
    fun onTempAireChange(v: String) = _uiState.update { it.copy(temperaturaAireC = v.filter { c -> c.isDigit() }) }
    fun onTempAguaChange(v: String) = _uiState.update { it.copy(temperaturaAguaC = v.filter { c -> c.isDigit() }) }
    fun onVisibilidadChange(v: String) = _uiState.update { it.copy(visibilidadMetros = v.filter { c -> c.isDigit() }) }
    fun onActividadChange(v: String) = _uiState.update { it.copy(actividad = v) }
    fun onComentariosChange(v: String) = _uiState.update { it.copy(comentarios = v) }

    fun onAguaToggle(opcion: String, marcado: Boolean) = _uiState.update {
        it.copy(
            caracteristicasAgua = if (marcado) it.caracteristicasAgua + opcion
            else it.caracteristicasAgua - opcion
        )
    }

    fun onEquipamientoToggle(opcion: String, marcado: Boolean) = _uiState.update {
        it.copy(
            equipamiento = if (marcado) it.equipamiento + opcion
            else it.equipamiento - opcion
        )
    }

    fun guardar() {
        val s = _uiState.value
        val errores = validar(s)
        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(errores = errores) }
            return
        }
        viewModelScope.launch {
            repository.guardar(
                InmersionEntity(
                    fecha = LocalDate.parse(s.fecha, FORMATO_FECHA),
                    lugar = s.lugar,
                    profundidadMetros = s.profundidadMetros.toFloat(), // ya validado, no hace falta toFloatOrNull
                    paradaSeguridadMin = s.paradaSeguridadMin.toFloatOrNull() ?: 0f,
                    tiempoFondo = parseTiempoFondo(s.tiempoFondo), // "mm:ss" -> Duration
                    horaEntrada = LocalTime.parse(s.horaEntrada),  // "HH:mm" -> LocalTime
                    horaSalida = LocalTime.parse(s.horaSalida),
                    caracteristicasAgua = s.caracteristicasAgua,
                    lastreKg = s.lastreKg.toFloatOrNull() ?: 0f,
                    proteccionMm = s.proteccionMm.toFloatOrNull() ?: 0f,
                    equipamiento = s.equipamiento,
                    temperaturaAireC = s.temperaturaAireC.toFloatOrNull() ?: 0f,
                    temperaturaAguaC = s.temperaturaAguaC.toFloatOrNull() ?: 0f,
                    visibilidadMetros = s.visibilidadMetros.toFloatOrNull() ?: 0f,
                    actividad = s.actividad,
                    comentarios = s.comentarios
                )
            )
            _uiState.update { it.copy(guardadoOk = true) }
        }
    }

    private fun parseTiempoFondo(v: String): Duration {
        val partes = v.split(":").mapNotNull { it.toIntOrNull() }
        val minutos = partes.getOrElse(0) { 0 }
        val segundos = partes.getOrElse(1) { 0 }
        return (minutos * 60 + segundos).seconds
    }

    private fun validar(s: NuevaInmersionUiState): Map<Campo, String> {
        val errores = mutableMapOf<Campo, String>()
        if (s.fecha.isBlank()) errores[Campo.FECHA] = "Ingresá una fecha."
        if (s.lugar.isBlank()) errores[Campo.LUGAR] = "Ingresá un lugar."
        if (s.profundidadMetros.toFloatOrNull() == null) errores[Campo.PROFUNDIDAD] = "Profundidad inválida."

        val parada = s.paradaSeguridadMin.toFloatOrNull()
        if (parada == null || parada < 3f) errores[Campo.PARADA] = "Mínimo 3 minutos."

        if (!REGEX_TIEMPO_FONDO.matches(s.tiempoFondo)) errores[Campo.TIEMPO_FONDO] = "Formato esperado mm:ss."

        val horaEntrada = s.horaEntrada.takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        val horaSalida = s.horaSalida.takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

        if (horaEntrada == null) errores[Campo.HORA_ENTRADA] = "Elegí la hora de entrada"
        if (horaSalida == null) errores[Campo.HORA_SALIDA] = "Elegí la hora de salida"
        if (horaEntrada != null && horaSalida != null && horaEntrada.isAfter(horaSalida)) {
            errores[Campo.HORA_SALIDA] = "Debe ser posterior a la hora de entrada"
        }

        val lastre = s.lastreKg.toFloatOrNull()
        if (lastre == null || lastre < 0f) errores[Campo.LASTRE] = "Lastre minimo 0kg."

        val proteccion = s.proteccionMm.toFloatOrNull()
        if (proteccion == null || proteccion < 0f) errores[Campo.PROTECCION] = "Proteccion minima 0mm."

        val temperaturaAire = s.temperaturaAireC.toFloatOrNull()
        if (temperaturaAire == null || temperaturaAire < 0f) errores[Campo.TEMP_AIRE] = "Temperatura mínima 0°C."
            else if (temperaturaAire > 40f) errores[Campo.TEMP_AIRE] = "Temperatura máxima 40°C."

        val temperaturaAgua = s.temperaturaAguaC.toFloatOrNull()
        if (temperaturaAgua == null || temperaturaAgua < 0f) errores[Campo.TEMP_AGUA] = "Temperatura mínima -10°C."
            else if (temperaturaAgua > 40f) errores[Campo.TEMP_AGUA] = "Temperatura máxima 10°C."

        val visibilidad = s.visibilidadMetros.toFloatOrNull()
        if (visibilidad == null || visibilidad < 0f) errores[Campo.VISIBILIDAD] = "Visibilidad minima 0m."
        return errores
    }

    companion object {
        fun Factory(repository: InmersionRepository) = viewModelFactory {
            initializer { NuevaInmersionViewModel(repository) }
        }
    }
}
