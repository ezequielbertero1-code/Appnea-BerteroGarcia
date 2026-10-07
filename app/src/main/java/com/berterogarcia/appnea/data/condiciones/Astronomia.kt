package com.berterogarcia.appnea.data.condiciones

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Cálculos astronómicos propios (sin internet): posición del Sol y la Luna, salida/puesta y
 * fase lunar.
 *
 * Usa el método de baja precisión de Paul Schlyter ("How to compute planetary positions",
 * stjarnhimlen.se), con un error típico de pocos minutos en las horas de salida/puesta.
 * Alcanza de sobra para planificar una salida; no es para navegación.
 */
object Astronomia {

    private const val MINUTOS_PASO = 5L
    private const val MES_SINODICO = 29.530588853

    // Altura del centro del astro en la salida/puesta (incluye refracción y semidiámetro)
    private const val H0_SOL = -0.833
    private const val H0_LUNA = 0.125
    private const val H0_CREPUSCULO_CIVIL = -6.0

    private fun rad(g: Double) = Math.toRadians(g)
    private fun grad(r: Double) = Math.toDegrees(r)
    private fun rev(x: Double) = ((x % 360.0) + 360.0) % 360.0

    /** Días desde 2000-01-00 0h UT (la "d" de Schlyter). */
    private fun dia(instante: Instant): Double =
        instante.epochSecond / 86400.0 + 2440587.5 - 2451543.5

    private data class Sol(val longitud: Double, val m: Double, val w: Double)

    private fun sol(d: Double): Sol {
        val w = 282.9404 + 4.70935e-5 * d
        val e = 0.016709 - 1.151e-9 * d
        val m = rev(356.0470 + 0.9856002585 * d)
        val ex = m + grad(e * sin(rad(m)) * (1 + e * cos(rad(m))))
        val xv = cos(rad(ex)) - e
        val yv = sqrt(1 - e * e) * sin(rad(ex))
        val v = grad(atan2(yv, xv))
        return Sol(rev(v + w), m, w)
    }

    private data class Luna(val longitud: Double, val latitud: Double)

    private fun luna(d: Double): Luna {
        val n = rev(125.1228 - 0.0529538083 * d)
        val i = 5.1454
        val w = rev(318.0634 + 0.1643573223 * d)
        val a = 60.2666
        val e = 0.054900
        val m = rev(115.3654 + 13.0649929509 * d)

        var ex = m + grad(e * sin(rad(m)) * (1 + e * cos(rad(m))))
        repeat(5) { ex -= (ex - grad(e * sin(rad(ex))) - m) / (1 - e * cos(rad(ex))) }

        val xv = a * (cos(rad(ex)) - e)
        val yv = a * sqrt(1 - e * e) * sin(rad(ex))
        val v = grad(atan2(yv, xv))
        val r = sqrt(xv * xv + yv * yv)

        val xh = r * (cos(rad(n)) * cos(rad(v + w)) - sin(rad(n)) * sin(rad(v + w)) * cos(rad(i)))
        val yh = r * (sin(rad(n)) * cos(rad(v + w)) + cos(rad(n)) * sin(rad(v + w)) * cos(rad(i)))
        val zh = r * sin(rad(v + w)) * sin(rad(i))
        var lon = grad(atan2(yh, xh))
        var lat = grad(atan2(zh, sqrt(xh * xh + yh * yh)))

        // Perturbaciones principales
        val s = sol(d)
        val ls = rev(s.m + s.w)
        val lm = rev(m + w + n)
        val dd = rev(lm - ls)
        val f = rev(lm - n)
        fun sn(x: Double) = sin(rad(x))
        lon += -1.274 * sn(m - 2 * dd) + 0.658 * sn(2 * dd) - 0.186 * sn(s.m) -
            0.059 * sn(2 * m - 2 * dd) - 0.057 * sn(m - 2 * dd + s.m) + 0.053 * sn(m + 2 * dd) +
            0.046 * sn(2 * dd - s.m) + 0.041 * sn(m - s.m) - 0.035 * sn(dd) - 0.031 * sn(m + s.m) -
            0.015 * sn(2 * f - 2 * dd) + 0.011 * sn(m - 4 * dd)
        lat += -0.173 * sn(f - 2 * dd) - 0.055 * sn(m - f - 2 * dd) - 0.046 * sn(m + f - 2 * dd) +
            0.033 * sn(f + 2 * dd) + 0.017 * sn(2 * m + f)
        return Luna(rev(lon), lat)
    }

