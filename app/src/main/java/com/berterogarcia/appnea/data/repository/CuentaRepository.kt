package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.data.local.UsuarioDao
import com.berterogarcia.appnea.data.seguridad.SeguridadPassword
import com.berterogarcia.appnea.dominio.EstadoIntentos
import com.berterogarcia.appnea.dominio.GestorSesion
import com.berterogarcia.appnea.dominio.PreferenciasLogin
import com.berterogarcia.appnea.dominio.modelo.TipoCuenta
import com.berterogarcia.appnea.dominio.repositorios.RepositorioCuenta
import com.berterogarcia.appnea.dominio.repositorios.ResultadoCuenta
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cambiar la contraseña y borrar la cuenta del usuario logueado.
 * El hash (PBKDF2, lento a propósito) se calcula en [cpu] para no trabar la pantalla.
 */
class CuentaRepository(
    private val dao: UsuarioDao,
    private val sesion: GestorSesion,
    private val preferencias: PreferenciasLogin,
    private val cpu: CoroutineDispatcher = Dispatchers.Default
) : RepositorioCuenta {

    override suspend fun tipoActual(): TipoCuenta? =
        sesion.usuarioActual.value?.let { dao.obtenerPorUsuario(it)?.tipoCuenta }

    override suspend fun cambiarPassword(actual: String, nueva: String): ResultadoCuenta {
        val usuario = sesion.usuarioActual.value?.let { dao.obtenerPorUsuario(it) } ?: return ResultadoCuenta.SinSesion
        if (usuario.tipoCuenta != TipoCuenta.LOCAL) return ResultadoCuenta.PasswordIncorrecta
        val correcta = withContext(cpu) { SeguridadPassword.verificar(actual, usuario.password) }
        if (!correcta) return ResultadoCuenta.PasswordIncorrecta
        val hash = withContext(cpu) { SeguridadPassword.hashear(nueva) }
        dao.actualizarPassword(usuario.nombreUsuario, hash)
        return ResultadoCuenta.Ok
    }

    override suspend fun borrarCuenta(password: String?): ResultadoCuenta {
        val usuario = sesion.usuarioActual.value?.let { dao.obtenerPorUsuario(it) } ?: return ResultadoCuenta.SinSesion
        if (usuario.tipoCuenta == TipoCuenta.LOCAL) {
            val correcta = withContext(cpu) { SeguridadPassword.verificar(password.orEmpty(), usuario.password) }
            if (!correcta) return ResultadoCuenta.PasswordIncorrecta
        }
        // ON DELETE CASCADE: se van con ella inmersiones, sesiones, perfil, equipo, contactos...
        dao.eliminar(usuario.nombreUsuario)
        if (preferencias.ultimoUsuario == usuario.nombreUsuario) {
            preferencias.ultimoUsuario = null
            preferencias.guardarIntentos(EstadoIntentos())
        }
        sesion.cerrarSesion()
        return ResultadoCuenta.Ok
    }
}
