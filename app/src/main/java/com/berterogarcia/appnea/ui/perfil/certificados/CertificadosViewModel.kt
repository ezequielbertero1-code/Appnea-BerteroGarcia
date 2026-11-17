package com.berterogarcia.appnea.ui.perfil.certificados

import com.berterogarcia.appnea.util.intentar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berterogarcia.appnea.dominio.modelo.Certificado
import com.berterogarcia.appnea.dominio.repositorios.RepositorioPerfil
import com.berterogarcia.appnea.ui.components.TextoUi
import com.berterogarcia.appnea.ui.perfil.comun.EstadoLista
import com.berterogarcia.appnea.ui.perfil.comun.EstadoVencimiento
import com.berterogarcia.appnea.ui.perfil.comun.MENSAJE_ERROR_ELIMINAR
import com.berterogarcia.appnea.ui.perfil.comun.MENSAJE_SIN_SESION
import com.berterogarcia.appnea.ui.perfil.comun.comoEstadoLista
import com.berterogarcia.appnea.ui.perfil.comun.estadoVencimiento
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Orden pensado para que lo urgente quede arriba:
 * vencidos → por vencer → vigentes con vencimiento → sin vencimiento; dentro de cada grupo
 * por tipo (orden del enum) y fecha de emisión más reciente primero.
 */
internal fun ordenarCertificados(lista: List<Certificado>): List<Certificado> {
    fun prioridad(c: Certificado) = when (estadoVencimiento(c.fechaVencimiento)) {
        EstadoVencimiento.VENCIDO -> 0
        EstadoVencimiento.POR_VENCER -> 1
        EstadoVencimiento.VIGENTE -> 2
        EstadoVencimiento.SIN_FECHA -> 3
    }
    return lista.sortedWith(
        compareBy<Certificado> { prioridad(it) }
            .thenBy { it.tipo.ordinal }
            .thenByDescending { it.fechaEmision }
    )
}

class CertificadosViewModel(
    private val repository: RepositorioPerfil
) : ViewModel() {
    /** Si había sesión al abrir la pantalla (el repositorio trabaja siempre sobre el usuario logueado). */
    private val haySesion: Boolean = repository.haySesion

    val estado: StateFlow<EstadoLista<Certificado>> =
        if (!haySesion) {
            MutableStateFlow(EstadoLista(cargando = false, error = MENSAJE_SIN_SESION))
        } else {
            repository.certificadosActuales.comoEstadoLista(viewModelScope, ::ordenarCertificados)
        }

    private val _mensaje = MutableStateFlow<TextoUi?>(null)
    val mensaje: StateFlow<TextoUi?> = _mensaje.asStateFlow()

    fun eliminar(id: Long) {
        if (!haySesion) return
        viewModelScope.launch {
            intentar("CertificadosViewModel") { repository.eliminarCertificado(id) }
                .onFailure { _mensaje.value = MENSAJE_ERROR_ELIMINAR }
        }
    }

    fun mensajeMostrado() {
        _mensaje.value = null
    }
}
