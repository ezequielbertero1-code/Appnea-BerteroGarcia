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

    /** Para pasar el hash viejo (SHA-256) a PBKDF2 la próxima vez que el usuario entra. */
    @Query("UPDATE usuarios SET password = :hash WHERE nombreUsuario = :nombreUsuario")
    suspend fun actualizarPassword(nombreUsuario: String, hash: String): Int

    /** Borra la cuenta. Las FK con ON DELETE CASCADE borran también todos sus datos. */
    @Query("DELETE FROM usuarios WHERE nombreUsuario = :nombreUsuario")
    suspend fun eliminar(nombreUsuario: String): Int
}
