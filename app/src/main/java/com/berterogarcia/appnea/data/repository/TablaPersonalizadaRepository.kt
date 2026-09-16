package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.data.local.TablaPersonalizadaDao
import com.berterogarcia.appnea.data.local.TablaPersonalizadaEntity
import kotlinx.coroutines.flow.Flow

class TablaPersonalizadaRepository(private val dao: TablaPersonalizadaDao) {
    val todasLasTablas: Flow<List<TablaPersonalizadaEntity>> = dao.obtenerTodas()

    suspend fun guardar(tabla: TablaPersonalizadaEntity) = dao.insertar(tabla)
    suspend fun eliminar(tabla: TablaPersonalizadaEntity) = dao.eliminar(tabla)
}
