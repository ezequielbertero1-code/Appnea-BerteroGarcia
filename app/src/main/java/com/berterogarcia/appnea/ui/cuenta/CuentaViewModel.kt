package com.berterogarcia.appnea.ui.cuenta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.data.local.CertificadoEntity
import com.berterogarcia.appnea.data.local.ContactoEmergenciaEntity
import com.berterogarcia.appnea.data.local.EquipoEntity
import com.berterogarcia.appnea.data.local.PerfilUsuarioEntity
import com.berterogarcia.appnea.data.local.SessionManager
import com.berterogarcia.appnea.data.local.formatearDuracion
import com.berterogarcia.appnea.data.repository.InmersionRepository
import com.berterogarcia.appnea.data.repository.PerfilRepository
import com.berterogarcia.appnea.data.repository.SesionApneaRepository
import com.berterogarcia.appnea.ui.perfil.certificados.ordenarCertificados
import com.berterogarcia.appnea.ui.perfil.comun.EstadoVencimiento
import com.berterogarcia.appnea.ui.perfil.comun.estadoVencimiento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.util.Locale
import kotlin.time.Duration

// [CAMBIO] Se eliminó PerfilSimulado / perfilDeEjemplo: ahora los datos salen de la base.

/**
 * Resumen del perfil para la pantalla Cuenta (datos reales de la base).
 * `cargando` evita mostrar "no cargaste nada" durante el primer instante.
 */
data class ResumenPerfil(
    val cargando: Boolean = true,
    val perfil: PerfilUsuarioEntity? = null,
    val equipos: List<EquipoEntity> = emptyList(),
    val certificados: List<CertificadoEntity> = emptyList(),
    val contactos: List<ContactoEmergenciaEntity> = emptyList(),
    val error: Boolean = false
) {
    val equiposEnUso: List<EquipoEntity> get() = equipos.filter { it.enUso }

    val equiposConServicePendiente: Int
        get() = equiposEnUso.count {
            estadoVencimiento(it.proximoMantenimiento) in setOf(EstadoVencimiento.POR_VENCER, EstadoVencimiento.VENCIDO)
        }

    val contactoPrincipal: ContactoEmergenciaEntity?
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
    private val sessionManager: SessionManager,
    perfilRepository: PerfilRepository,
    inmersionRepository: InmersionRepository,
    sesionApneaRepository: SesionApneaRepository
) : ViewModel() {
    val usuario: String? = sessionManager.obtenerUsuario()

    // Perfil real, se actualiza solo cuando se edita en las otras pantallas (Flow de Room).
    private val resumenFlow: Flow<ResumenPerfil> =
        if (usuario == null) {
            flowOf(ResumenPerfil(cargando = false))
        } else {
            combine(
                perfilRepository.observarPerfil(usuario),
                perfilRepository.observarEquipamiento(usuario),
                perfilRepository.observarCertificados(usuario),
                perfilRepository.observarContactos(usuario)
            ) { perfil, equipos, certificados, contactos ->
                ResumenPerfil(
                    cargando = false,
                    perfil = perfil,
                    equipos = equipos,
                    certificados = ordenarCertificados(certificados),
                    contactos = contactos
                )
            }.catch { emit(ResumenPerfil(cargando = false, error = true)) }
        }

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

    companion object {
        fun Factory(
            sessionManager: SessionManager,
            perfilRepository: PerfilRepository,
            inmersionRepository: InmersionRepository,
            sesionApneaRepository: SesionApneaRepository
        ) = viewModelFactory {
            initializer {
                CuentaViewModel(sessionManager, perfilRepository, inmersionRepository, sesionApneaRepository)
            }
        }
    }
}