package com.berterogarcia.appnea.ui.detalleinmersion

import com.berterogarcia.appnea.util.intentar
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berterogarcia.appnea.data.export.UddfExporter
import com.berterogarcia.appnea.data.local.InmersionEntity
import com.berterogarcia.appnea.data.perfil.PerfilSimulado
import com.berterogarcia.appnea.data.perfil.PuntoPerfil
import com.berterogarcia.appnea.data.repository.InmersionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
}