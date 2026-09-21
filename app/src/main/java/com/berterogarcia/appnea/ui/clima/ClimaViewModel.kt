package com.berterogarcia.appnea.ui.clima

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.data.local.UBICACION_COMODORO_RIVADAVIA
import com.berterogarcia.appnea.data.local.Ubicacion
import com.berterogarcia.appnea.data.local.UbicacionHelper
import com.berterogarcia.appnea.data.remote.ClimaActual
import com.berterogarcia.appnea.data.remote.MarActual
import com.berterogarcia.appnea.data.remote.OpenMeteoService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClimaUiState(
    val ubicacion: Ubicacion = UBICACION_COMODORO_RIVADAVIA,
    val cargando: Boolean = false,
    val clima: ClimaActual? = null,
    val mar: MarActual? = null,
    val marDisponible: Boolean = true,
    val error: String? = null
)

class ClimaViewModel(private val appContext: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(ClimaUiState())
    val uiState: StateFlow<ClimaUiState> = _uiState

    init {
        cargarClima()
    }

    fun cargarClima() {
        val ubicacion = _uiState.value.ubicacion
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }

            val climaResultado = OpenMeteoService.obtenerClimaActual(ubicacion.latitud, ubicacion.longitud)
            val marResultado = OpenMeteoService.obtenerMarActual(ubicacion.latitud, ubicacion.longitud)

            climaResultado.fold(
                onSuccess = { clima ->
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            clima = clima,
                            mar = marResultado.getOrNull(),
                            marDisponible = marResultado.isSuccess,
                            error = null
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            error = "No se pudo obtener el clima. Revisá tu conexión a internet e intentá de nuevo."
                        )
                    }
                }
            )
        }
    }

    fun usarMiUbicacion() {
        if (!UbicacionHelper.tienePermiso(appContext)) {
            _uiState.update { it.copy(error = "Necesitás dar permiso de ubicación para usar esta opción.") }
            return
        }
        val ubicacion = UbicacionHelper.obtenerUltimaUbicacion(appContext)
        if (ubicacion == null) {
            _uiState.update {
                it.copy(error = "No se pudo obtener tu ubicación todavía. Probá de nuevo en unos segundos.")
            }
            return
        }
        _uiState.update { it.copy(ubicacion = ubicacion.copy(nombre = "Mi ubicación")) }
        cargarClima()
    }

    fun usarComodoroRivadavia() {
        _uiState.update { it.copy(ubicacion = UBICACION_COMODORO_RIVADAVIA) }
        cargarClima()
    }

    companion object {
        fun Factory(appContext: Context) = viewModelFactory {
            initializer { ClimaViewModel(appContext) }
        }
    }
}
