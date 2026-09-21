package com.berterogarcia.appnea.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SalidaPescaDao {
    @Insert
    suspend fun insertar(salida: SalidaPescaEntity)

    @Delete
    suspend fun eliminar(salida: SalidaPescaEntity)

    @Query("SELECT * FROM salidas_pesca ORDER BY fecha DESC, id DESC")
    fun obtenerTodas(): Flow<List<SalidaPescaEntity>>
}
