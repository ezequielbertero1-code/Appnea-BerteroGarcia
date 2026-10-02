package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.data.local.ProveedorUsuario
import com.berterogarcia.appnea.data.local.SesionApneaDao
import com.berterogarcia.appnea.data.local.SesionApneaEntity
import com.berterogarcia.appnea.data.local.porUsuario
import kotlinx.coroutines.flow.Flow

/** [CAMBIO v7] Igual que InmersionRepository: todo sobre el usuario logueado. */
class SesionApneaRepository(
    private val dao: SesionApneaDao,
    private val sesion: ProveedorUsuario
) {
    val todasLasSesiones: Flow<List<SesionApneaEntity>> =
        sesion.porUsuario(emptyList()) { usuario -> dao.obtenerTodas(usuario) }

    /** @return false si no hay sesión. */
    suspend fun guardar(sesionApnea: SesionApneaEntity): Boolean {
        val usuario = sesion.usuarioActual.value ?: return false
        dao.insertar(sesionApnea.copy(nombreUsuario = usuario))
        return true
    }
}