    /** Eclípticas -> ecuatoriales (ascensión recta, declinación), en grados. */
    private fun ecuatorial(lon: Double, lat: Double, d: Double): Pair<Double, Double> {
        val ecl = 23.4393 - 3.563e-7 * d
        val xg = cos(rad(lon)) * cos(rad(lat))
        val yg = sin(rad(lon)) * cos(rad(lat))
        val zg = sin(rad(lat))
        val ye = yg * cos(rad(ecl)) - zg * sin(rad(ecl))
        val ze = yg * sin(rad(ecl)) + zg * cos(rad(ecl))
        return rev(grad(atan2(ye, xg))) to grad(atan2(ze, sqrt(xg * xg + ye * ye)))
    }

    private fun altura(ra: Double, dec: Double, d: Double, instante: Instant, lat: Double, lon: Double): Double {
        val s = sol(d)
        val gmst0 = rev(s.m + s.w + 180.0)
        val ut = (instante.epochSecond % 86400L + 86400L) % 86400L / 3600.0
        val ha = rev(gmst0 + ut * 15.0 + lon - ra)
        return grad(asin(sin(rad(lat)) * sin(rad(dec)) + cos(rad(lat)) * cos(rad(dec)) * cos(rad(ha))))
    }

    fun alturaSol(instante: Instant, lat: Double, lon: Double): Double {
        val d = dia(instante)
        val (ra, dec) = ecuatorial(sol(d).longitud, 0.0, d)
        return altura(ra, dec, d, instante, lat, lon)
    }

    fun alturaLuna(instante: Instant, lat: Double, lon: Double): Double {
        val d = dia(instante)
        val l = luna(d)
        val (ra, dec) = ecuatorial(l.longitud, l.latitud, d)
        return altura(ra, dec, d, instante, lat, lon)
    }

    /** Elongación Luna-Sol (0 = luna nueva, 180 = luna llena), en grados. */
    fun elongacion(instante: Instant): Double {
        val d = dia(instante)
        return rev(luna(d).longitud - sol(d).longitud)
    }

    fun iluminacionPorc(elongacion: Double): Int =
        Math.round((1 - cos(rad(elongacion))) / 2 * 100).toInt()

    fun nombreFase(elongacion: Double): String = when {
        elongacion < 10 || elongacion >= 350 -> "Luna nueva"
        elongacion < 80 -> "Creciente"
        elongacion < 100 -> "Cuarto creciente"
        elongacion < 170 -> "Gibosa creciente"
        elongacion < 190 -> "Luna llena"
        elongacion < 260 -> "Gibosa menguante"
        elongacion < 280 -> "Cuarto menguante"
        else -> "Menguante"
    }

    fun emojiFase(elongacion: Double): String = when {
        elongacion < 10 || elongacion >= 350 -> "🌑"
        elongacion < 80 -> "🌒"
        elongacion < 100 -> "🌓"
        elongacion < 170 -> "🌔"
        elongacion < 190 -> "🌕"
        elongacion < 260 -> "🌖"
        elongacion < 280 -> "🌗"
        else -> "🌘"
    }

