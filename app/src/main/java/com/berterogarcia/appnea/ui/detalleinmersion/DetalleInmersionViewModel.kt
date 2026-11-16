package com.berterogarcia.appnea.ui.detalleinmersion

import com.berterogarcia.appnea.util.intentar
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.data.export.UddfExporter
import com.berterogarcia.appnea.data.local.InmersionEntity
import com.berterogarcia.appnea.data.perfil.PerfilSimulado
import com.berterogarcia.appnea.data.perfil.PuntoPerfil
import com.berterogarcia.appnea.data.repository.InmersionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetalleInmersionViewModel(
    private val repository: InmersionRepository,
    id: Long,
    private val appContext: Context
) : ViewModel() {

    val inmersion: StateFlow<InmersionEntity?> = repository.obtenerPorId(id).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    /** Perfil simulado para la pestaña Gráfico (se recalcula solo si cambia la inmersión). */
    val perfil: StateFlow<List<PuntoPerfil>> = inmersion
        .map { it?.let(PerfilSimulado::generar).orEmpty() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun eliminar(onEliminada: () -> Unit) {
        val i = inmersion.value ?: return
        viewModelScope.launch {
            repository.eliminar(i)
            onEliminada()
        }
    }

    /** Escribe el UDDF en el archivo que eligió el usuario. Devuelve true si salió bien. */
    suspend fun exportarUddf(uri: Uri): Boolean {
        val i = inmersion.value ?: return false
        val contenido = UddfExporter.generar(i)
        return withContext(Dispatchers.IO) {
            intentar("DetalleInmersionViewModel") {
                appContext.contentResolver.openOutputStream(uri)
                    ?.use { it.write(contenido.toByteArray(Charsets.UTF_8)) }
                    ?: error("No se pudo abrir el archivo")
            }.isSuccess
        }
    }

    companion object {
        fun Factory(repository: InmersionRepository, id: Long, context: Context) = viewModelFactory {
            initializer { DetalleInmersionViewModel(repository, id, context.applicationContext) }
        }
    }
}