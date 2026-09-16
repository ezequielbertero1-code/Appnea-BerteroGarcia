package com.berterogarcia.appnea.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "tablas_personalizadas")
@TypeConverters(Converters::class)
data class TablaPersonalizadaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val fases: List<Fase>
) {
    fun aTablaApnea(): TablaApnea = TablaApnea(nombre, fases)
}
