package com.berterogarcia.appnea.dominio.modelo

import java.time.LocalDate
import java.time.LocalTime

/** Un intento del cronómetro libre (apnea estática, de Iniciar a Parar). */
data class IntentoLibre(
    val id: Long = 0,
    val fecha: LocalDate,
    /** Hora de inicio del intento. */
    val hora: LocalTime,
    val duracionMs: Long
)
