package com.berterogarcia.appnea.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InmersionDao {
    @Insert
    suspend fun insertar(inmersion: InmersionEntity)

    @Query("SELECT * FROM inmersiones ORDER BY id DESC")
    fun obtenerTodas(): Flow<List<InmersionEntity>>
}