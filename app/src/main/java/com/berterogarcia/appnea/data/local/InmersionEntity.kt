package com.berterogarcia.appnea.data.local

import com.berterogarcia.appnea.dominio.modelo.CondicionesRegistro
import com.berterogarcia.appnea.dominio.modelo.RegistroApnea
import com.berterogarcia.appnea.dominio.modelo.TipoInmersion
import com.berterogarcia.appnea.dominio.modelo.Visibilidad
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration

// [CAMBIO v7] Cada inmersión pertenece a un usuario (FK a usuarios, se borra en cascada).
@Entity(
    tableName = "inmersiones",
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
    // [CAMBIO v10] Lastre y protección extra son opcionales: null = no usó
    val lastreKg: Float?,
    val proteccionMm: Float?,
    val equipamiento: Set<String>,
    val temperaturaAireC: Float,
    val temperaturaAguaC: Float,
    // [CAMBIO v10] Visibilidad: escala de 5 niveles + metros opcionales
    val visibilidad: Visibilidad? = null,
    val visibilidadMetros: Float? = null,
    val actividad: String,
    val comentarios: String,
    // Condiciones (clima, mar, marea, luna) guardadas automáticamente al registrar
    val condiciones: CondicionesRegistro? = null,
    // [NUEVO v7] Dueño. Lo completa InmersionRepository con el usuario logueado: no hace falta
    // pasarlo desde los ViewModels (por eso tiene valor por defecto y va al final).
    val nombreUsuario: String = "",
    // [NUEVO v9]
    val tipo: TipoInmersion = TipoInmersion.BUCEO,
    // Lugar marcado en el mapa (null en las inmersiones viejas o si no se marcó)
    val latitud: Double? = null,
    val longitud: Double? = null,
    // [NUEVO v10] Datos de apnea (null en buceo)
    val apnea: RegistroApnea? = null
)