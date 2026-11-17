package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.data.local.IntentoApneaDao
import com.berterogarcia.appnea.data.local.IntentoApneaEntity
import com.berterogarcia.appnea.data.sesion.ProveedorUsuario
import com.berterogarcia.appnea.data.sesion.porUsuario
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Intentos del cronómetro libre del usuario logueado (igual que SesionApneaRepository). */
class IntentoApneaRepository(
    private val dao: IntentoApneaDao,
    private val sesion: ProveedorUsuario
) {
    val todosLosIntentos: Flow<List<IntentoApneaEntity>> =
        sesion.porUsuario(emptyList()) { usuario -> dao.obtenerTodos(usuario) }

    fun delDia(fecha: LocalDate): Flow<List<IntentoApneaEntity>> =
        sesion.porUsuario(emptyList()) { usuario -> dao.obtenerDelDia(usuario, fecha) }

    /** @return false si no hay sesión. */
    suspend fun guardar(intento: IntentoApneaEntity): Boolean {
        val usuario = sesion.usuarioActual.value ?: return false
        dao.insertar(intento.copy(nombreUsuario = usuario))
        return true
    }
}
