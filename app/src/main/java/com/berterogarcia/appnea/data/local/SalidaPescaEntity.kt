package com.berterogarcia.appnea.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.time.LocalDate

// [CAMBIO v7] Cada salida de pesca pertenece a un usuario.
@Entity(
    tableName = "salidas_pesca",
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
data class SalidaPescaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: LocalDate,
    val lugar: String,
    val latitud: Double?,
    val longitud: Double?,
    val capturas: List<CapturaPesca>,
    val fotoPath: String?,
    val comentario: String,
    // Condiciones (clima, mar, marea, luna, actividad de pesca) guardadas al registrar
    val condiciones: CondicionesRegistro? = null,
    // [NUEVO v7] Dueño, lo completa SalidaPescaRepository.
    val nombreUsuario: String = ""
)
