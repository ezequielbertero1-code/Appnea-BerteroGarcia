package com.berterogarcia.appnea.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Es una abstract class (y no una interface como los otros DAO) porque necesita
 * métodos con lógica propia anotados con @Transaction. En una abstract class Room
 * los sobreescribe sin vueltas; tienen que ser `open`.
 *
 * Regla que se mantiene: si el usuario tiene al menos un contacto, exactamente
 * UNO es principal.
 */
@Dao
abstract class ContactoEmergenciaDao {

    // ---------------------------- Consultas ----------------------------

    @Query(
        "SELECT * FROM contactos_emergencia WHERE nombreUsuario = :nombreUsuario " +
            "ORDER BY esPrincipal DESC, nombre COLLATE NOCASE"
    )
    abstract fun observarTodos(nombreUsuario: String): Flow<List<ContactoEmergenciaEntity>>

    @Query("SELECT * FROM contactos_emergencia WHERE id = :id AND nombreUsuario = :nombreUsuario LIMIT 1")
    abstract suspend fun obtener(id: Long, nombreUsuario: String): ContactoEmergenciaEntity?

    @Query("SELECT COUNT(*) FROM contactos_emergencia WHERE nombreUsuario = :nombreUsuario")
    abstract suspend fun contar(nombreUsuario: String): Int

    // ------------------- Operaciones básicas (protegidas) -------------------
    // No se exponen directamente para que nadie se saltee la regla del principal.

    @Insert
    protected abstract suspend fun insertar(contacto: ContactoEmergenciaEntity): Long

    @Update
    protected abstract suspend fun actualizar(contacto: ContactoEmergenciaEntity): Int

    @Query("DELETE FROM contactos_emergencia WHERE id = :id AND nombreUsuario = :nombreUsuario")
    protected abstract suspend fun eliminar(id: Long, nombreUsuario: String): Int

    @Query("UPDATE contactos_emergencia SET esPrincipal = 0 WHERE nombreUsuario = :nombreUsuario")
    protected abstract suspend fun quitarPrincipales(nombreUsuario: String)

    @Query("UPDATE contactos_emergencia SET esPrincipal = 1 WHERE id = :id AND nombreUsuario = :nombreUsuario")
    protected abstract suspend fun marcarComoPrincipal(id: Long, nombreUsuario: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM contactos_emergencia WHERE nombreUsuario = :nombreUsuario AND esPrincipal = 1)")
    protected abstract suspend fun tienePrincipal(nombreUsuario: String): Boolean

    @Query("SELECT id FROM contactos_emergencia WHERE nombreUsuario = :nombreUsuario ORDER BY id LIMIT 1")
    protected abstract suspend fun primerId(nombreUsuario: String): Long?

    // ---------------------- Operaciones con reglas ----------------------

    /**
     * Inserta (id == 0) o actualiza un contacto respetando la regla del principal.
     * @return el id del contacto guardado.
     * @throws IllegalStateException si se intenta editar un contacto que no existe o no es del usuario.
     */
    @Transaction
    open suspend fun guardar(contacto: ContactoEmergenciaEntity): Long {
        val usuario = contacto.nombreUsuario
        if (contacto.id != 0L) {
            checkNotNull(obtener(contacto.id, usuario)) { "El contacto no existe o no pertenece al usuario." }
        }
        if (contacto.esPrincipal) quitarPrincipales(usuario)

        val id = if (contacto.id == 0L) {
            insertar(contacto)
        } else {
            actualizar(contacto)
            contacto.id
        }
        asegurarUnPrincipal(usuario)
        return id
    }

    /** Elimina y, si se borró el principal, promueve al contacto más antiguo. */
    @Transaction
    open suspend fun eliminarYReasignar(id: Long, nombreUsuario: String): Int {
        val borrados = eliminar(id, nombreUsuario)
        asegurarUnPrincipal(nombreUsuario)
        return borrados
    }

    /** Marca un contacto como principal y desmarca el resto, toodo junto. */
    @Transaction
    open suspend fun hacerPrincipal(id: Long, nombreUsuario: String) {
        checkNotNull(obtener(id, nombreUsuario)) { "El contacto no existe o no pertenece al usuario." }
        quitarPrincipales(nombreUsuario)
        marcarComoPrincipal(id, nombreUsuario)
    }

    private suspend fun asegurarUnPrincipal(nombreUsuario: String) {
        if (!tienePrincipal(nombreUsuario)) {
            primerId(nombreUsuario)?.let { marcarComoPrincipal(it, nombreUsuario) }
        }
    }
}
