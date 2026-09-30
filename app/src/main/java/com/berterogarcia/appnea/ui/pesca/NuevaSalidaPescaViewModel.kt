package com.berterogarcia.appnea.ui.pesca

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.data.condiciones.CondicionesRepository
import com.berterogarcia.appnea.data.local.CapturaPesca
import com.berterogarcia.appnea.data.local.UBICACION_COMODORO_RIVADAVIA
import com.berterogarcia.appnea.data.local.FotoHelper
import com.berterogarcia.appnea.data.local.SalidaPescaEntity
import com.berterogarcia.appnea.data.local.Ubicacion
import com.berterogarcia.appnea.data.local.UbicacionHelper
import com.berterogarcia.appnea.data.repository.SalidaPescaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class CampoSalida { FECHA, LUGAR, CAPTURAS }

private val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.US)

data class NuevaSalidaUiState(
    val fecha: String = LocalDate.now().format(FORMATO_FECHA),
    val lugar: String = "",
    val latitud: Double? = null,
    val longitud: Double? = null,
    val capturas: List<CapturaPesca> = emptyList(),
    val fotoPath: String? = null,
    val comentario: String = "",
    val guardadoOk: Boolean = false,
    val guardando: Boolean = false,
    val errores: Map<CampoSalida, String> = emptyMap(),
    val mensajeUbicacion: String? = null
)

class NuevaSalidaPescaViewModel(
    private val repository: SalidaPescaRepository,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(NuevaSalidaUiState())
    val uiState: StateFlow<NuevaSalidaUiState> = _uiState

    fun onFechaChange(v: String) = _uiState.update { it.copy(fecha = v) }
    fun onLugarChange(v: String) = _uiState.update { it.copy(lugar = v) }
    fun onComentarioChange(v: String) = _uiState.update { it.copy(comentario = v) }

    fun agregarCaptura(especie: String, cantidad: Int, pesoKg: Double?) {
        if (especie.isBlank() || cantidad <= 0) return
        _uiState.update { it.copy(capturas = it.capturas + CapturaPesca(especie.trim(), cantidad, pesoKg)) }
    }

    fun quitarCaptura(indice: Int) {
        _uiState.update { it.copy(capturas = it.capturas.toMutableList().apply { removeAt(indice) }) }
    }

    fun usarUbicacionActual() {
        if (!UbicacionHelper.tienePermiso(appContext)) {
            _uiState.update { it.copy(mensajeUbicacion = "Necesitás dar permiso de ubicación para usar esta opción.") }
            return
        }
        val ubicacion: Ubicacion? = UbicacionHelper.obtenerUltimaUbicacion(appContext)
        if (ubicacion == null) {
            _uiState.update {
                it.copy(mensajeUbicacion = "No se pudo obtener tu ubicación todavía. Probá de nuevo en unos segundos.")
            }
            return
        }
        _uiState.update {
            it.copy(
                latitud = ubicacion.latitud,
                longitud = ubicacion.longitud,
                mensajeUbicacion = "Ubicación guardada: ${"%.4f".format(ubicacion.latitud)}, ${"%.4f".format(ubicacion.longitud)}"
            )
        }
    }

    fun onFotoSeleccionada(uri: Uri) {
        val path = FotoHelper.copiarACarpetaInterna(appContext, uri)
        _uiState.update { it.copy(fotoPath = path) }
    }

    fun guardar() {
        val s = _uiState.value
        val errores = validar(s)
        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(errores = errores) }
            return
        }
        if (s.guardando) return
        _uiState.update { it.copy(guardando = true, errores = emptyMap()) }
        viewModelScope.launch {
            val fecha = LocalDate.parse(s.fecha, FORMATO_FECHA)
            // Momento de referencia: ahora si la salida es de hoy, si no las 10:00 de ese día
            val momento = if (fecha == LocalDate.now()) LocalDateTime.now() else fecha.atTime(10, 0)
            val lat = s.latitud ?: UBICACION_COMODORO_RIVADAVIA.latitud
            val lon = s.longitud ?: UBICACION_COMODORO_RIVADAVIA.longitud
            val condiciones = runCatching { CondicionesRepository.registroPara(lat, lon, momento) }.getOrNull()
            repository.guardar(
                SalidaPescaEntity(
                    fecha = fecha,
                    lugar = s.lugar.trim(),
                    latitud = s.latitud,
                    longitud = s.longitud,
                    capturas = s.capturas,
                    fotoPath = s.fotoPath,
                    comentario = s.comentario.trim(),
                    condiciones = condiciones
                )
            )
            _uiState.update { it.copy(guardadoOk = true, guardando = false) }
        }
    }

    private fun validar(s: NuevaSalidaUiState): Map<CampoSalida, String> {
        val errores = mutableMapOf<CampoSalida, String>()
        if (s.fecha.isBlank() || runCatching { LocalDate.parse(s.fecha, FORMATO_FECHA) }.isFailure) {
            errores[CampoSalida.FECHA] = "Ingresá una fecha válida."
        }
        if (s.lugar.isBlank()) errores[CampoSalida.LUGAR] = "Ingresá un lugar."
        if (s.capturas.isEmpty()) errores[CampoSalida.CAPTURAS] = "Agregá al menos una especie pescada."
        return errores
    }

    companion object {
        fun Factory(repository: SalidaPescaRepository, appContext: Context) = viewModelFactory {
            initializer { NuevaSalidaPescaViewModel(repository, appContext) }
        }
    }
}
