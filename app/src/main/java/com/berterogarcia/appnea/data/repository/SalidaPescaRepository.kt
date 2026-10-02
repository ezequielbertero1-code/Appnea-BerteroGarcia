package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.data.local.ProveedorUsuario
import com.berterogarcia.appnea.data.local.SalidaPescaDao
import com.berterogarcia.appnea.data.local.SalidaPescaEntity
import com.berterogarcia.appnea.data.local.porUsuario
import kotlinx.coroutines.flow.Flow

/** [CAMBIO v7] Igual que InmersionRepository: todo sobre el usuario logueado. */
class SalidaPescaRepository(
    private val dao: SalidaPescaDao,
    private val sesion: ProveedorUsuario
) {
    val todasLasSalidas: Flow<List<SalidaPescaEntity>> =
        sesion.porUsuario(emptyList()) { usuario -> dao.obtenerTodas(usuario) }

    /** @return false si no hay sesión. */
    suspend fun guardar(salida: SalidaPescaEntity): Boolean {
        val usuario = sesion.usuarioActual.value ?: return false
        dao.insertar(salida.copy(nombreUsuario = usuario))
        return true
    }

    /** @return true si se borró. */
    suspend fun eliminar(salida: SalidaPescaEntity): Boolean {
        val usuario = sesion.usuarioActual.value ?: return false
        return dao.eliminar(salida.id, usuario) > 0
    }
}
