package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.data.local.CertificadoDao
import com.berterogarcia.appnea.data.local.CertificadoEntity
import com.berterogarcia.appnea.data.local.ContactoEmergenciaDao
import com.berterogarcia.appnea.data.local.ContactoEmergenciaEntity
import com.berterogarcia.appnea.data.local.EquipoDao
import com.berterogarcia.appnea.data.local.EquipoEntity
import com.berterogarcia.appnea.data.local.PerfilUsuarioDao
import com.berterogarcia.appnea.data.local.PerfilUsuarioEntity
import com.berterogarcia.appnea.data.local.ProveedorUsuario
import com.berterogarcia.appnea.data.local.porUsuario
import kotlinx.coroutines.flow.Flow

/**
 * Punto único de acceso a toodo lo que es "perfil del usuario": datos personales,
 * equipamiento, certificados y contactos de emergencia.
 *
 * Contrato:
 *  - La fuente de verdad del usuario es la sesión ([ProveedorUsuario]). Los flujos `...Actual`
 *    se re-suscriben solos al cambiar de cuenta.
 *  - Las funciones que reciben `nombreUsuario` (o una entidad con él) verifican que sea el
 *    usuario logueado: si no, lanzan IllegalStateException. Así ningún ViewModel con un dato
 *    viejo puede leer ni pisar datos de otra cuenta.
 *  - Las funciones suspend pueden lanzar excepciones (SQLite, FK, registro inexistente):
 *    el ViewModel las atrapa con runCatching y muestra un mensaje, la app no crashea.
 */
class PerfilRepository(
    private val perfilDao: PerfilUsuarioDao,
    private val equipoDao: EquipoDao,
    private val certificadoDao: CertificadoDao,
    private val contactoDao: ContactoEmergenciaDao,
    private val sesion: ProveedorUsuario
) {
    /** Usuario logueado ahora (o null). */
    val usuarioActual: String? get() = sesion.usuarioActual.value

    val perfilActual: Flow<PerfilUsuarioEntity?> get() = sesion.porUsuario(null) { perfilDao.observar(it) }
    val equipamientoActual: Flow<List<EquipoEntity>> get() = sesion.porUsuario(emptyList()) { equipoDao.observarTodos(it) }
    val certificadosActuales: Flow<List<CertificadoEntity>> get() = sesion.porUsuario(emptyList()) { certificadoDao.observarTodos(it) }
    val contactosActuales: Flow<List<ContactoEmergenciaEntity>> get() = sesion.porUsuario(emptyList()) { contactoDao.observarTodos(it) }

    /** Devuelve [nombreUsuario] si es el logueado; si no, corta la operación. */
    private fun propio(nombreUsuario: String): String {
        check(nombreUsuario == sesion.usuarioActual.value) { "La operación no es sobre el usuario logueado." }
        return nombreUsuario
    }

    // ============================ Datos personales ============================

    fun observarPerfil(nombreUsuario: String): Flow<PerfilUsuarioEntity?> =
        perfilDao.observar(propio(nombreUsuario))

    suspend fun obtenerPerfil(nombreUsuario: String): PerfilUsuarioEntity? =
        perfilDao.obtener(propio(nombreUsuario))

    suspend fun guardarPerfil(perfil: PerfilUsuarioEntity) =
        perfilDao.guardar(perfil.copy(nombreUsuario = propio(perfil.nombreUsuario), actualizadoEn = System.currentTimeMillis()))

    // ============================== Equipamiento ==============================

    fun observarEquipamiento(nombreUsuario: String): Flow<List<EquipoEntity>> =
        equipoDao.observarTodos(propio(nombreUsuario))

    suspend fun obtenerEquipo(id: Long, nombreUsuario: String): EquipoEntity? =
        equipoDao.obtener(id, propio(nombreUsuario))

    /** Inserta si id == 0, si no actualiza (verificando que sea del usuario). Devuelve el id. */
    suspend fun guardarEquipo(equipo: EquipoEntity): Long {
        propio(equipo.nombreUsuario)
        if (equipo.id == 0L) return equipoDao.insertar(equipo)
        check(equipoDao.actualizarSiEsDe(equipo)) { "El equipo no existe o no pertenece al usuario." }
        return equipo.id
    }

    suspend fun eliminarEquipo(id: Long, nombreUsuario: String): Boolean =
        equipoDao.eliminar(id, propio(nombreUsuario)) > 0

    // =============================== Certificados ===============================

    fun observarCertificados(nombreUsuario: String): Flow<List<CertificadoEntity>> =
        certificadoDao.observarTodos(propio(nombreUsuario))

    suspend fun obtenerCertificado(id: Long, nombreUsuario: String): CertificadoEntity? =
        certificadoDao.obtener(id, propio(nombreUsuario))

    suspend fun guardarCertificado(certificado: CertificadoEntity): Long {
        propio(certificado.nombreUsuario)
        if (certificado.id == 0L) return certificadoDao.insertar(certificado)
        check(certificadoDao.actualizarSiEsDe(certificado)) { "El certificado no existe o no pertenece al usuario." }
        return certificado.id
    }

    suspend fun eliminarCertificado(id: Long, nombreUsuario: String): Boolean =
        certificadoDao.eliminar(id, propio(nombreUsuario)) > 0

    // ========================= Contactos de emergencia =========================

    fun observarContactos(nombreUsuario: String): Flow<List<ContactoEmergenciaEntity>> =
        contactoDao.observarTodos(propio(nombreUsuario))

    suspend fun obtenerContacto(id: Long, nombreUsuario: String): ContactoEmergenciaEntity? =
        contactoDao.obtener(id, propio(nombreUsuario))

    suspend fun cantidadContactos(nombreUsuario: String): Int =
        contactoDao.contar(propio(nombreUsuario))

    /** La regla "siempre hay un principal" la garantiza el DAO dentro de una transacción. */
    suspend fun guardarContacto(contacto: ContactoEmergenciaEntity): Long {
        propio(contacto.nombreUsuario)
        return contactoDao.guardar(contacto)
    }

    suspend fun hacerContactoPrincipal(id: Long, nombreUsuario: String) =
        contactoDao.hacerPrincipal(id, propio(nombreUsuario))

    suspend fun eliminarContacto(id: Long, nombreUsuario: String): Boolean =
        contactoDao.eliminarYReasignar(id, propio(nombreUsuario)) > 0
}
