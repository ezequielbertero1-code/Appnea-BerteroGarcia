package com.berterogarcia.appnea.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.time.LocalDate

// [CAMBIO v7] Cada sesión de entrenamiento pertenece a un usuario.
@Entity(
    tableName = "sesiones_apnea",
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
data class SesionApneaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: LocalDate,
    val nombreTabla: String,
    val completada: Boolean,     // true = terminó toda la tabla, false = la abortó antes
    val faseAlcanzada: Int,      // número de fase (1-based) donde terminó o abortó
    val totalFases: Int,
    val marcaSegundos: Int,      // mejor tiempo de apnea sostenido durante la sesión
    val factores: Set<String>,   // checklist: descanso, alimentación, estrés, etc.
    val sensaciones: String,     // descripción libre de cómo se sintió
    // [NUEVO v7] Dueño, lo completa SesionApneaRepository.
    val nombreUsuario: String = ""
)
