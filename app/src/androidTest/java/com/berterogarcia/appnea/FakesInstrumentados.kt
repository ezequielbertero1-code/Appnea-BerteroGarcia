package com.berterogarcia.appnea

import com.berterogarcia.appnea.dominio.FuenteCondiciones
import com.berterogarcia.appnea.dominio.ServicioUbicacion
import com.berterogarcia.appnea.dominio.modelo.Certificado
import com.berterogarcia.appnea.dominio.modelo.Condiciones
import com.berterogarcia.appnea.dominio.modelo.CondicionesRegistro
import com.berterogarcia.appnea.dominio.modelo.ContactoEmergencia
import com.berterogarcia.appnea.dominio.modelo.Equipo
import com.berterogarcia.appnea.dominio.modelo.Inmersion
import com.berterogarcia.appnea.dominio.modelo.IntentoLibre
import com.berterogarcia.appnea.dominio.modelo.PerfilUsuario
import com.berterogarcia.appnea.dominio.modelo.SesionApnea
import com.berterogarcia.appnea.dominio.modelo.TablaPersonalizada
import com.berterogarcia.appnea.dominio.modelo.TipoInmersion
import com.berterogarcia.appnea.dominio.modelo.Ubicacion
import com.berterogarcia.appnea.dominio.repositorios.RepositorioInmersiones
import com.berterogarcia.appnea.dominio.repositorios.RepositorioIntentos
import com.berterogarcia.appnea.dominio.repositorios.RepositorioPerfil
import com.berterogarcia.appnea.dominio.repositorios.RepositorioSesionesApnea
import com.berterogarcia.appnea.dominio.repositorios.RepositorioTablas
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/*
 * Versiones mínimas en memoria para los tests de UI (los de JVM tienen las suyas en test/fakes).
 * Cuando exista un módulo :core:testing (Fase 9) se unifican.
 */

fun inmersionDePrueba(id: Long = 0, lugar: String = "Punta Marqués", usuario: String = "ana") = Inmersion(
    id = id, fecha = LocalDate.of(2026, 11, 10), lugar = lugar, profundidadMetros = 18f, paradaSeguridadMin = 3f,
    tiempoFondo = 30.minutes, horaEntrada = LocalTime.of(10, 0), horaSalida = LocalTime.of(10, 45),
    caracteristicasAgua = emptySet(), lastreKg = null, proteccionMm = null, equipamiento = emptySet(),
    temperaturaAireC = 20f, temperaturaAguaC = 14f, actividad = "", comentarios = "", tipo = TipoInmersion.BUCEO
)

class InmersionesEnMemoria : RepositorioInmersiones {
    val filas = MutableStateFlow<List<Inmersion>>(emptyList())
    override val todasLasInmersiones: Flow<List<Inmersion>> = filas
    override fun obtenerPorId(id: Long) = filas.map { l -> l.firstOrNull { it.id == id } }
    override suspend fun guardar(inmersion: Inmersion): Long {
        val id = filas.value.size + 1L
        filas.value = filas.value + inmersion.copy(id = id)
        return id
    }
    override suspend fun actualizar(inmersion: Inmersion) = false
    override suspend fun eliminar(inmersion: Inmersion) = false
}

class SesionesEnMemoria : RepositorioSesionesApnea {
    val filas = MutableStateFlow<List<SesionApnea>>(emptyList())
    override val todasLasSesiones: Flow<List<SesionApnea>> = filas
    override suspend fun guardar(sesion: SesionApnea): Boolean {
        filas.value = filas.value + sesion
        return true
    }
}

class IntentosEnMemoria : RepositorioIntentos {
    override val todosLosIntentos: Flow<List<IntentoLibre>> = flowOf(emptyList())
    override fun delDia(fecha: LocalDate): Flow<List<IntentoLibre>> = flowOf(emptyList())
    override suspend fun guardar(intento: IntentoLibre) = true
}

class TablasEnMemoria : RepositorioTablas {
    override val todasLasTablas: Flow<List<TablaPersonalizada>> = flowOf(emptyList())
    override suspend fun guardar(tabla: TablaPersonalizada) = true
    override suspend fun eliminar(tabla: TablaPersonalizada) = true
}

/** Perfil vacío del usuario "ana" (el formulario de inmersión solo lee el equipo). */
class PerfilVacio : RepositorioPerfil {
    override val haySesion = true
    override val usuarioActual = "ana"
    override val perfilActual: Flow<PerfilUsuario?> = flowOf(null)
    override val equipamientoActual: Flow<List<Equipo>> = flowOf(emptyList())
    override val certificadosActuales: Flow<List<Certificado>> = flowOf(emptyList())
    override val contactosActuales: Flow<List<ContactoEmergencia>> = flowOf(emptyList())
    override suspend fun obtenerPerfil(): PerfilUsuario? = null
    override suspend fun guardarPerfil(perfil: PerfilUsuario) = Unit
    override suspend fun obtenerEquipo(id: Long): Equipo? = null
    override suspend fun guardarEquipo(equipo: Equipo) = 0L
    override suspend fun eliminarEquipo(id: Long) = false
    override suspend fun obtenerCertificado(id: Long): Certificado? = null
    override suspend fun guardarCertificado(certificado: Certificado) = 0L
    override suspend fun eliminarCertificado(id: Long) = false
    override suspend fun obtenerContacto(id: Long): ContactoEmergencia? = null
    override suspend fun cantidadContactos() = 0
    override suspend fun guardarContacto(contacto: ContactoEmergencia) = 0L
    override suspend fun hacerContactoPrincipal(id: Long) = Unit
    override suspend fun eliminarContacto(id: Long) = false
}

class SinGps : ServicioUbicacion {
    override fun tienePermiso() = false
    override fun ubicacionActivada() = false
    override suspend fun actual(timeout: Duration): Ubicacion? = null
    override fun ultima(): Ubicacion? = null
    override suspend fun nombreDelLugar(latitud: Double, longitud: Double): String? = null
}

/** Sin internet: solo la fase lunar. [conDatos] simula que Open-Meteo respondió. */
class CondicionesFijas(var conDatos: Boolean = false) : FuenteCondiciones {
    override suspend fun obtener(ubicacion: Ubicacion): Result<Condiciones> = Result.failure(UnsupportedOperationException())
    override suspend fun registroPara(latitud: Double, longitud: Double, momento: LocalDateTime) =
        CondicionesRegistro(temperaturaC = if (conDatos) 18.0 else null, faseLunar = "Llena", iluminacionLunaPorc = 100, conDatosOnline = conDatos)
    override fun registroAstronomico(latitud: Double, longitud: Double, momento: LocalDateTime) =
        CondicionesRegistro(faseLunar = "Llena", iluminacionLunaPorc = 100, conDatosOnline = false)
}
