package com.berterogarcia.appnea.ui.clima

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.data.condiciones.Condiciones
import com.berterogarcia.appnea.data.condiciones.CondicionesRepository
import com.berterogarcia.appnea.data.local.UBICACION_COMODORO_RIVADAVIA
import com.berterogarcia.appnea.data.local.Ubicacion
import com.berterogarcia.appnea.data.local.UbicacionHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClimaUiState(
    val ubicacion: Ubicacion = UBICACION_COMODORO_RIVADAVIA,
    val cargando: Boolean = false,
    val buscandoUbicacion: Boolean = false,   // true mientras se espera el GPS
    val condiciones: Condiciones? = null,
    val error: String? = null
)

/**
 * Carga todas las condiciones (clima, mar, mareas, sol, luna, actividad de pesca).
 * Lo usan la sección de clima de Inicio y la pantalla completa de Condiciones.
 */
class ClimaViewModel(private val appContext: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(ClimaUiState())
    val uiState: StateFlow<ClimaUiState> = _uiState

    // Pedido en curso: si llega uno nuevo, se cancela el anterior para que no pise el resultado
    private var trabajo: Job? = null

    init {
        cargarClima()
    }

    /** Recarga la ubicación actual (botón "Actualizar"): mantiene los datos viejos en pantalla. */
    fun cargarClima() = lanzar { cargar(_uiState.value.ubicacion) }

    fun usarComodoroRivadavia() = lanzar {
        _uiState.update { it.copy(ubicacion = UBICACION_COMODORO_RIVADAVIA, condiciones = null) }
        cargar(UBICACION_COMODORO_RIVADAVIA)
    }

    fun usarMiUbicacion() {
        if (!UbicacionHelper.tienePermiso(appContext)) {
            permisoDenegado()
            return
        }
        lanzar {
            // Limpia la pantalla: se ve la ruedita y "Buscando tu ubicación…"
            _uiState.update {
                it.copy(cargando = true, buscandoUbicacion = true, condiciones = null, error = null)
            }

            val motivo: String?
            val ubicacion: Ubicacion?
            if (!UbicacionHelper.ubicacionActivada(appContext)) {
                motivo = "La ubicación del teléfono está desactivada."
                ubicacion = null
            } else {
                ubicacion = UbicacionHelper.obtenerUbicacionActual(appContext)
                motivo = if (ubicacion == null)
                    "No se pudo obtener tu ubicación (probá cerca de una ventana o activá " +
                            "\"Precisión de la ubicación de Google\" en Ajustes)."
                else null
            }
            _uiState.update { it.copy(buscandoUbicacion = false) }

            if (ubicacion == null) {
                // Falló: vuelve a Comodoro y avisa por qué
                _uiState.update { it.copy(ubicacion = UBICACION_COMODORO_RIVADAVIA) }
                cargar(UBICACION_COMODORO_RIVADAVIA, aviso = "$motivo Se muestra Comodoro Rivadavia.")
            } else {
                val mia = ubicacion.copy(nombre = "Mi ubicación")
                _uiState.update { it.copy(ubicacion = mia) }
                cargar(mia)
            }
        }
    }

    fun permisoDenegado() {
        _uiState.update {
            it.copy(
                error = "Sin permiso de ubicación. Si lo rechazaste antes, habilitalo en " +
                        "Ajustes > Apps > Appnea > Permisos."
            )
        }
    }

    private fun lanzar(bloque: suspend () -> Unit) {
        trabajo?.cancel()
        trabajo = viewModelScope.launch { bloque() }
    }

    /** [aviso]: mensaje que queda visible al terminar (ej.: por qué se volvió a Comodoro). */
    private suspend fun cargar(ubicacion: Ubicacion, aviso: String? = null) {
        _uiState.update { it.copy(cargando = true, error = null) }
        val resultado = CondicionesRepository.obtener(ubicacion)
        currentCoroutineContext().ensureActive()   // si se canceló mientras cargaba, no toca el estado
        resultado.fold(
            onSuccess = { condiciones ->
                _uiState.update { it.copy(cargando = false, condiciones = condiciones, error = aviso) }
            },
            onFailure = {
                _uiState.update {
                    it.copy(
                        cargando = false,
                        error = listOfNotNull(
                            aviso,
                            "No se pudo obtener el clima. Revisá tu conexión a internet e intentá de nuevo."
                        ).joinToString("\n")
                    )
                }
            }
        )
    }

    companion object {
        fun Factory(appContext: Context) = viewModelFactory {
            initializer { ClimaViewModel(appContext) }
        }
    }
}