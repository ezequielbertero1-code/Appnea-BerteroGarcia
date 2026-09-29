package com.berterogarcia.appnea.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InmersionDao {
    @Insert
    suspend fun insertar(inmersion: InmersionEntity)

    @Delete
    suspend fun eliminar(inmersion: InmersionEntity)

    @Query("SELECT * FROM inmersiones ORDER BY id DESC")
    fun obtenerTodas(): Flow<List<InmersionEntity>>

    @Query("SELECT * FROM inmersiones WHERE id = :id")
    fun obtenerPorId(id: Long): Flow<InmersionEntity?>
}