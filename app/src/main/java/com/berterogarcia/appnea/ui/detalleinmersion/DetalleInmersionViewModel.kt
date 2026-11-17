package com.berterogarcia.appnea.ui.detalleinmersion

import com.berterogarcia.appnea.dominio.ExportadorInmersiones
import com.berterogarcia.appnea.util.intentar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berterogarcia.appnea.dominio.modelo.Inmersion
import com.berterogarcia.appnea.dominio.PerfilSimulado
import com.berterogarcia.appnea.dominio.PuntoPerfil
import com.berterogarcia.appnea.dominio.repositorios.RepositorioInmersiones
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetalleInmersionViewModel(
    private val repository: RepositorioInmersiones,
    id: Long,
    private val exportador: ExportadorInmersiones
) : ViewModel() {

    val inmersion: StateFlow<Inmersion?> = repository.obtenerPorId(id).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    /** Perfil simulado para la pestaña Gráfico (se recalcula solo si cambia la inmersión). */
    val perfil: StateFlow<List<PuntoPerfil>> = inmersion
        .map { it?.let(PerfilSimulado::generar).orEmpty() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Resultado de eliminar: null = nada pendiente, true = se borró, false = falló.
     * Es estado (no un callback guardado en el ViewModel): la pantalla reacciona aunque se haya
     * recreado (rotación) y un error de la base ya no cierra la app.
     */
    private val _eliminada = MutableStateFlow<Boolean?>(null)
    val eliminada: StateFlow<Boolean?> = _eliminada.asStateFlow()

    fun eliminar() {
        val i = inmersion.value ?: return
        viewModelScope.launch {
            _eliminada.value = intentar("DetalleInmersionViewModel") { repository.eliminar(i) }.getOrDefault(false)
        }
    }

    /** La pantalla ya mostró el error: se limpia para poder reintentar. */
    fun errorEliminarMostrado() {
        _eliminada.value = null
    }

    /** Escribe el UDDF en el archivo que eligió el usuario ([destino]: la Uri como texto). */
    suspend fun exportarUddf(destino: String): Boolean {
        val i = inmersion.value ?: return false
        return exportador.exportar(i, destino)
    }
}
