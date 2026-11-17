package com.berterogarcia.appnea.ui.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berterogarcia.appnea.dominio.modelo.Inmersion
import com.berterogarcia.appnea.dominio.repositorios.RepositorioInmersiones
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class InicioUiState(
    val ultimas: List<Inmersion> = emptyList(),
    val total: Int = 0,
    val error: Boolean = false
)

class InicioViewModel(repository: RepositorioInmersiones) : ViewModel() {

    /**
     * null = todavía cargando. Así al abrir la app no aparece un instante el
     * "todavía no registraste inmersiones" antes de que lleguen los datos de Room.
     */
    val uiState: StateFlow<InicioUiState?> = repository.todasLasInmersiones
        .map<List<Inmersion>, InicioUiState?> { lista ->
            InicioUiState(
                // El DAO ordena por id (orden de carga). Acá se ordena por fecha real de la
                // inmersión, así una inmersión vieja importada no aparece como "la última".
                ultimas = lista
                    .sortedWith(
                        compareByDescending<Inmersion> { it.fecha }
                            .thenByDescending { it.horaEntrada }
                    )
                    .take(CANTIDAD_RECIENTES),
                total = lista.size
            )
        }
        .catch { emit(InicioUiState(error = true)) } // si falla Room, se avisa en vez de crashear
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {

        private const val CANTIDAD_RECIENTES = 3
    }
}