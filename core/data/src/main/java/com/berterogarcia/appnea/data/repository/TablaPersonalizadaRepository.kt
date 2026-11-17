package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.dominio.modelo.TablaPersonalizada
import com.berterogarcia.appnea.dominio.repositorios.RepositorioTablas
import com.berterogarcia.appnea.data.local.aModelo
import com.berterogarcia.appnea.data.local.aEntidad
import kotlinx.coroutines.flow.map
import com.berterogarcia.appnea.dominio.ProveedorUsuario
import com.berterogarcia.appnea.data.local.TablaPersonalizadaDao
import com.berterogarcia.appnea.data.sesion.porUsuario
import kotlinx.coroutines.flow.Flow

/** [CAMBIO v7] Igual que InmersionRepository: todo sobre el usuario logueado. */
class TablaPersonalizadaRepository(
    private val dao: TablaPersonalizadaDao,
    private val sesion: ProveedorUsuario
) : RepositorioTablas {
    override val todasLasTablas: Flow<List<TablaPersonalizada>> =
        sesion.porUsuario(emptyList()) { usuario -> dao.obtenerTodas(usuario).map { l -> l.map { it.aModelo() } } }

    /** @return false si no hay sesión. */
    override suspend fun guardar(tabla: TablaPersonalizada): Boolean {
        val usuario = sesion.usuarioActual.value ?: return false
        dao.insertar(tabla.aEntidad(usuario))
        return true
    }

    /** @return true si se borró. */
    override suspend fun eliminar(tabla: TablaPersonalizada): Boolean {
        val usuario = sesion.usuarioActual.value ?: return false
        return dao.eliminar(tabla.id, usuario) > 0
    }
}
