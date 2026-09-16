package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.data.local.SesionApneaDao
import com.berterogarcia.appnea.data.local.SesionApneaEntity
import kotlinx.coroutines.flow.Flow

class SesionApneaRepository(private val dao: SesionApneaDao) {
    val todasLasSesiones: Flow<List<SesionApneaEntity>> = dao.obtenerTodas()

    suspend fun guardar(sesion: SesionApneaEntity) = dao.insertar(sesion)
}
