package com.berterogarcia.appnea.data.repository

import androidx.room.withTransaction
import com.berterogarcia.appnea.data.auth.CuentaGoogle
import com.berterogarcia.appnea.data.local.AppDatabase
import com.berterogarcia.appnea.data.local.PerfilUsuarioEntity
import com.berterogarcia.appnea.data.local.UsuarioEntity

/**
 * Cuentas creadas con "Iniciar sesión con Google".
 *
 * El `nombreUsuario` es el email: el registro normal no deja usar "@" en el usuario
 * (ver ReglasRegistro), así que nunca choca con una cuenta con contraseña.
 */
class CuentaGoogleRepository(private val db: AppDatabase) {

    /**
     * Devuelve el usuario con el que hay que iniciar sesión. Si es la primera vez, crea usuario y
     * perfil en una transacción; si ya existía, no toca nada (el perfil puede estar editado).
     */
    suspend fun entrar(cuenta: CuentaGoogle): String {
        val usuario = cuenta.email
        db.withTransaction {
            if (!db.usuarioDao().existeUsuario(usuario)) {
                db.usuarioDao().insertar(UsuarioEntity(usuario, PASSWORD_CUENTA_GOOGLE))
                db.perfilUsuarioDao().guardar(
                    PerfilUsuarioEntity(
                        nombreUsuario = usuario,
                        nombre = cuenta.nombre,
                        apellido = cuenta.apellido,
                        email = cuenta.email
                    )
                )
            }
        }
        return usuario
    }

    private companion object {
        /**
         * No tiene el formato de SeguridadPassword.hashear, así que verificar() siempre da false:
         * a estas cuentas no se puede entrar escribiendo una contraseña.
         */
        const val PASSWORD_CUENTA_GOOGLE = "!google"
    }
}
