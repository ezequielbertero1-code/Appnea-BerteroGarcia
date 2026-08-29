package com.berterogarcia.appnea.ui.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.berterogarcia.appnea.data.local.InmersionEntity
import com.berterogarcia.appnea.data.repository.InmersionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistorialViewModel(repository: InmersionRepository) : ViewModel() {

    val inmersiones: StateFlow<List<InmersionEntity>> =
        repository.todasLasInmersiones.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    companion object {
        fun Factory(repository: InmersionRepository) = viewModelFactory {
            initializer { HistorialViewModel(repository) }
        }
    }
}