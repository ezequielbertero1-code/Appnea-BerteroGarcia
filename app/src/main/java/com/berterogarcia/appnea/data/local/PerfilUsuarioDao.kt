package com.berterogarcia.appnea.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PerfilUsuarioDao {
    /** Emite null mientras el usuario no haya cargado sus datos. */
    @Query("SELECT * FROM perfiles_usuario WHERE nombreUsuario = :nombreUsuario LIMIT 1")
    fun observar(nombreUsuario: String): Flow<PerfilUsuarioEntity?>

    @Query("SELECT * FROM perfiles_usuario WHERE nombreUsuario = :nombreUsuario LIMIT 1")
    suspend fun obtener(nombreUsuario: String): PerfilUsuarioEntity?

    /**
     * Inserta o actualiza. Se usa @Upsert y NO @Insert(onConflict = REPLACE) porque
     * REPLACE hace DELETE + INSERT, y si el día de mañana alguna tabla referencia al
     * perfil con CASCADE, se borrarían esos hijos sin querer.
     */
    @Upsert
    suspend fun guardar(perfil: PerfilUsuarioEntity)
}
