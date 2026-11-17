package com.berterogarcia.appnea

import com.berterogarcia.appnea.dominio.modelo.Inmersion
import com.berterogarcia.appnea.dominio.modelo.TipoInmersion
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration.Companion.minutes

/** Inmersión válida con valores por defecto, para no repetir los 20 campos en cada test. */
fun inmersion(
    id: Long = 1,
    fecha: LocalDate = LocalDate.of(2026, 11, 10),
    lugar: String = "Punta Marqués",
    profundidad: Float = 18f,
    tipo: TipoInmersion = TipoInmersion.BUCEO,
    horaEntrada: LocalTime = LocalTime.of(10, 0)
) = Inmersion(
    id = id,
    fecha = fecha,
    lugar = lugar,
    profundidadMetros = profundidad,
    paradaSeguridadMin = 3f,
    tiempoFondo = 30.minutes,
    horaEntrada = horaEntrada,
    horaSalida = horaEntrada.plusMinutes(45),
    caracteristicasAgua = emptySet(),
    lastreKg = null,
    proteccionMm = null,
    equipamiento = emptySet(),
    temperaturaAireC = 20f,
    temperaturaAguaC = 14f,
    actividad = "",
    comentarios = "",
    tipo = tipo
)
