package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.data.sesion.ProveedorUsuario
import com.berterogarcia.appnea.data.local.TablaPersonalizadaDao
import com.berterogarcia.appnea.data.local.TablaPersonalizadaEntity
import com.berterogarcia.appnea.data.sesion.porUsuario
import kotlinx.coroutines.flow.Flow

/** [CAMBIO v7] Igual que InmersionRepository: todo sobre el usuario logueado. */
class TablaPersonalizadaRepository(
    private val dao: TablaPersonalizadaDao,
    private val sesion: ProveedorUsuario
) {
    val todasLasTablas: Flow<List<TablaPersonalizadaEntity>> =
        sesion.porUsuario(emptyList()) { usuario -> dao.obtenerTodas(usuario) }

    /** @return false si no hay sesión. */
    suspend fun guardar(tabla: TablaPersonalizadaEntity): Boolean {
        val usuario = sesion.usuarioActual.value ?: return false
        dao.insertar(tabla.copy(nombreUsuario = usuario))
        return true
    }

    /** @return true si se borró. */
    suspend fun eliminar(tabla: TablaPersonalizadaEntity): Boolean {
        val usuario = sesion.usuarioActual.value ?: return false
        return dao.eliminar(tabla.id, usuario) > 0
    }
}
