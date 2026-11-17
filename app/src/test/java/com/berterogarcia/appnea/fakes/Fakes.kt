package com.berterogarcia.appnea.fakes

import com.berterogarcia.appnea.dominio.EstadoIntentos
import com.berterogarcia.appnea.dominio.FuenteCondiciones
import com.berterogarcia.appnea.dominio.GestorSesion
import com.berterogarcia.appnea.dominio.PreferenciasLogin
import com.berterogarcia.appnea.dominio.ProgramadorCondiciones
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
import com.berterogarcia.appnea.dominio.modelo.Ubicacion
import com.berterogarcia.appnea.dominio.repositorios.NuevoRegistro
import com.berterogarcia.appnea.dominio.repositorios.RepositorioCuentaGoogle
import com.berterogarcia.appnea.dominio.repositorios.RepositorioInmersiones
import com.berterogarcia.appnea.dominio.repositorios.RepositorioIntentos
import com.berterogarcia.appnea.dominio.repositorios.RepositorioPerfil
import com.berterogarcia.appnea.dominio.repositorios.RepositorioRegistro
import com.berterogarcia.appnea.dominio.repositorios.RepositorioSesionesApnea
import com.berterogarcia.appnea.dominio.repositorios.RepositorioTablas
import com.berterogarcia.appnea.dominio.repositorios.RepositorioUsuarios
import com.berterogarcia.appnea.dominio.repositorios.ResultadoRegistro
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.time.Duration

/*
 * Versiones en memoria de los repositorios y puertos, para probar ViewModels y casos de uso
 * sin Room ni Android. `falla = true` simula un error de la base (lanzan una excepción).
 */

class FakeSesion(usuario: String? = "ana") : GestorSesion {
    override val usuarioActual = MutableStateFlow(usuario)
    override fun guardarUsuario(nombreUsuario: String) { usuarioActual.value = nombreUsuario }
    override fun obtenerUsuario(): String? = usuarioActual.value
    override fun cerrarSesion() { usuarioActual.value = null }
}

class FakePreferenciasLogin(override var ultimoUsuario: String? = null) : PreferenciasLogin {
    var intentos = EstadoIntentos()
    override fun leerIntentos() = intentos
    override fun guardarIntentos(estado: EstadoIntentos) { intentos = estado }
}

class FakeUsuarios(private val validos: Map<String, String> = mapOf("ana" to "clave1234")) : RepositorioUsuarios {
    var falla = false
    override suspend fun validar(nombreUsuario: String, password: String): Boolean {
        if (falla) error("base caída")
        return validos[nombreUsuario] == password
    }
}

class FakeCuentasGoogle(val existentes: MutableSet<String> = mutableSetOf()) : RepositorioCuentaGoogle {
    val registradas = mutableListOf<Triple<String, PerfilUsuario, ContactoEmergencia>>()
    override suspend fun existe(email: String) = email in existentes
    override suspend fun registrar(email: String, perfil: PerfilUsuario, contacto: ContactoEmergencia) {
        existentes += email
        registradas += Triple(email, perfil, contacto)
    }
}

class FakeRegistro(val ocupados: MutableSet<String> = mutableSetOf()) : RepositorioRegistro {
    val registros = mutableListOf<NuevoRegistro>()
    var falla = false
    override suspend fun usuarioDisponible(usuario: String) = usuario !in ocupados
    override suspend fun registrar(datos: NuevoRegistro): ResultadoRegistro {
        if (falla) error("base caída")
        if (datos.usuario in ocupados) return ResultadoRegistro.UsuarioExistente
        ocupados += datos.usuario
        registros += datos
        return ResultadoRegistro.Creado
    }
}

class FakeInmersiones(iniciales: List<Inmersion> = emptyList()) : RepositorioInmersiones {
    val filas = MutableStateFlow(iniciales)
    var falla = false
    /** Simula que se cerró la sesión: guardar devuelve null y actualizar false. */
    var sinSesion = false
    override val todasLasInmersiones: Flow<List<Inmersion>> = filas
    override fun obtenerPorId(id: Long): Flow<Inmersion?> = filas.map { l -> l.firstOrNull { it.id == id } }
    override suspend fun guardar(inmersion: Inmersion): Long? {
        if (falla) error("base caída")
        if (sinSesion) return null
        val id = (filas.value.maxOfOrNull { it.id } ?: 0L) + 1
        filas.value = filas.value + inmersion.copy(id = id)
        return id
    }
    override suspend fun actualizar(inmersion: Inmersion): Boolean {
        if (falla) error("base caída")
        if (sinSesion || filas.value.none { it.id == inmersion.id }) return false
        filas.value = filas.value.map { if (it.id == inmersion.id) inmersion else it }
        return true
    }
    override suspend fun eliminar(inmersion: Inmersion): Boolean {
        val antes = filas.value.size
        filas.value = filas.value.filter { it.id != inmersion.id }
        return filas.value.size < antes
    }
}

class FakeSesionesApnea(iniciales: List<SesionApnea> = emptyList()) : RepositorioSesionesApnea {
    val filas = MutableStateFlow(iniciales)
    var falla = false
    override val todasLasSesiones: Flow<List<SesionApnea>> = filas
    override suspend fun guardar(sesion: SesionApnea): Boolean {
        if (falla) error("base caída")
        filas.value = filas.value + sesion.copy(id = filas.value.size + 1L)
        return true
    }
}

