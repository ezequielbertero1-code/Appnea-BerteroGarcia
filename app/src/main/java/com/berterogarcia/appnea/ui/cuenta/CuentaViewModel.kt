package com.berterogarcia.appnea.ui.cuenta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.data.local.SessionManager
import com.berterogarcia.appnea.data.local.formatearDuracion
import com.berterogarcia.appnea.data.repository.InmersionRepository
import com.berterogarcia.appnea.data.repository.SalidaPescaRepository
import com.berterogarcia.appnea.data.repository.SesionApneaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration

/**
 * Perfil SIMULADO: todavía no guardamos estos datos en la base, así que se muestran
 * valores de ejemplo para ver cómo quedaría la pantalla con todos los campos.
 */
data class PerfilSimulado(
    val nombreCompleto: String,
    val email: String,
    val fechaNacimiento: String,
    val ciudad: String,
    val telefono: String,
    val certificacionBuceo: String,
    val certificacionApnea: String,
    val escuela: String,
    val numeroCertificado: String,
    val aptoMedicoVence: String,
    val contactoEmergenciaNombre: String,
    val contactoEmergenciaRelacion: String,
    val contactoEmergenciaTelefono: String,
    val companeroHabitual: String
)

private fun perfilDeEjemplo(usuario: String?): PerfilSimulado {
    val nombre = usuario?.takeIf { it.isNotBlank() } ?: "Usuario"
    return if (nombre.equals("Franco", ignoreCase = true)) {
        PerfilSimulado(
            nombreCompleto = "Franco García",
            email = "franco.garcia@appnea.com.ar",
            fechaNacimiento = "14/03/1995",
            ciudad = "Comodoro Rivadavia, Chubut",
            telefono = "+54 297 415-2231",
            certificacionBuceo = "Buceador 2 Estrellas (CMAS)",
            certificacionApnea = "Apnea Nivel 1",
            escuela = "Escuela de Buceo Zeus",
            numeroCertificado = "ARG-CMAS-2023-04871",
            aptoMedicoVence = "30/06/2027",
            contactoEmergenciaNombre = "Laura González",
            contactoEmergenciaRelacion = "Hermana",
            contactoEmergenciaTelefono = "+54 297 430-8812",
            companeroHabitual = "Eze Bertero"
        )
    } else {
        PerfilSimulado(
            nombreCompleto = nombre,
            email = "${nombre.lowercase()}@appnea.com.ar",
            fechaNacimiento = "01/01/1998",
            ciudad = "Comodoro Rivadavia, Chubut",
            telefono = "+54 297 400-0000",
            certificacionBuceo = "Buceador 1 Estrella (CMAS)",
            certificacionApnea = "Apnea Nivel 1",
            escuela = "Escuela de Buceo Zeus",
            numeroCertificado = "ARG-CMAS-2024-00000",
            aptoMedicoVence = "31/12/2026",
            contactoEmergenciaNombre = "Contacto de ejemplo",
            contactoEmergenciaRelacion = "Familiar",
            contactoEmergenciaTelefono = "+54 297 400-0001",
            companeroHabitual = "Sin cargar"
        )
    }
}

/**
 * Estadísticas REALES calculadas a partir de lo que hay cargado en la base
 * (inmersiones, sesiones de entrenamiento y salidas de pesca).
 */
data class EstadisticasCuenta(
    val inmersiones: Int = 0,
    val profundidadMaxima: String = "-",
    val tiempoTotalFondo: String = "-",
    val sesionesEntrenamiento: Int = 0,
    val sesionesCompletas: Int = 0,
    val mejorApnea: String = "-",
    val salidasPesca: Int = 0,
    val capturasTotales: Int = 0,
    val mayorPieza: String = "-"
)

class CuentaViewModel(
    private val sessionManager: SessionManager,
    inmersionRepository: InmersionRepository,
    sesionApneaRepository: SesionApneaRepository,
    salidaPescaRepository: SalidaPescaRepository
) : ViewModel() {
    val usuario: String? = sessionManager.obtenerUsuario()

    val perfil: PerfilSimulado = perfilDeEjemplo(usuario)

    val estadisticas: StateFlow<EstadisticasCuenta> = combine(
        inmersionRepository.todasLasInmersiones,
        sesionApneaRepository.todasLasSesiones,
        salidaPescaRepository.todasLasSalidas
    ) { inmersiones, sesiones, salidas ->
        val capturas = salidas.flatMap { it.capturas }
        val mayor = capturas.filter { it.pesoKg != null }.maxByOrNull { it.pesoKg!! }
        val fondoTotal = inmersiones.fold(Duration.ZERO) { acc, i -> acc + i.tiempoFondo }
        EstadisticasCuenta(
            inmersiones = inmersiones.size,
            profundidadMaxima = inmersiones.maxOfOrNull { it.profundidadMetros }
                ?.let { "${"%.1f".format(it)} m" } ?: "-",
            tiempoTotalFondo = if (inmersiones.isEmpty()) "-"
                else "${fondoTotal.inWholeMinutes} min",
            sesionesEntrenamiento = sesiones.size,
            sesionesCompletas = sesiones.count { it.completada },
            mejorApnea = sesiones.maxOfOrNull { it.marcaSegundos }
                ?.let { formatearDuracion(it) } ?: "-",
            salidasPesca = salidas.size,
            capturasTotales = capturas.sumOf { it.cantidad },
            mayorPieza = mayor?.let { "${it.especie} (${"%.1f".format(it.pesoKg)} kg)" } ?: "-"
        )
    }.stateIn(
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
            inmersionRepository: InmersionRepository,
            sesionApneaRepository: SesionApneaRepository,
            salidaPescaRepository: SalidaPescaRepository
        ) = viewModelFactory {
            initializer {
                CuentaViewModel(sessionManager, inmersionRepository, sesionApneaRepository, salidaPescaRepository)
            }
        }
    }
}
