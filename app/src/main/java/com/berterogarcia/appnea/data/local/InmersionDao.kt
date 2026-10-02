package com.berterogarcia.appnea.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// [CAMBIO v7] Todas las consultas filtran por usuario: nadie lee ni borra datos de otro.
@Dao
interface InmersionDao {
    @Insert
    suspend fun insertar(inmersion: InmersionEntity)

    /** Devuelve la cantidad de filas borradas (0 si no existía o era de otro usuario). */
    @Query("DELETE FROM inmersiones WHERE id = :id AND nombreUsuario = :nombreUsuario")
    suspend fun eliminar(id: Long, nombreUsuario: String): Int

    @Query("SELECT * FROM inmersiones WHERE nombreUsuario = :nombreUsuario ORDER BY id DESC")
    fun obtenerTodas(nombreUsuario: String): Flow<List<InmersionEntity>>

    @Query("SELECT * FROM inmersiones WHERE id = :id AND nombreUsuario = :nombreUsuario")
    fun obtenerPorId(id: Long, nombreUsuario: String): Flow<InmersionEntity?>
}