class FakeIntentos(iniciales: List<IntentoLibre> = emptyList()) : RepositorioIntentos {
    val filas = MutableStateFlow(iniciales)
    override val todosLosIntentos: Flow<List<IntentoLibre>> = filas
    override fun delDia(fecha: LocalDate): Flow<List<IntentoLibre>> = filas.map { l -> l.filter { it.fecha == fecha } }
    override suspend fun guardar(intento: IntentoLibre): Boolean {
        filas.value = listOf(intento.copy(id = filas.value.size + 1L)) + filas.value
        return true
    }
}

class FakeTablas(iniciales: List<TablaPersonalizada> = emptyList()) : RepositorioTablas {
    val filas = MutableStateFlow(iniciales)
    override val todasLasTablas: Flow<List<TablaPersonalizada>> = filas
    override suspend fun guardar(tabla: TablaPersonalizada): Boolean {
        filas.value = filas.value + tabla.copy(id = filas.value.size + 1L)
        return true
    }
    override suspend fun eliminar(tabla: TablaPersonalizada): Boolean {
        val antes = filas.value.size
        filas.value = filas.value.filter { it.id != tabla.id }
        return filas.value.size < antes
    }
}

class FakePerfil(
    perfil: PerfilUsuario? = null,
    equipos: List<Equipo> = emptyList(),
    certificados: List<Certificado> = emptyList(),
    contactos: List<ContactoEmergencia> = emptyList(),
    private val sesion: StateFlow<String?> = MutableStateFlow("ana")
) : RepositorioPerfil {
    val perfil = MutableStateFlow(perfil)
    val equipos = MutableStateFlow(equipos)
    val certificados = MutableStateFlow(certificados)
    val contactos = MutableStateFlow(contactos)

    override val haySesion get() = sesion.value != null
    override val usuarioActual get() = sesion.value
    override val perfilActual: Flow<PerfilUsuario?> = this.perfil
    override val equipamientoActual: Flow<List<Equipo>> = this.equipos
    override val certificadosActuales: Flow<List<Certificado>> = this.certificados
    override val contactosActuales: Flow<List<ContactoEmergencia>> = this.contactos

    override suspend fun obtenerPerfil() = perfil.value
    override suspend fun guardarPerfil(perfil: PerfilUsuario) { this.perfil.value = perfil }
    override suspend fun obtenerEquipo(id: Long) = equipos.value.firstOrNull { it.id == id }
    override suspend fun guardarEquipo(equipo: Equipo): Long {
        val id = equipo.id.takeIf { it > 0 } ?: (equipos.value.size + 1L)
        equipos.value = equipos.value.filter { it.id != id } + equipo.copy(id = id)
        return id
    }
    override suspend fun eliminarEquipo(id: Long) = equipos.value.any { it.id == id }.also {
        equipos.value = equipos.value.filter { e -> e.id != id }
    }
    override suspend fun obtenerCertificado(id: Long) = certificados.value.firstOrNull { it.id == id }
    override suspend fun guardarCertificado(certificado: Certificado): Long {
        val id = certificado.id.takeIf { it > 0 } ?: (certificados.value.size + 1L)
        certificados.value = certificados.value.filter { it.id != id } + certificado.copy(id = id)
        return id
    }
    override suspend fun eliminarCertificado(id: Long) = certificados.value.any { it.id == id }.also {
        certificados.value = certificados.value.filter { c -> c.id != id }
    }
    override suspend fun obtenerContacto(id: Long) = contactos.value.firstOrNull { it.id == id }
    override suspend fun cantidadContactos() = contactos.value.size
    override suspend fun guardarContacto(contacto: ContactoEmergencia): Long {
        val id = contacto.id.takeIf { it > 0 } ?: (contactos.value.size + 1L)
        contactos.value = contactos.value.filter { it.id != id } + contacto.copy(id = id)
        return id
    }
    override suspend fun hacerContactoPrincipal(id: Long) {
        contactos.value = contactos.value.map { it.copy(esPrincipal = it.id == id) }
    }
    override suspend fun eliminarContacto(id: Long) = contactos.value.any { it.id == id }.also {
        contactos.value = contactos.value.filter { c -> c.id != id }
    }
}

/** GPS falso: [actual] devuelve lo configurado (null = no encontró señal). */
class FakeUbicacion(
    var actual: Ubicacion? = null,
    var ultima: Ubicacion? = null,
    var activada: Boolean = true,
    var nombre: String? = null
) : ServicioUbicacion {
    override fun tienePermiso() = true
    override fun ubicacionActivada() = activada
    override suspend fun actual(timeout: Duration) = actual
    override fun ultima() = ultima
    override suspend fun nombreDelLugar(latitud: Double, longitud: Double) = nombre
}

/** Condiciones falsas: sin internet, solo la fase lunar. */
class FakeCondiciones : FuenteCondiciones {
    val pedidos = mutableListOf<Pair<Ubicacion, LocalDateTime>>()
    override suspend fun obtener(ubicacion: Ubicacion): Result<Condiciones> = Result.failure(UnsupportedOperationException())
    override suspend fun registroPara(latitud: Double, longitud: Double, momento: LocalDateTime) =
        registroAstronomico(latitud, longitud, momento)
    override fun registroAstronomico(latitud: Double, longitud: Double, momento: LocalDateTime): CondicionesRegistro {
        pedidos += Ubicacion(latitud, longitud) to momento
        return CondicionesRegistro(faseLunar = "Llena", iluminacionLunaPorc = 100, conDatosOnline = false)
    }
}

/** Anota qué se pidió completar en segundo plano. */
class FakeProgramador : ProgramadorCondiciones {
    val programados = mutableListOf<Triple<Long, Ubicacion, LocalDateTime>>()
    override fun programar(idInmersion: Long, ubicacion: Ubicacion, momento: LocalDateTime) {
        programados += Triple(idInmersion, ubicacion, momento)
    }
}
