package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.dominio.modelo.Inmersion
import com.berterogarcia.appnea.dominio.repositorios.RepositorioInmersiones
import com.berterogarcia.appnea.data.local.aModelo
import com.berterogarcia.appnea.data.local.aEntidad
import kotlinx.coroutines.flow.map
import com.berterogarcia.appnea.data.local.InmersionDao
import com.berterogarcia.appnea.dominio.ProveedorUsuario
import com.berterogarcia.appnea.data.sesion.porUsuario
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
) : RepositorioInmersiones {
    override val todasLasInmersiones: Flow<List<Inmersion>> =
        sesion.porUsuario(emptyList()) { usuario -> dao.obtenerTodas(usuario).map { l -> l.map { it.aModelo() } } }

    override fun obtenerPorId(id: Long): Flow<Inmersion?> =
        sesion.porUsuario(null) { usuario -> dao.obtenerPorId(id, usuario).map { it?.aModelo() } }

    /** @return el id de la inmersión nueva, o null si no hay sesión. */
    override suspend fun guardar(inmersion: Inmersion): Long? {
        val usuario = sesion.usuarioActual.value ?: return null
        return dao.insertar(inmersion.aEntidad(usuario))
    }

    /**
     * Guarda los cambios de una inmersión que ya existe.
     * @return false si no hay sesión o si esa inmersión no es del usuario logueado
     * (se verifica antes: @Update solo mira el id y podría pisar la de otro usuario).
     */
    override suspend fun actualizar(inmersion: Inmersion): Boolean {
        val usuario = sesion.usuarioActual.value ?: return false
        return dao.actualizarSiEsDe(inmersion.aEntidad(usuario), usuario)
    }

    /** @return true si se borró (false si no hay sesión, no existía o era de otro usuario). */
    override suspend fun eliminar(inmersion: Inmersion): Boolean {
        val usuario = sesion.usuarioActual.value ?: return false
        return dao.eliminar(inmersion.id, usuario) > 0
    }
}
