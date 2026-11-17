package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.dominio.modelo.IntentoLibre
import com.berterogarcia.appnea.dominio.repositorios.RepositorioIntentos
import com.berterogarcia.appnea.data.local.aModelo
import com.berterogarcia.appnea.data.local.aEntidad
import kotlinx.coroutines.flow.map
import com.berterogarcia.appnea.data.local.IntentoApneaDao
import com.berterogarcia.appnea.dominio.ProveedorUsuario
import com.berterogarcia.appnea.data.sesion.porUsuario
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Intentos del cronómetro libre del usuario logueado (igual que SesionApneaRepository). */
class IntentoApneaRepository(
    private val dao: IntentoApneaDao,
    private val sesion: ProveedorUsuario
) : RepositorioIntentos {
    override val todosLosIntentos: Flow<List<IntentoLibre>> =
        sesion.porUsuario(emptyList()) { usuario -> dao.obtenerTodos(usuario).map { l -> l.map { it.aModelo() } } }

    override fun delDia(fecha: LocalDate): Flow<List<IntentoLibre>> =
        sesion.porUsuario(emptyList()) { usuario -> dao.obtenerDelDia(usuario, fecha).map { l -> l.map { it.aModelo() } } }

    /** @return false si no hay sesión. */
    override suspend fun guardar(intento: IntentoLibre): Boolean {
        val usuario = sesion.usuarioActual.value ?: return false
        dao.insertar(intento.aEntidad(usuario))
        return true
    }
}
