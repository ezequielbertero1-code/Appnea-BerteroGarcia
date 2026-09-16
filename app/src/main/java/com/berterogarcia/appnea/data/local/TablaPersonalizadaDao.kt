package com.berterogarcia.appnea.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TablaPersonalizadaDao {
    @Insert
    suspend fun insertar(tabla: TablaPersonalizadaEntity)

    @Delete
    suspend fun eliminar(tabla: TablaPersonalizadaEntity)

    @Query("SELECT * FROM tablas_personalizadas ORDER BY id DESC")
    fun obtenerTodas(): Flow<List<TablaPersonalizadaEntity>>
}
