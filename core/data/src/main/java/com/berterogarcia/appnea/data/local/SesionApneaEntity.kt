package com.berterogarcia.appnea.data.local

import com.berterogarcia.appnea.dominio.modelo.FactorEntrenamiento
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.time.LocalDate
import java.time.LocalTime

/** Una tabla de entrenamiento hecha (completa o cortada). Pertenece a un usuario. */
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
    val factores: Set<FactorEntrenamiento>,   // checklist: descanso, alimentación, estrés, etc.
    val sensaciones: String,     // descripción libre de cómo se sintió
    // Dueño, lo completa SesionApneaRepository.
    val nombreUsuario: String = "",
    /** Hora en que arrancó la tabla (v12). null en las sesiones guardadas antes. */
    val horaInicio: LocalTime? = null,
    /** Cuánto duró, de Iniciar a terminar o abortar (v12). null en las sesiones guardadas antes. */
    val duracionSegundos: Int? = null
)
