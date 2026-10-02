package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.data.local.InmersionDao
import com.berterogarcia.appnea.data.local.InmersionEntity
import com.berterogarcia.appnea.data.local.ProveedorUsuario
import com.berterogarcia.appnea.data.local.porUsuario
import kotlinx.coroutines.flow.Flow

/**
 * [CAMBIO v7] Trabaja siempre sobre el usuario logueado ([sesion]).
 *  - Las lecturas se actualizan solas al iniciar/cerrar sesión.
 *  - Al guardar, el dueño lo pone el repositorio: un ViewModel no puede guardar a nombre de otro.
 *  - Sin sesión no se guarda ni se borra nada (devuelve false) y las lecturas vienen vacías.
 *
 * La API pública es la misma de antes, así que los ViewModels no cambian.
 */
class InmersionRepository(
    private val dao: InmersionDao,
    private val sesion: ProveedorUsuario
) {
    val todasLasInmersiones: Flow<List<InmersionEntity>> =
        sesion.porUsuario(emptyList()) { usuario -> dao.obtenerTodas(usuario) }

    fun obtenerPorId(id: Long): Flow<InmersionEntity?> =
        sesion.porUsuario(null) { usuario -> dao.obtenerPorId(id, usuario) }

    /** @return false si no hay sesión. */
    suspend fun guardar(inmersion: InmersionEntity): Boolean {
        val usuario = sesion.usuarioActual.value ?: return false
        dao.insertar(inmersion.copy(nombreUsuario = usuario))
        return true
    }

    /** @return true si se borró (false si no hay sesión, no existía o era de otro usuario). */
    suspend fun eliminar(inmersion: InmersionEntity): Boolean {
        val usuario = sesion.usuarioActual.value ?: return false
        return dao.eliminar(inmersion.id, usuario) > 0
    }
}
