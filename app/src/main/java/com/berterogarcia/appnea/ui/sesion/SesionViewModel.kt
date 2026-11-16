package com.berterogarcia.appnea.ui.sesion

import com.berterogarcia.appnea.util.intentar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.AppDestinations
import com.berterogarcia.appnea.data.repository.SesionRepository
import com.berterogarcia.appnea.navigation.RUTA_LOGIN
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado del arranque de la app. */
sealed interface EstadoArranque {
    /** Todavía se está verificando la sesión guardada (se muestra un indicador). */
    data object Verificando : EstadoArranque

    /** Ya se sabe a dónde ir: INICIO si hay sesión válida, LOGIN si no. */
    data class Listo(val destinoInicial: String) : EstadoArranque
}

/**
 * Se crea UNA vez en MainActivity (by viewModels), así sobrevive a la rotación y no
 * vuelve a consultar la base ni a "parpadear" el login.
 *
 * Por qué hace falta: el NavHost tenía `startDestination = RUTA_LOGIN` fijo, entonces
 * aunque el usuario quedaba guardado en SharedPreferences, al abrir la app siempre
 * arrancaba en el login. Ahora el destino inicial se decide ANTES de crear el NavHost.
 */
class SesionViewModel(private val sesionRepository: SesionRepository) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoArranque>(EstadoArranque.Verificando)
    val estado: StateFlow<EstadoArranque> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val usuario = intentar("SesionViewModel") { sesionRepository.usuarioConSesionValida() }.getOrNull()
            _estado.value = EstadoArranque.Listo(
                destinoInicial = if (usuario != null) AppDestinations.INICIO.name else RUTA_LOGIN
            )
        }
    }

    companion object {
        fun Factory(sesionRepository: SesionRepository) = viewModelFactory {
            initializer { SesionViewModel(sesionRepository) }
        }
    }
}
