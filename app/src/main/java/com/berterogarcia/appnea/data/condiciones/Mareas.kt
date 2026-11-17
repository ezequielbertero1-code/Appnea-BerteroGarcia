package com.berterogarcia.appnea.data.condiciones

import com.berterogarcia.appnea.dominio.modelo.EstadoMarea
import com.berterogarcia.appnea.dominio.modelo.ExtremoMarea
import com.berterogarcia.appnea.dominio.modelo.HoraMar
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Mareas a partir de la serie horaria "sea_level_height_msl" de Open-Meteo Marine
 * (nivel del mar incluyendo marea astronómica). Las pleamares/bajamares se buscan como
 * máximos/mínimos locales y se ajusta la hora exacta con una parábola por 3 puntos.
 *
 * Ojo: es un modelo global (~8 km). Cerca de la costa es orientativo y no reemplaza
 * la tabla de mareas oficial del Servicio de Hidrografía Naval.
 */
object Mareas {

    fun extremos(horas: List<HoraMar>): List<ExtremoMarea> {
        val serie = horas.filter { it.nivelMarM != null }
        if (serie.size < 3) return emptyList()
        val resultado = mutableListOf<ExtremoMarea>()
        for (i in 1 until serie.size - 1) {
            val a = serie[i - 1].nivelMarM!!
            val b = serie[i].nivelMarM!!
            val c = serie[i + 1].nivelMarM!!
            val esMax = b > a && b >= c
            val esMin = b < a && b <= c
            if (!esMax && !esMin) continue
            val denominador = a - 2 * b + c
            val delta = if (denominador != 0.0) (0.5 * (a - c) / denominador).coerceIn(-0.5, 0.5) else 0.0
            val altura = b - 0.25 * (a - c) * delta
            val hora = serie[i].hora.plusSeconds((delta * 3600).toLong())
            resultado.add(ExtremoMarea(hora.truncatedTo(ChronoUnit.MINUTES), altura, esMax))
        }
        return resultado
    }

    /** Altura interpolada linealmente para un momento cualquiera. */
    fun alturaEn(horas: List<HoraMar>, momento: LocalDateTime): Double? {
        val serie = horas.filter { it.nivelMarM != null }
        for (i in 0 until serie.size - 1) {
            val h1 = serie[i]
            val h2 = serie[i + 1]
            if (!momento.isBefore(h1.hora) && momento.isBefore(h2.hora)) {
                val total = ChronoUnit.SECONDS.between(h1.hora, h2.hora).toDouble()
                val fr = ChronoUnit.SECONDS.between(h1.hora, momento) / total
                val n1 = h1.nivelMarM ?: return null
                val n2 = h2.nivelMarM ?: return null
                return n1 + (n2 - n1) * fr
            }
        }
        return null
    }

    fun estado(horas: List<HoraMar>, extremos: List<ExtremoMarea>, momento: LocalDateTime): EstadoMarea? {
        val altura = alturaEn(horas, momento) ?: return null
        val anterior = extremos.lastOrNull { !it.hora.isAfter(momento) }
        val siguiente = extremos.firstOrNull { it.hora.isAfter(momento) }
        val subiendo = when {
            siguiente != null -> siguiente.esPleamar
            anterior != null -> !anterior.esPleamar
            else -> false
        }
        return EstadoMarea(altura, subiendo, anterior, siguiente)
    }
}
