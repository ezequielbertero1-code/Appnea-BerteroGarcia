package com.berterogarcia.appnea.ui.pesca

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.data.local.SalidaPescaEntity
import com.berterogarcia.appnea.data.repository.SalidaPescaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PescaSubmarinaViewModel(private val repository: SalidaPescaRepository) : ViewModel() {

    val salidas: StateFlow<List<SalidaPescaEntity>> =
        repository.todasLasSalidas.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun eliminar(salida: SalidaPescaEntity) {
        viewModelScope.launch { repository.eliminar(salida) }
    }

    companion object {
        fun Factory(repository: SalidaPescaRepository) = viewModelFactory {
            initializer { PescaSubmarinaViewModel(repository) }
        }
    }
}
