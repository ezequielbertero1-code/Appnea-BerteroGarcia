package com.berterogarcia.appnea.ui.cuenta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berterogarcia.appnea.dominio.modelo.Certificado
import com.berterogarcia.appnea.dominio.modelo.ContactoEmergencia
import com.berterogarcia.appnea.dominio.modelo.Equipo
import com.berterogarcia.appnea.dominio.modelo.PerfilUsuario
import com.berterogarcia.appnea.dominio.GestorSesion
import com.berterogarcia.appnea.dominio.modelo.formatearDuracion
import com.berterogarcia.appnea.dominio.repositorios.RepositorioInmersiones
import com.berterogarcia.appnea.dominio.repositorios.RepositorioPerfil
import com.berterogarcia.appnea.dominio.repositorios.RepositorioSesionesApnea
import com.berterogarcia.appnea.ui.perfil.certificados.ordenarCertificados
import com.berterogarcia.appnea.ui.perfil.comun.EstadoVencimiento
import com.berterogarcia.appnea.ui.perfil.comun.estadoVencimiento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Locale
import kotlin.time.Duration


/**
 * Resumen del perfil para la pantalla Cuenta (datos reales de la base).
 * `cargando` evita mostrar "no cargaste nada" durante el primer instante.
 */
data class ResumenPerfil(
    val cargando: Boolean = true,
    val perfil: PerfilUsuario? = null,
    val equipos: List<Equipo> = emptyList(),
    val certificados: List<Certificado> = emptyList(),
    val contactos: List<ContactoEmergencia> = emptyList(),
    val error: Boolean = false
) {
    val equiposEnUso: List<Equipo> get() = equipos.filter { it.enUso }

    val equiposConServicePendiente: Int
        get() = equiposEnUso.count {
            estadoVencimiento(it.proximoMantenimiento) in setOf(EstadoVencimiento.POR_VENCER, EstadoVencimiento.VENCIDO)
        }

    val contactoPrincipal: ContactoEmergencia?
        get() = contactos.firstOrNull { it.esPrincipal } ?: contactos.firstOrNull()
}

/**
 * Estadísticas REALES calculadas a partir de lo que hay cargado en la base
 * (inmersiones y sesiones de entrenamiento).
 */
data class EstadisticasCuenta(
    val inmersiones: Int = 0,
    val profundidadMaxima: String = "-",   // solo el número ("22,3"): la unidad la pone la pantalla
    val tiempoTotalFondo: String = "-",
    val sesionesEntrenamiento: Int = 0,
    val sesionesCompletas: Int = 0,
    val mejorApnea: String = "-"
)

class CuentaViewModel(
    private val sessionManager: GestorSesion,
    perfilRepository: RepositorioPerfil,
    inmersionRepository: RepositorioInmersiones,
    sesionApneaRepository: RepositorioSesionesApnea,
    entrenando: Flow<Boolean> = flowOf(false)
) : ViewModel() {
    /** true mientras corre una tabla: al cerrar sesión se corta, así que se pide confirmación. */
    val entrenamientoEnCurso: StateFlow<Boolean> = entrenando.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Se actualiza solo si cambia la sesión (no es una foto del momento en que se abrió). */
    val usuario: StateFlow<String?> = sessionManager.usuarioActual

    // Perfil real, se actualiza solo cuando se edita en las otras pantallas (Flow de Room).
    private val resumenFlow: Flow<ResumenPerfil> =
        combine(
            perfilRepository.perfilActual,
            perfilRepository.equipamientoActual,
            perfilRepository.certificadosActuales,
            perfilRepository.contactosActuales
        ) { perfil, equipos, certificados, contactos ->
            ResumenPerfil(
                cargando = false,
                perfil = perfil,
                equipos = equipos,
                certificados = ordenarCertificados(certificados),
                contactos = contactos
            )
        }.catch { emit(ResumenPerfil(cargando = false, error = true)) }

    val resumen: StateFlow<ResumenPerfil> = resumenFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ResumenPerfil()
    )

    val estadisticas: StateFlow<EstadisticasCuenta> = combine(
        inmersionRepository.todasLasInmersiones,
        sesionApneaRepository.todasLasSesiones
    ) { inmersiones, sesiones ->
        val fondoTotal = inmersiones.fold(Duration.ZERO) { acc, i -> acc + i.tiempoFondo }
        EstadisticasCuenta(
            inmersiones = inmersiones.size,
            profundidadMaxima = inmersiones.maxOfOrNull { it.profundidadMetros }
                ?.let { "%.1f".format(Locale.forLanguageTag("es-AR"), it) } ?: "-",
            tiempoTotalFondo = if (inmersiones.isEmpty()) "-"
            else "${fondoTotal.inWholeMinutes} min",
            sesionesEntrenamiento = sesiones.size,
            sesionesCompletas = sesiones.count { it.completada },
            mejorApnea = sesiones.maxOfOrNull { it.marcaSegundos }
                ?.let { formatearDuracion(it) } ?: "-"
        )
    }
        .catch { emit(EstadisticasCuenta()) } // si falla la base, se muestran guiones en vez de crashear
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EstadisticasCuenta()
        )

    fun cerrarSesion() {
        sessionManager.cerrarSesion()
    }
}
