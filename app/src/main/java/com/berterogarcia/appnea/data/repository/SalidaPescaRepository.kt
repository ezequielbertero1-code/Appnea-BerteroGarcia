package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.data.local.SalidaPescaDao
import com.berterogarcia.appnea.data.local.SalidaPescaEntity
import kotlinx.coroutines.flow.Flow

class SalidaPescaRepository(private val dao: SalidaPescaDao) {
    val todasLasSalidas: Flow<List<SalidaPescaEntity>> = dao.obtenerTodas()

    suspend fun guardar(salida: SalidaPescaEntity) {
        dao.insertar(salida)
    }

    suspend fun eliminar(salida: SalidaPescaEntity) {
        dao.eliminar(salida)
    }
}
