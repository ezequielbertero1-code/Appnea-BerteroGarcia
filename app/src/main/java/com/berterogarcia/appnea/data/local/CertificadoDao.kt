package com.berterogarcia.appnea.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CertificadoDao {
    @Query("SELECT * FROM certificados WHERE nombreUsuario = :nombreUsuario ORDER BY id DESC")
    fun observarTodos(nombreUsuario: String): Flow<List<CertificadoEntity>>

    @Query("SELECT * FROM certificados WHERE id = :id AND nombreUsuario = :nombreUsuario LIMIT 1")
    suspend fun obtener(id: Long, nombreUsuario: String): CertificadoEntity?

    @Query("SELECT COUNT(*) FROM certificados WHERE nombreUsuario = :nombreUsuario")
    suspend fun contar(nombreUsuario: String): Int

    @Insert
    suspend fun insertar(certificado: CertificadoEntity): Long

    @Update
    suspend fun actualizar(certificado: CertificadoEntity): Int

    @Query("DELETE FROM certificados WHERE id = :id AND nombreUsuario = :nombreUsuario")
    suspend fun eliminar(id: Long, nombreUsuario: String): Int
}
