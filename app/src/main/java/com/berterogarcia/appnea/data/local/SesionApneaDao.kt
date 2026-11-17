package com.berterogarcia.appnea.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SesionApneaDao {
    @Insert
    suspend fun insertar(sesion: SesionApneaEntity)

    @Query("SELECT * FROM sesiones_apnea WHERE nombreUsuario = :nombreUsuario ORDER BY fecha DESC, horaInicio DESC, id DESC")
    fun obtenerTodas(nombreUsuario: String): Flow<List<SesionApneaEntity>>
}
