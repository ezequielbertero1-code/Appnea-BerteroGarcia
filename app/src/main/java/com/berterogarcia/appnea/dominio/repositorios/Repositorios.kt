package com.berterogarcia.appnea.dominio.repositorios

import com.berterogarcia.appnea.dominio.modelo.Certificado
import com.berterogarcia.appnea.dominio.modelo.ContactoEmergencia
import com.berterogarcia.appnea.dominio.modelo.Equipo
import com.berterogarcia.appnea.dominio.modelo.Inmersion
import com.berterogarcia.appnea.dominio.modelo.IntentoLibre
import com.berterogarcia.appnea.dominio.modelo.PerfilUsuario
import com.berterogarcia.appnea.dominio.modelo.SesionApnea
import com.berterogarcia.appnea.dominio.modelo.TablaPersonalizada
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/*
 * Lo que las pantallas necesitan de los datos, como interfaces (inversión de dependencias).
 * Todo es sobre el usuario logueado: los flujos se re-suscriben solos al cambiar de cuenta y,
 * sin sesión, emiten vacío. Las implementaciones (Room) están en data/repository.
 */

interface RepositorioInmersiones {
    val todasLasInmersiones: Flow<List<Inmersion>>
    fun obtenerPorId(id: Long): Flow<Inmersion?>
    /** @return el id de la inmersión nueva, o null si no hay sesión. */
    suspend fun guardar(inmersion: Inmersion): Long?
    /** @return false si no hay sesión o si la inmersión no es del usuario logueado. */
    suspend fun actualizar(inmersion: Inmersion): Boolean
    /** @return true si se borró. */
    suspend fun eliminar(inmersion: Inmersion): Boolean
}

interface RepositorioSesionesApnea {
    val todasLasSesiones: Flow<List<SesionApnea>>
    /** @return false si no hay sesión. */
    suspend fun guardar(sesion: SesionApnea): Boolean
}

interface RepositorioIntentos {
    val todosLosIntentos: Flow<List<IntentoLibre>>
    fun delDia(fecha: LocalDate): Flow<List<IntentoLibre>>
    /** @return false si no hay sesión. */
    suspend fun guardar(intento: IntentoLibre): Boolean
}

interface RepositorioTablas {
    val todasLasTablas: Flow<List<TablaPersonalizada>>
    /** @return false si no hay sesión. */
    suspend fun guardar(tabla: TablaPersonalizada): Boolean
    /** @return true si se borró. */
    suspend fun eliminar(tabla: TablaPersonalizada): Boolean
}

/**
 * Perfil, equipamiento, certificados y contactos del usuario logueado.
 * Las operaciones de escritura fallan con IllegalStateException si no hay sesión.
 */
interface RepositorioPerfil {
    /** false si no hay nadie logueado (las pantallas de perfil muestran un aviso). */
    val haySesion: Boolean
    /** Nombre de usuario logueado (o null). */
    val usuarioActual: String?
    val perfilActual: Flow<PerfilUsuario?>
    val equipamientoActual: Flow<List<Equipo>>
    val certificadosActuales: Flow<List<Certificado>>
    val contactosActuales: Flow<List<ContactoEmergencia>>

    suspend fun obtenerPerfil(): PerfilUsuario?
    suspend fun guardarPerfil(perfil: PerfilUsuario)

    suspend fun obtenerEquipo(id: Long): Equipo?
    /** Inserta si id == 0, si no actualiza. Devuelve el id. */
    suspend fun guardarEquipo(equipo: Equipo): Long
    suspend fun eliminarEquipo(id: Long): Boolean

    suspend fun obtenerCertificado(id: Long): Certificado?
    suspend fun guardarCertificado(certificado: Certificado): Long
    suspend fun eliminarCertificado(id: Long): Boolean

    suspend fun obtenerContacto(id: Long): ContactoEmergencia?
    suspend fun cantidadContactos(): Int
    /** Siempre queda un contacto principal (lo garantiza la base en una transacción). */
    suspend fun guardarContacto(contacto: ContactoEmergencia): Long
    suspend fun hacerContactoPrincipal(id: Long)
    suspend fun eliminarContacto(id: Long): Boolean
}

/** Login con usuario y contraseña. */
interface RepositorioUsuarios {
    /** @return true si usuario y contraseña son correctos. */
    suspend fun validar(nombreUsuario: String, password: String): Boolean
}

/** Todo lo que se carga en el registro. */
data class NuevoRegistro(
    val usuario: String,
    val password: String,
    val perfil: PerfilUsuario,
    val contacto: ContactoEmergencia
)

sealed interface ResultadoRegistro {
    data object Creado : ResultadoRegistro
    data object UsuarioExistente : ResultadoRegistro
}

/** Crear una cuenta: usuario + perfil + contacto de emergencia, todo junto o nada. */
interface RepositorioRegistro {
    suspend fun usuarioDisponible(usuario: String): Boolean
    suspend fun registrar(datos: NuevoRegistro): ResultadoRegistro
}

/** Cuentas que entran con Google (no tienen contraseña). */
interface RepositorioCuentaGoogle {
    /** true si esta cuenta ya completó el registro. */
    suspend fun existe(email: String): Boolean
    suspend fun registrar(email: String, perfil: PerfilUsuario, contacto: ContactoEmergencia)
}

/** Al abrir la app: ¿hay una sesión guardada válida? */
interface RepositorioSesion {
    /** @return el usuario logueado si la sesión es válida, o null si hay que ir al login. */
    suspend fun usuarioConSesionValida(): String?
}