    /** Primer día (desde [desde]) en que la elongación cruza [objetivo] grados. */
    private fun proximaFase(desde: LocalDate, zona: ZoneId, objetivo: Double): LocalDate {
        var anterior = rev(elongacion(desde.atTime(12, 0).atZone(zona).toInstant()) - objetivo)
        for (k in 1..31) {
            val fecha = desde.plusDays(k.toLong())
            val actual = rev(elongacion(fecha.atTime(12, 0).atZone(zona).toInstant()) - objetivo)
            if (actual < anterior) { // pasó por 0 (dio la vuelta)
                return if (actual < 360 - anterior) fecha else fecha.minusDays(1)
            }
            anterior = actual
        }
        return desde.plusDays(29)
    }

    /**
     * Calcula tpodo lo astronómico de un día local: Sol y Luna.
     */
    fun calcularDia(fecha: LocalDate, lat: Double, lon: Double, zona: ZoneId): DiaAstro {
        val inicio = fecha.atStartOfDay(zona).toInstant()
        val pasos = (24 * 60 / MINUTOS_PASO).toInt()
        val tiempos = (0..pasos).map { inicio.plusSeconds(it * MINUTOS_PASO * 60) }
        val altSol = tiempos.map { alturaSol(it, lat, lon) }
        val altLuna = tiempos.map { alturaLuna(it, lat, lon) }

        fun aLocal(i: Instant) = LocalDateTime.ofInstant(i, zona)

        // Cruces de una altura h0 (subida o bajada), con interpolación lineal
        fun cruces(alturas: List<Double>, h0: Double, subiendo: Boolean): LocalDateTime? {
            for (k in 1 until alturas.size) {
                val a = alturas[k - 1] - h0
                val b = alturas[k] - h0
                val cruza = if (subiendo) a < 0 && b >= 0 else a >= 0 && b < 0
                if (cruza) {
                    val fr = a / (a - b)
                    return aLocal(tiempos[k - 1].plusSeconds((fr * MINUTOS_PASO * 60).toLong()))
                }
            }
            return null
        }

        // Máximos / mínimos locales (tránsito y antitránsito), ignorando los bordes del día
        fun extremos(alturas: List<Double>, maximo: Boolean): LocalDateTime? {
            for (k in 1 until alturas.size - 1) {
                val esExtremo = if (maximo) alturas[k] > alturas[k - 1] && alturas[k] >= alturas[k + 1]
                else alturas[k] < alturas[k - 1] && alturas[k] <= alturas[k + 1]
                if (esExtremo) return aLocal(tiempos[k])
            }
            return null
        }

        val amanecer = cruces(altSol, H0_SOL, true)
        val atardecer = cruces(altSol, H0_SOL, false)
        val salidaLuna = cruces(altLuna, H0_LUNA, true)
        val puestaLuna = cruces(altLuna, H0_LUNA, false)
        val transito = extremos(altLuna, true)
        val antitransito = extremos(altLuna, false)

        val mediodia = tiempos.indices.maxByOrNull { altSol[it] }?.let { aLocal(tiempos[it]) }
        val elong = elongacion(fecha.atTime(12, 0).atZone(zona).toInstant())

        return DiaAstro(
            fecha = fecha,
            amanecer = amanecer,
            atardecer = atardecer,
            inicioCrepusculo = cruces(altSol, H0_CREPUSCULO_CIVIL, true),
            finCrepusculo = cruces(altSol, H0_CREPUSCULO_CIVIL, false),
            mediodiaSolar = mediodia,
            alturaMaximaSol = altSol.maxOrNull() ?: 0.0,
            salidaLuna = salidaLuna,
            puestaLuna = puestaLuna,
            transitoLuna = transito,
            antitransitoLuna = antitransito,
            elongacion = elong,
            iluminacionPorc = iluminacionPorc(elong),
            edadLunaDias = elong / 360.0 * MES_SINODICO,
            fase = nombreFase(elong),
            emojiFase = emojiFase(elong),
            proximaLunaNueva = proximaFase(fecha, zona, 0.0),
            proximaLunaLlena = proximaFase(fecha, zona, 180.0)
        )
    }
}
