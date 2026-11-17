package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.dominio.repositorios.RepositorioSesion
import com.berterogarcia.appnea.dominio.GestorSesion
import com.berterogarcia.appnea.util.intentar
import com.berterogarcia.appnea.data.local.UsuarioDao

/**
 * Decide si al abrir la app hay una sesión VÁLIDA.
 *
 * El nombre de usuario ya queda guardado en SharedPreferences al loguearse
 * (SessionManager), pero no alcanza con que exista ese valor: la base puede haberse
 * regenerado (fallbackToDestructiveMigration al subir la versión) o el usuario puede
 * no existir más. Por eso se valida contra la tabla `usuarios`.
 */
class SesionRepository(
    private val sessionManager: GestorSesion,
    private val usuarioDao: UsuarioDao
) : RepositorioSesion {
    /**
     * @return el usuario logueado si la sesión es válida, o null si hay que ir al login.
     * Si el usuario guardado ya no existe en la base, limpia la sesión.
     * Si falla la lectura de la base, devuelve null pero NO borra la sesión
     * (un error puntual no debería desloguear a nadie).
     */
    override suspend fun usuarioConSesionValida(): String? {
        val guardado = sessionManager.obtenerUsuario()?.takeIf { it.isNotBlank() } ?: return null
        val existe = intentar("SesionRepository") { usuarioDao.existeUsuario(guardado) }.getOrElse { return null }
        if (!existe) sessionManager.cerrarSesion()
        return guardado.takeIf { existe }
    }
}
