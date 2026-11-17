package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.dominio.modelo.SesionApnea
import com.berterogarcia.appnea.dominio.repositorios.RepositorioSesionesApnea
import com.berterogarcia.appnea.data.local.aModelo
import com.berterogarcia.appnea.data.local.aEntidad
import kotlinx.coroutines.flow.map
import com.berterogarcia.appnea.dominio.ProveedorUsuario
import com.berterogarcia.appnea.data.local.SesionApneaDao
import com.berterogarcia.appnea.data.sesion.porUsuario
import kotlinx.coroutines.flow.Flow

/** [CAMBIO v7] Igual que InmersionRepository: todo sobre el usuario logueado. */
class SesionApneaRepository(
    private val dao: SesionApneaDao,
    private val sesion: ProveedorUsuario
) : RepositorioSesionesApnea {
    override val todasLasSesiones: Flow<List<SesionApnea>> =
        sesion.porUsuario(emptyList()) { usuario -> dao.obtenerTodas(usuario).map { l -> l.map { it.aModelo() } } }

    /** @return false si no hay sesión. */
    override suspend fun guardar(sesion: SesionApnea): Boolean {
        val usuario = this.sesion.usuarioActual.value ?: return false
        dao.insertar(sesion.aEntidad(usuario))
        return true
    }
}
