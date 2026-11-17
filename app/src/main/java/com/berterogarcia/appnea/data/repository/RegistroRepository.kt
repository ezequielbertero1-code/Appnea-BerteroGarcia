package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.dominio.repositorios.RepositorioRegistro
import com.berterogarcia.appnea.dominio.repositorios.NuevoRegistro
import com.berterogarcia.appnea.dominio.repositorios.ResultadoRegistro
import com.berterogarcia.appnea.data.local.aEntidad
import androidx.room.withTransaction
import com.berterogarcia.appnea.data.local.AppDatabase
import com.berterogarcia.appnea.data.seguridad.SeguridadPassword
import com.berterogarcia.appnea.data.local.UsuarioEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Crea la cuenta completa: usuario + perfil + contacto de emergencia, en UNA transacción
 * (o se guardan las tres cosas o ninguna).
 */
class RegistroRepository(
    private val db: AppDatabase,
    private val cpu: CoroutineDispatcher = Dispatchers.Default
) : RepositorioRegistro {
    override suspend fun usuarioDisponible(usuario: String): Boolean = !db.usuarioDao().existeUsuario(usuario)

    /** @return Creado o UsuarioExistente. Otros errores de base se propagan (el ViewModel los muestra). */
    override suspend fun registrar(datos: NuevoRegistro): ResultadoRegistro {
        if (!usuarioDisponible(datos.usuario)) return ResultadoRegistro.UsuarioExistente

        // El hash es lento a propósito: se calcula ANTES de la transacción y fuera del hilo principal.
        val hash = withContext(cpu) { SeguridadPassword.hashear(datos.password) }

        return try {
            db.withTransaction {
                db.usuarioDao().insertar(UsuarioEntity(datos.usuario, hash))
                db.perfilUsuarioDao().guardar(datos.perfil.aEntidad(datos.usuario))
                db.contactoEmergenciaDao().guardar(
                    datos.contacto.copy(esPrincipal = true).aEntidad(datos.usuario)
                )
            }
            ResultadoRegistro.Creado
        } catch (e: Exception) {
            // Si otro registro con el mismo nombre ganó la carrera, la PK rechaza el insert
            // y la transacción se deshace entera.
            if (!usuarioDisponible(datos.usuario)) ResultadoRegistro.UsuarioExistente else throw e
        }
    }
}
