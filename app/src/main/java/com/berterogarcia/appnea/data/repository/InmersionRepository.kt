package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.data.local.InmersionDao
import com.berterogarcia.appnea.data.local.InmersionEntity
import kotlinx.coroutines.flow.Flow

class InmersionRepository(private val dao: InmersionDao) {
    fun obtenerPorId(id: Long): Flow<InmersionEntity?> = dao.obtenerPorId(id)
    val todasLasInmersiones: Flow<List<InmersionEntity>> = dao.obtenerTodas()

    suspend fun guardar(inmersion: InmersionEntity) {
        dao.insertar(inmersion)
    }

    suspend fun eliminar(inmersion: InmersionEntity) = dao.eliminar(inmersion)
}