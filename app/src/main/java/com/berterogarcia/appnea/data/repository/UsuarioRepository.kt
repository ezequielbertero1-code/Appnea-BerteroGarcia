package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.dominio.repositorios.RepositorioUsuarios
import com.berterogarcia.appnea.util.intentar
import com.berterogarcia.appnea.data.seguridad.SeguridadPassword
import com.berterogarcia.appnea.data.local.UsuarioDao
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
) : RepositorioUsuarios {
    /** Para que un usuario inexistente tarde lo mismo que una contraseña incorrecta. */
    private val hashFicticio: String by lazy { SeguridadPassword.hashear("usuario-inexistente") }

    /**
     * @return true si usuario y contraseña son correctos.
     * Si tenía el hash viejo, lo pasa a PBKDF2 ahora (si falla, se reintenta el próximo login).
     */
    override suspend fun validar(nombreUsuario: String, password: String): Boolean {
        val usuario = dao.obtenerPorUsuario(nombreUsuario)
        if (usuario == null) {
            withContext(cpu) { SeguridadPassword.verificar(password, hashFicticio) }
            return false
        }

        val correcta = withContext(cpu) { SeguridadPassword.verificar(password, usuario.password) }
        if (correcta && SeguridadPassword.necesitaActualizar(usuario.password)) {
            intentar("UsuarioRepository") {
                val nuevo = withContext(cpu) { SeguridadPassword.hashear(password) }
                dao.actualizarPassword(nombreUsuario, nuevo)
            }
        }
        return correcta
    }
}