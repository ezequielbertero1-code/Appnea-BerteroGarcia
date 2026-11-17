package com.berterogarcia.appnea.data.local

import com.berterogarcia.appnea.dominio.modelo.Fase
import com.berterogarcia.appnea.dominio.modelo.TablaApnea
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

// [CAMBIO v7] Cada tabla personalizada pertenece a un usuario (las predefinidas no están en la base).
@Entity(
    tableName = "tablas_personalizadas",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["nombreUsuario"],
            childColumns = ["nombreUsuario"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["nombreUsuario"])]
)
@TypeConverters(Converters::class)
data class TablaPersonalizadaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val fases: List<Fase>,
    // [NUEVO v7] Dueño, lo completa TablaPersonalizadaRepository.
    val nombreUsuario: String = ""
) {
    fun aTablaApnea(): TablaApnea = TablaApnea(nombre, fases)
}
