package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.data.local.CertificadoDao
import com.berterogarcia.appnea.data.local.CertificadoEntity
import com.berterogarcia.appnea.data.local.ContactoEmergenciaDao
import com.berterogarcia.appnea.data.local.ContactoEmergenciaEntity
import com.berterogarcia.appnea.data.local.EquipoDao
import com.berterogarcia.appnea.data.local.EquipoEntity
import com.berterogarcia.appnea.data.local.PerfilUsuarioDao
import com.berterogarcia.appnea.data.local.PerfilUsuarioEntity
import kotlinx.coroutines.flow.Flow

/**
 * Punto único de acceso a toodo lo que es "perfil del usuario": datos personales,
 * equipamiento, certificados y contactos de emergencia.
 *
 * Contrato:
 *  - Todas las funciones reciben el `nombreUsuario` (lo pone el ViewModel a partir de la
 *    sesión) y nunca devuelven/modifican datos de otro usuario.
 *  - Las funciones suspend pueden lanzar excepciones (SQLite, FK, registro inexistente):
 *    el ViewModel las atrapa con runCatching y muestra un mensaje, la app no crashea.
 */
class PerfilRepository(
    private val perfilDao: PerfilUsuarioDao,
    private val equipoDao: EquipoDao,
    private val certificadoDao: CertificadoDao,
    private val contactoDao: ContactoEmergenciaDao
) {

    // ============================ Datos personales ============================

    fun observarPerfil(nombreUsuario: String): Flow<PerfilUsuarioEntity?> =
        perfilDao.observar(nombreUsuario)

    suspend fun obtenerPerfil(nombreUsuario: String): PerfilUsuarioEntity? =
        perfilDao.obtener(nombreUsuario)

    suspend fun guardarPerfil(perfil: PerfilUsuarioEntity) =
        perfilDao.guardar(perfil.copy(actualizadoEn = System.currentTimeMillis()))

    // ============================== Equipamiento ==============================

    fun observarEquipamiento(nombreUsuario: String): Flow<List<EquipoEntity>> =
        equipoDao.observarTodos(nombreUsuario)

    suspend fun obtenerEquipo(id: Long, nombreUsuario: String): EquipoEntity? =
        equipoDao.obtener(id, nombreUsuario)

    /** Inserta si id == 0, si no actualiza (verificando que sea del usuario). Devuelve el id. */
    suspend fun guardarEquipo(equipo: EquipoEntity): Long {
        if (equipo.id == 0L) return equipoDao.insertar(equipo)
        checkNotNull(equipoDao.obtener(equipo.id, equipo.nombreUsuario)) {
            "El equipo no existe o no pertenece al usuario."
        }
        equipoDao.actualizar(equipo)
        return equipo.id
    }

    suspend fun eliminarEquipo(id: Long, nombreUsuario: String): Boolean =
        equipoDao.eliminar(id, nombreUsuario) > 0

    // =============================== Certificados ===============================

    fun observarCertificados(nombreUsuario: String): Flow<List<CertificadoEntity>> =
        certificadoDao.observarTodos(nombreUsuario)

    suspend fun obtenerCertificado(id: Long, nombreUsuario: String): CertificadoEntity? =
        certificadoDao.obtener(id, nombreUsuario)

    suspend fun guardarCertificado(certificado: CertificadoEntity): Long {
        if (certificado.id == 0L) return certificadoDao.insertar(certificado)
        checkNotNull(certificadoDao.obtener(certificado.id, certificado.nombreUsuario)) {
            "El certificado no existe o no pertenece al usuario."
        }
        certificadoDao.actualizar(certificado)
        return certificado.id
    }

    suspend fun eliminarCertificado(id: Long, nombreUsuario: String): Boolean =
        certificadoDao.eliminar(id, nombreUsuario) > 0

    // ========================= Contactos de emergencia =========================

    fun observarContactos(nombreUsuario: String): Flow<List<ContactoEmergenciaEntity>> =
        contactoDao.observarTodos(nombreUsuario)

    suspend fun obtenerContacto(id: Long, nombreUsuario: String): ContactoEmergenciaEntity? =
        contactoDao.obtener(id, nombreUsuario)

    suspend fun cantidadContactos(nombreUsuario: String): Int =
        contactoDao.contar(nombreUsuario)

    /** La regla "siempre hay un principal" la garantiza el DAO dentro de una transacción. */
    suspend fun guardarContacto(contacto: ContactoEmergenciaEntity): Long =
        contactoDao.guardar(contacto)

    suspend fun hacerContactoPrincipal(id: Long, nombreUsuario: String) =
        contactoDao.hacerPrincipal(id, nombreUsuario)

    suspend fun eliminarContacto(id: Long, nombreUsuario: String): Boolean =
        contactoDao.eliminarYReasignar(id, nombreUsuario) > 0
}
