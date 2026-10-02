package com.berterogarcia.appnea.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// [CAMBIO v7] Filtra por usuario.
@Dao
interface TablaPersonalizadaDao {
    @Insert
    suspend fun insertar(tabla: TablaPersonalizadaEntity)

    @Query("DELETE FROM tablas_personalizadas WHERE id = :id AND nombreUsuario = :nombreUsuario")
    suspend fun eliminar(id: Long, nombreUsuario: String): Int

    @Query("SELECT * FROM tablas_personalizadas WHERE nombreUsuario = :nombreUsuario ORDER BY id DESC")
    fun obtenerTodas(nombreUsuario: String): Flow<List<TablaPersonalizadaEntity>>
}
