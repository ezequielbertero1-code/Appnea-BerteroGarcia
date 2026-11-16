package com.berterogarcia.appnea.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// [CAMBIO v7] Todas las consultas filtran por usuario: nadie lee ni borra datos de otro.
@Dao
interface InmersionDao {
    /** Devuelve el id generado. */
    @Insert
    suspend fun insertar(inmersion: InmersionEntity): Long

    /** Completa las condiciones (clima, mar) cuando llegan de internet, después de guardar. */
    @Query("UPDATE inmersiones SET condiciones = :condiciones WHERE id = :id")
    suspend fun actualizarCondiciones(id: Long, condiciones: CondicionesRegistro?): Int

    /** Actualiza por id. Devuelve cuántas filas cambió (0 si no existía). */
    @Update
    suspend fun actualizar(inmersion: InmersionEntity): Int

    @Query("SELECT COUNT(*) FROM inmersiones WHERE id = :id AND nombreUsuario = :nombreUsuario")
    suspend fun contarPropia(id: Long, nombreUsuario: String): Int

    /**
     * Actualiza solo si la inmersión existe y es de [nombreUsuario]. La verificación y el UPDATE
     * van en la misma transacción: nadie puede borrarla o cambiarla en el medio.
     */
    @Transaction
    suspend fun actualizarSiEsDe(inmersion: InmersionEntity, nombreUsuario: String): Boolean {
        if (contarPropia(inmersion.id, nombreUsuario) == 0) return false
        return actualizar(inmersion.copy(nombreUsuario = nombreUsuario)) > 0
    }

    /** Devuelve la cantidad de filas borradas (0 si no existía o era de otro usuario). */
    @Query("DELETE FROM inmersiones WHERE id = :id AND nombreUsuario = :nombreUsuario")
    suspend fun eliminar(id: Long, nombreUsuario: String): Int

    @Query("SELECT * FROM inmersiones WHERE nombreUsuario = :nombreUsuario ORDER BY id DESC")
    fun obtenerTodas(nombreUsuario: String): Flow<List<InmersionEntity>>

    @Query("SELECT * FROM inmersiones WHERE id = :id AND nombreUsuario = :nombreUsuario")
    fun obtenerPorId(id: Long, nombreUsuario: String): Flow<InmersionEntity?>
}
