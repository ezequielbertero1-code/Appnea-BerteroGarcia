package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.data.local.CertificadoDao
import com.berterogarcia.appnea.data.local.ContactoEmergenciaDao
import com.berterogarcia.appnea.data.local.EquipoDao
import com.berterogarcia.appnea.data.local.PerfilUsuarioDao
import com.berterogarcia.appnea.data.local.aEntidad
import com.berterogarcia.appnea.data.local.aModelo
import com.berterogarcia.appnea.data.sesion.porUsuario
import com.berterogarcia.appnea.dominio.ProveedorUsuario
import com.berterogarcia.appnea.dominio.modelo.Certificado
import com.berterogarcia.appnea.dominio.modelo.ContactoEmergencia
import com.berterogarcia.appnea.dominio.modelo.Equipo
import com.berterogarcia.appnea.dominio.modelo.PerfilUsuario
import com.berterogarcia.appnea.dominio.repositorios.RepositorioPerfil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Punto único de acceso a todo lo que es "perfil del usuario": datos personales,
 * equipamiento, certificados y contactos de emergencia.
 *
 * Contrato:
 *  - La fuente de verdad del usuario es la sesión ([ProveedorUsuario]). Los flujos `...Actual`
 *    se re-suscriben solos al cambiar de cuenta.
 *  - Todo es sobre el usuario logueado: ningún ViewModel puede pedir datos de otra cuenta.
 *    Sin sesión, las escrituras lanzan IllegalStateException.
 *  - Las funciones suspend pueden lanzar excepciones (SQLite, FK, registro inexistente):
 *    el ViewModel las atrapa y muestra un mensaje, la app no crashea.
 */
class PerfilRepository(
    private val perfilDao: PerfilUsuarioDao,
    private val equipoDao: EquipoDao,
    private val certificadoDao: CertificadoDao,
    private val contactoDao: ContactoEmergenciaDao,
    private val sesion: ProveedorUsuario
) : RepositorioPerfil {

    override val haySesion: Boolean get() = sesion.usuarioActual.value != null
    override val usuarioActual: String? get() = sesion.usuarioActual.value

    override val perfilActual: Flow<PerfilUsuario?>
        get() = sesion.porUsuario(null) { u -> perfilDao.observar(u).map { it?.aModelo() } }
    override val equipamientoActual: Flow<List<Equipo>>
        get() = sesion.porUsuario(emptyList()) { u -> equipoDao.observarTodos(u).map { l -> l.map { it.aModelo() } } }
    override val certificadosActuales: Flow<List<Certificado>>
        get() = sesion.porUsuario(emptyList()) { u -> certificadoDao.observarTodos(u).map { l -> l.map { it.aModelo() } } }
    override val contactosActuales: Flow<List<ContactoEmergencia>>
        get() = sesion.porUsuario(emptyList()) { u -> contactoDao.observarTodos(u).map { l -> l.map { it.aModelo() } } }

    /** Usuario logueado; si no hay sesión, corta la operación. */
    private fun usuario(): String = checkNotNull(sesion.usuarioActual.value) { "No hay sesión iniciada." }

    // ============================ Datos personales ============================

    override suspend fun obtenerPerfil(): PerfilUsuario? = perfilDao.obtener(usuario())?.aModelo()

    override suspend fun guardarPerfil(perfil: PerfilUsuario) =
        perfilDao.guardar(perfil.copy(actualizadoEn = System.currentTimeMillis()).aEntidad(usuario()))

    // ============================== Equipamiento ==============================

    override suspend fun obtenerEquipo(id: Long): Equipo? = equipoDao.obtener(id, usuario())?.aModelo()

    override suspend fun guardarEquipo(equipo: Equipo): Long {
        val entidad = equipo.aEntidad(usuario())
        if (equipo.id == 0L) return equipoDao.insertar(entidad)
        check(equipoDao.actualizarSiEsDe(entidad)) { "El equipo no existe o no pertenece al usuario." }
        return equipo.id
    }

    override suspend fun eliminarEquipo(id: Long): Boolean = equipoDao.eliminar(id, usuario()) > 0

    // =============================== Certificados ===============================

    override suspend fun obtenerCertificado(id: Long): Certificado? = certificadoDao.obtener(id, usuario())?.aModelo()

    override suspend fun guardarCertificado(certificado: Certificado): Long {
        val entidad = certificado.aEntidad(usuario())
        if (certificado.id == 0L) return certificadoDao.insertar(entidad)
        check(certificadoDao.actualizarSiEsDe(entidad)) { "El certificado no existe o no pertenece al usuario." }
        return certificado.id
    }

    override suspend fun eliminarCertificado(id: Long): Boolean = certificadoDao.eliminar(id, usuario()) > 0

    // ========================= Contactos de emergencia =========================

    override suspend fun obtenerContacto(id: Long): ContactoEmergencia? = contactoDao.obtener(id, usuario())?.aModelo()

    override suspend fun cantidadContactos(): Int = contactoDao.contar(usuario())

    override suspend fun guardarContacto(contacto: ContactoEmergencia): Long = contactoDao.guardar(contacto.aEntidad(usuario()))

    override suspend fun hacerContactoPrincipal(id: Long) = contactoDao.hacerPrincipal(id, usuario())

    override suspend fun eliminarContacto(id: Long): Boolean = contactoDao.eliminarYReasignar(id, usuario()) > 0
}
