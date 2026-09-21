package com.berterogarcia.appnea.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.time.LocalDate

@Entity(tableName = "salidas_pesca")
@TypeConverters(Converters::class)
data class SalidaPescaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: LocalDate,
    val lugar: String,
    val latitud: Double?,
    val longitud: Double?,
    val capturas: List<CapturaPesca>,
    val fotoPath: String?,
    val comentario: String
)
