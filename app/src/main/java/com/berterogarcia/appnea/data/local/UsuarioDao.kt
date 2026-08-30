package com.berterogarcia.appnea.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UsuarioDao {
    @Insert
    suspend fun insertar(user: UsuarioEntity)

    @Query("SELECT * FROM usuarios WHERE nombreUsuario = :nombreUsuario Limit 1")
    suspend fun obtenerPorUsuario(nombreUsuario: String): UsuarioEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM usuarios WHERE nombreUsuario = :nombreUsuario)")
    suspend fun existeUsuario(nombreUsuario: String): Boolean
}