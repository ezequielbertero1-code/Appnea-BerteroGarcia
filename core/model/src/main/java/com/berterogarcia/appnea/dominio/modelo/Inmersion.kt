package com.berterogarcia.appnea.dominio.modelo

import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration

/**
 * Una inmersión (buceo o apnea) del usuario logueado. Es el modelo que usan las pantallas;
 * cómo se guarda en la base (Inmersion, con su dueño) es asunto de la capa de datos.
 */
data class Inmersion(
    val id: Long = 0,
    val fecha: LocalDate,
    val lugar: String,
    val profundidadMetros: Float,
    val paradaSeguridadMin: Float,
    val tiempoFondo: Duration,
    val horaEntrada: LocalTime,
    val horaSalida: LocalTime,
    val caracteristicasAgua: Set<String>,
    /** null = no usó lastre. */
    val lastreKg: Float?,
    /** null = sin protección extra. */
    val proteccionMm: Float?,
    val equipamiento: Set<String>,
    val temperaturaAireC: Float,
    val temperaturaAguaC: Float,
    val visibilidad: Visibilidad? = null,
    val visibilidadMetros: Float? = null,
    val actividad: String,
    val comentarios: String,
    /** Clima, mar, marea y luna del momento (se completan solos al registrar). */
    val condiciones: CondicionesRegistro? = null,
    val tipo: TipoInmersion = TipoInmersion.BUCEO,
    /** Lugar marcado en el mapa (null si no se marcó). */
    val latitud: Double? = null,
    val longitud: Double? = null,
    /** Datos de apnea (null en buceo). */
    val apnea: RegistroApnea? = null
)
