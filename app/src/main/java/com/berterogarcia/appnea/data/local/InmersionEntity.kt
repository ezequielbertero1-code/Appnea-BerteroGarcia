package com.berterogarcia.appnea.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kotlin.time.Duration
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "inmersiones")
@TypeConverters(Converters::class)
data class InmersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: LocalDate,              // dd/mm/aa
    val lugar: String,
    val profundidadMetros: Float,
    val paradaSeguridadMin: Float,
    val tiempoFondo: Duration,        // mm:ss
    val horaEntrada: LocalTime,        // hh:mm
    val horaSalida: LocalTime,         // hh:mm
    val caracteristicasAgua: Set<String>,
    val lastreKg: Float,
    val proteccionMm: Float,
    val equipamiento: Set<String>,
    val temperaturaAireC: Float,
    val temperaturaAguaC: Float,
    val visibilidadMetros: Float,
    val actividad: String,
    val comentarios: String,
    // Condiciones (clima, mar, marea, luna) guardadas automáticamente al registrar
    val condiciones: CondicionesRegistro? = null
)