package com.berterogarcia.appnea.ui.clima

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.condiciones.Condiciones
import com.berterogarcia.appnea.data.condiciones.CondicionesRepository
import com.berterogarcia.appnea.data.local.UBICACION_COMODORO_RIVADAVIA
import com.berterogarcia.appnea.data.local.Ubicacion
import com.berterogarcia.appnea.data.local.UbicacionHelper
import com.berterogarcia.appnea.ui.components.TextoUi
import com.berterogarcia.appnea.ui.components.textoUi
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
    val error: TextoUi? = null
)

/**
 * Carga todas las condiciones (clima, mar, mareas, sol, luna).
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

            val motivo: TextoUi?
            val ubicacion: Ubicacion?
            if (!UbicacionHelper.ubicacionActivada(appContext)) {
                motivo = textoUi(R.string.clima_error_ubicacion_desactivada)
                ubicacion = null
            } else {
                ubicacion = UbicacionHelper.obtenerUbicacionActual(appContext)
                motivo = if (ubicacion == null)
                    textoUi(R.string.clima_error_ubicacion)
                else null
            }
            _uiState.update { it.copy(buscandoUbicacion = false) }

            if (ubicacion == null) {
                // Falló: vuelve a Comodoro y avisa por qué
                _uiState.update { it.copy(ubicacion = UBICACION_COMODORO_RIVADAVIA) }
                cargar(UBICACION_COMODORO_RIVADAVIA, aviso = motivo?.let { textoUi(R.string.clima_vuelve_comodoro, it) })
            } else {
                // Sin nombre: la pantalla muestra "Mi ubicación" (strings.xml)
                val mia = ubicacion.copy(nombre = null)
                _uiState.update { it.copy(ubicacion = mia) }
                cargar(mia)
            }
        }
    }

    fun permisoDenegado() {
        _uiState.update {
            it.copy(
                error = textoUi(R.string.clima_error_permiso)
            )
        }
    }

    private fun lanzar(bloque: suspend () -> Unit) {
        trabajo?.cancel()
        trabajo = viewModelScope.launch { bloque() }
    }

    /** [aviso]: mensaje que queda visible al terminar (ej.: por qué se volvió a Comodoro). */
    private suspend fun cargar(ubicacion: Ubicacion, aviso: TextoUi? = null) {
        _uiState.update { it.copy(cargando = true, error = null) }
        val resultado = CondicionesRepository.obtener(ubicacion)
        currentCoroutineContext().ensureActive()   // si se canceló mientras cargaba, no toca el estado
        resultado.fold(
            onSuccess = { condiciones ->
                _uiState.update { it.copy(cargando = false, condiciones = condiciones, error = aviso) }
            },
            onFailure = { error ->
                // Sin esto el motivo real (red, JSON, cálculo de mareas…) se pierde: la UI solo dice "revisá tu conexión"
                Log.e(TAG, "No se pudo cargar el clima de $ubicacion", error)
                _uiState.update {
                    it.copy(
                        cargando = false,
                        // Si había un aviso (ej. se volvió a Comodoro), va arriba del error de red
                        error = textoUi(R.string.clima_error_red).let { red ->
                            if (aviso != null) textoUi(R.string.clima_dos_mensajes, aviso, red) else red
                        }
                    )
                }
            }
        )
    }

    companion object {
        private const val TAG = "Clima"

        fun Factory(appContext: Context) = viewModelFactory {
            initializer { ClimaViewModel(appContext) }
        }
    }
}