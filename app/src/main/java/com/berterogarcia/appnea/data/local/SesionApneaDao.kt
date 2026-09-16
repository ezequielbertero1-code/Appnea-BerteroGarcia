package com.berterogarcia.appnea.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SesionApneaDao {
    @Insert
    suspend fun insertar(sesion: SesionApneaEntity)

    @Query("SELECT * FROM sesiones_apnea ORDER BY id DESC")
    fun obtenerTodas(): Flow<List<SesionApneaEntity>>
}
