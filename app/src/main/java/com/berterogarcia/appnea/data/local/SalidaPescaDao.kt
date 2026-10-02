package com.berterogarcia.appnea.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// [CAMBIO v7] Filtra por usuario.
@Dao
interface SalidaPescaDao {
    @Insert
    suspend fun insertar(salida: SalidaPescaEntity)

    @Query("DELETE FROM salidas_pesca WHERE id = :id AND nombreUsuario = :nombreUsuario")
    suspend fun eliminar(id: Long, nombreUsuario: String): Int

    @Query("SELECT * FROM salidas_pesca WHERE nombreUsuario = :nombreUsuario ORDER BY fecha DESC, id DESC")
    fun obtenerTodas(nombreUsuario: String): Flow<List<SalidaPescaEntity>>
}
