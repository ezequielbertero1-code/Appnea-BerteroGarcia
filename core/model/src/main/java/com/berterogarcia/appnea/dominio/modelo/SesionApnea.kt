package com.berterogarcia.appnea.dominio.modelo

import java.time.LocalDate
import java.time.LocalTime

/** Una tabla de entrenamiento hecha (completa o cortada). */
data class SesionApnea(
    val id: Long = 0,
    val fecha: LocalDate,
    val nombreTabla: String,
    /** true = terminó toda la tabla, false = la cortó antes. */
    val completada: Boolean,
    /** Número de fase (desde 1) donde terminó o cortó. */
    val faseAlcanzada: Int,
    val totalFases: Int,
    /** Apnea más larga sostenida durante la sesión. */
    val marcaSegundos: Int,
    val factores: Set<FactorEntrenamiento>,
    val sensaciones: String,
    /** null en las sesiones guardadas antes de la v12. */
    val horaInicio: LocalTime? = null,
    /** null en las sesiones guardadas antes de la v12. */
    val duracionSegundos: Int? = null
)
