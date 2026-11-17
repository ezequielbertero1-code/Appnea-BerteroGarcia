package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.dominio.modelo.TipoCuenta
import com.berterogarcia.appnea.dominio.modelo.PerfilUsuario
import com.berterogarcia.appnea.dominio.modelo.ContactoEmergencia
import com.berterogarcia.appnea.dominio.repositorios.RepositorioCuentaGoogle
import com.berterogarcia.appnea.data.local.aEntidad
import androidx.room.withTransaction
import com.berterogarcia.appnea.data.local.AppDatabase
import com.berterogarcia.appnea.data.local.UsuarioEntity

/**
 * Cuentas creadas con "Iniciar sesión con Google".
 *
 * El `nombreUsuario` es el email: el registro normal no deja usar "@" en el usuario
 * (ver ReglasRegistro), así que nunca choca con una cuenta con contraseña.
 */
class CuentaGoogleRepository(private val db: AppDatabase) : RepositorioCuentaGoogle {

    /** true si esta cuenta de Google ya completó el registro (si no, hay que mandarla a registrarse). */
    override suspend fun existe(email: String): Boolean = db.usuarioDao().existeUsuario(email)

    /**
     * Crea usuario + perfil + contacto de emergencia en UNA transacción (o se guardan los tres o ninguno).
     * Si la cuenta ya existía (ej. otro registro ganó la carrera) no toca nada: es la misma cuenta.
     */
    override suspend fun registrar(email: String, perfil: PerfilUsuario, contacto: ContactoEmergencia) {
        db.withTransaction {
            if (db.usuarioDao().existeUsuario(email)) return@withTransaction
            db.usuarioDao().insertar(UsuarioEntity(email, password = "", tipoCuenta = TipoCuenta.GOOGLE))
            db.perfilUsuarioDao().guardar(perfil.aEntidad(email))
            db.contactoEmergenciaDao().guardar(contacto.copy(esPrincipal = true).aEntidad(email))
        }
    }
}
