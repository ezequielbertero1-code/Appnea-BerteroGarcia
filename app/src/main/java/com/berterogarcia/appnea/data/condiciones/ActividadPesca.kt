package com.berterogarcia.appnea.data.condiciones

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Índice ORIENTATIVO de actividad de peces (0-100), al estilo de las tablas solunares.
 *
 * Combina factores que los pescadores suelen tener en cuenta:
 *  - Fase lunar: luna nueva y llena suman más (mareas de sicigia).
 *  - Períodos solunares: luna en lo alto/bajo (mayores) y salida/puesta de luna (menores).
 *  - Amanecer y atardecer.
 *  - Cambio de marea (cerca de pleamar o bajamar).
 *  - Presión: bajando rápido resta, estable o subiendo suma un poco.
 *  - Viento y olas fuertes restan (condiciones de pesca submarina).
 *
 * No es un dato científico: sirve para comparar horas y días entre sí.
 */
object ActividadPesca {

    fun calcular(
        momento: LocalDateTime,
        astro: DiaAstro,
        mareas: List<ExtremoMarea>,
        clima: HoraClima?,
        climaHace3h: HoraClima?,
        mar: HoraMar?
    ): ActividadHora {
        var puntaje = 15.0
        val motivos = mutableListOf<String>()

        // Fase lunar (hasta +25)
        val fase = Astronomia.factorFase(astro.elongacion)
        puntaje += 25 * fase
        if (fase > 0.8) motivos += "${astro.fase}: buena fase"

        // Períodos solunares (hasta +30)
        val periodo = astro.periodos.firstOrNull { !momento.isBefore(it.inicio) && !momento.isAfter(it.fin) }
        if (periodo != null) {
            puntaje += if (periodo.mayor) 30 else 18
            motivos += "Período ${if (periodo.mayor) "mayor" else "menor"}: ${periodo.motivo.lowercase()}"
        }

        // Amanecer / atardecer (±1 h, +12)
        val cercaDeLuz = listOfNotNull(astro.amanecer, astro.atardecer).any {
            abs(ChronoUnit.MINUTES.between(it, momento)) <= 60
        }
        if (cercaDeLuz) {
            puntaje += 12
            motivos += "Cerca del amanecer/atardecer"
        }

        // Cambio de marea (±1 h de un extremo, +10)
        val cambio = mareas.firstOrNull { abs(ChronoUnit.MINUTES.between(it.hora, momento)) <= 60 }
        if (cambio != null) {
            puntaje += 10
            motivos += "Cambio de marea (${if (cambio.esPleamar) "pleamar" else "bajamar"})"
        }

        // Presión (tendencia en 3 h)
        val p = clima?.presionHpa
        val p3 = climaHace3h?.presionHpa
        if (p != null && p3 != null) {
            val tendencia = p - p3
            if (tendencia <= -2.0) {
                puntaje -= 12
                motivos += "Presión bajando rápido"
            } else if (tendencia >= -0.5) {
                puntaje += 5
                motivos += "Presión estable o subiendo"
            }
        }

        // Viento y olas
        val viento = clima?.vientoKmh
        if (viento != null && viento >= 35) {
            puntaje -= 15
            motivos += "Viento fuerte"
        }
        val ola = mar?.olaM
        if (ola != null && ola >= 1.5) {
            puntaje -= 10
            motivos += "Oleaje alto"
        }

        val total = puntaje.roundToInt().coerceIn(0, 100)
        return ActividadHora(momento, total, NivelActividad.desde(total), motivos)
    }

    /** Semáforo para entrar al agua con los datos de una hora. */
    fun aptitudAgua(clima: HoraClima?, mar: HoraMar?): AptitudAgua {
        var nivel = Aptitud.BUENA
        val motivos = mutableListOf<String>()
        fun empeorar(a: Aptitud, motivo: String) {
            if (a.ordinal > nivel.ordinal) nivel = a
            motivos += motivo
        }

        val ola = mar?.olaM
        if (ola != null) {
            if (ola >= 1.5) empeorar(Aptitud.MALA, "Olas de ${"%.1f".format(ola)} m")
            else if (ola >= 0.8) empeorar(Aptitud.REGULAR, "Olas de ${"%.1f".format(ola)} m")
        }
        val viento = clima?.vientoKmh
        if (viento != null) {
            if (viento >= 30) empeorar(Aptitud.MALA, "Viento de ${viento.roundToInt()} km/h")
            else if (viento >= 20) empeorar(Aptitud.REGULAR, "Viento de ${viento.roundToInt()} km/h")
        }
        val rafagas = clima?.rafagasKmh
        if (rafagas != null) {
            if (rafagas >= 50) empeorar(Aptitud.MALA, "Ráfagas de ${rafagas.roundToInt()} km/h")
            else if (rafagas >= 35) empeorar(Aptitud.REGULAR, "Ráfagas de ${rafagas.roundToInt()} km/h")
        }
        val codigo = clima?.codigoClima
        if (codigo != null) {
            if (codigo >= 95) empeorar(Aptitud.MALA, "Tormenta eléctrica")
            else if (codigo in 61..67 || codigo in 80..82) empeorar(Aptitud.REGULAR, "Lluvia")
        }
        val visibilidad = clima?.visibilidadM
        if (visibilidad != null && visibilidad < 1000) {
            empeorar(Aptitud.REGULAR, "Poca visibilidad en superficie (niebla)")
        }
        val corriente = mar?.corrienteKmh
        if (corriente != null && corriente >= 2.5) {
            empeorar(Aptitud.REGULAR, "Corriente de ${"%.1f".format(corriente)} km/h")
        }
        if (motivos.isEmpty()) motivos += "Viento y mar tranquilos"
        return AptitudAgua(nivel, motivos)
    }
}
