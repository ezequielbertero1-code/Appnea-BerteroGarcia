package com.berterogarcia.appnea.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface IntentoApneaDao {
    @Insert
    suspend fun insertar(intento: IntentoApneaEntity): Long

    @Query("SELECT * FROM intentos_apnea WHERE nombreUsuario = :nombreUsuario ORDER BY fecha DESC, hora DESC, id DESC")
    fun obtenerTodos(nombreUsuario: String): Flow<List<IntentoApneaEntity>>

    @Query("SELECT * FROM intentos_apnea WHERE nombreUsuario = :nombreUsuario AND fecha = :fecha ORDER BY hora DESC, id DESC")
    fun obtenerDelDia(nombreUsuario: String, fecha: LocalDate): Flow<List<IntentoApneaEntity>>
}
