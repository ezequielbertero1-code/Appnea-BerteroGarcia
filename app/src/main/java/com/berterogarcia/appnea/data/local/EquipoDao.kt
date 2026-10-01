package com.berterogarcia.appnea.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Todas las consultas filtran por `nombreUsuario`: aunque se pase un id de otro
 * usuario, nunca se lee ni se borra algo que no le pertenece.
 */
@Dao
interface EquipoDao {
    @Query("SELECT * FROM equipamiento WHERE nombreUsuario = :nombreUsuario ORDER BY enUso DESC, id DESC")
    fun observarTodos(nombreUsuario: String): Flow<List<EquipoEntity>>

    @Query("SELECT * FROM equipamiento WHERE id = :id AND nombreUsuario = :nombreUsuario LIMIT 1")
    suspend fun obtener(id: Long, nombreUsuario: String): EquipoEntity?

    @Query("SELECT COUNT(*) FROM equipamiento WHERE nombreUsuario = :nombreUsuario")
    suspend fun contar(nombreUsuario: String): Int

    @Insert
    suspend fun insertar(equipo: EquipoEntity): Long

    /** Devuelve la cantidad de filas actualizadas (0 = no existía). */
    @Update
    suspend fun actualizar(equipo: EquipoEntity): Int

    @Query("DELETE FROM equipamiento WHERE id = :id AND nombreUsuario = :nombreUsuario")
    suspend fun eliminar(id: Long, nombreUsuario: String): Int
}
