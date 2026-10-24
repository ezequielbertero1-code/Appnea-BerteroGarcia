package com.berterogarcia.appnea.data.repository

import androidx.room.withTransaction
import com.berterogarcia.appnea.data.local.AppDatabase
import com.berterogarcia.appnea.data.local.ContactoEmergenciaEntity
import com.berterogarcia.appnea.data.local.PerfilUsuarioEntity
import com.berterogarcia.appnea.data.local.UsuarioEntity

/**
 * Cuentas creadas con "Iniciar sesión con Google".
 *
 * El `nombreUsuario` es el email: el registro normal no deja usar "@" en el usuario
 * (ver ReglasRegistro), así que nunca choca con una cuenta con contraseña.
 */
class CuentaGoogleRepository(private val db: AppDatabase) {

    /** true si esta cuenta de Google ya completó el registro (si no, hay que mandarla a registrarse). */
    suspend fun existe(email: String): Boolean = db.usuarioDao().existeUsuario(email)

    /**
     * Crea usuario + perfil + contacto de emergencia en UNA transacción (o se guardan los tres o ninguno).
     * Si la cuenta ya existía (ej. otro registro ganó la carrera) no toca nada: es la misma cuenta.
     */
    suspend fun registrar(email: String, perfil: PerfilUsuarioEntity, contacto: ContactoEmergenciaEntity) {
        db.withTransaction {
            if (db.usuarioDao().existeUsuario(email)) return@withTransaction
            db.usuarioDao().insertar(UsuarioEntity(email, PASSWORD_CUENTA_GOOGLE))
            db.perfilUsuarioDao().guardar(perfil.copy(nombreUsuario = email))
            db.contactoEmergenciaDao().guardar(contacto.copy(nombreUsuario = email, esPrincipal = true))
        }
    }

    private companion object {
        /**
         * No tiene el formato de SeguridadPassword.hashear, así que verificar() siempre da false:
         * a estas cuentas no se puede entrar escribiendo una contraseña.
         */
        const val PASSWORD_CUENTA_GOOGLE = "!google"
    }
}
