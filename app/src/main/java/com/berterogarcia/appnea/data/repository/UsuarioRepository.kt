package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.data.local.SeguridadPassword
import com.berterogarcia.appnea.data.local.UsuarioDao
import com.berterogarcia.appnea.data.local.UsuarioEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Contraseñas con PBKDF2 + salt (ver SeguridadPassword).
 * El hash es lento a propósito: se calcula en [cpu] (Dispatchers.Default) para no trabar la pantalla.
 */
class UsuarioRepository(
    private val dao: UsuarioDao,
    private val cpu: CoroutineDispatcher = Dispatchers.Default
) {
    /** Para que un usuario inexistente tarde lo mismo que una contraseña incorrecta. */
    private val hashFicticio: String by lazy { SeguridadPassword.hashear("usuario-inexistente") }

    /** @return false si el usuario ya existe. */
    suspend fun registrar(nombreUsuario: String, password: String): Boolean {
        if (dao.existeUsuario(nombreUsuario)) return false
        val hash = withContext(cpu) { SeguridadPassword.hashear(password) }
        return try {
            dao.insertar(UsuarioEntity(nombreUsuario, hash))
            true
        } catch (e: Exception) {
            // Si otro registro con el mismo nombre ganó la carrera, la PK lo rechaza: "ya existe".
            if (dao.existeUsuario(nombreUsuario)) false else throw e
        }
    }

    /**
     * @return true si usuario y contraseña son correctos.
     * Si tenía el hash viejo, lo pasa a PBKDF2 ahora (si falla, se reintenta el próximo login).
     */
    suspend fun validar(nombreUsuario: String, password: String): Boolean {
        val usuario = dao.obtenerPorUsuario(nombreUsuario)
        if (usuario == null) {
            withContext(cpu) { SeguridadPassword.verificar(password, hashFicticio) }
            return false
        }

        val correcta = withContext(cpu) { SeguridadPassword.verificar(password, usuario.password) }
        if (correcta && SeguridadPassword.necesitaActualizar(usuario.password)) {
            runCatching {
                val nuevo = withContext(cpu) { SeguridadPassword.hashear(password) }
                dao.actualizarPassword(nombreUsuario, nuevo)
            }
        }
        return correcta
    }
}