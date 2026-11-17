package com.berterogarcia.appnea.ui.sesion

import com.berterogarcia.appnea.util.intentar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berterogarcia.appnea.dominio.repositorios.RepositorioSesion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado del arranque de la app. */
sealed interface EstadoArranque {
    /** Todavía se está verificando la sesión guardada (se muestra un indicador). */
    data object Verificando : EstadoArranque

    /** Ya se sabe a dónde ir: Inicio si hay sesión válida, Login si no. */
    data class Listo(val haySesion: Boolean) : EstadoArranque
}

/**
 * Se crea UNA vez en MainActivity (by viewModels), así sobrevive a la rotación y no
 * vuelve a consultar la base ni a "parpadear" el login.
 *
 * Por qué hace falta: el NavHost tenía el login como destino inicial fijo, entonces
 * aunque el usuario quedaba guardado en SharedPreferences, al abrir la app siempre
 * arrancaba en el login. Ahora el destino inicial se decide ANTES de crear el NavHost.
 */
class SesionViewModel(private val sesionRepository: RepositorioSesion) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoArranque>(EstadoArranque.Verificando)
    val estado: StateFlow<EstadoArranque> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val usuario = intentar("SesionViewModel") { sesionRepository.usuarioConSesionValida() }.getOrNull()
            _estado.value = EstadoArranque.Listo(haySesion = usuario != null)
        }
    }
}
