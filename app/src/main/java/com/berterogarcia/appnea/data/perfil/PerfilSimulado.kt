package com.berterogarcia.appnea.data.perfil

import com.berterogarcia.appnea.data.local.InmersionEntity
import com.berterogarcia.appnea.data.perfil.PerfilSimulado.ASCENSO_BUCEO
import com.berterogarcia.appnea.data.perfil.PerfilSimulado.DESCENSO_BUCEO
import com.berterogarcia.appnea.data.perfil.PerfilSimulado.MAX_PUNTOS
import com.berterogarcia.appnea.data.perfil.PerfilSimulado.PROF_PARADA_M
import com.berterogarcia.appnea.data.perfil.PerfilSimulado.VELOCIDAD_APNEA
import com.berterogarcia.appnea.data.perfil.PerfilSimulado.generar
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt

data class PuntoPerfil(val segundo: Int, val profundidadM: Double)

/**
 * Genera un perfil de inmersión SIMULADO (profundidad vs. tiempo) a partir del
 * resumen que el usuario carga a mano en el formulario.
 *
 * Se usa en dos lugares:
 *  - La pestaña "Gráfico" del detalle de inmersión ([ GraficoPerfil ]).
 *  - El bloque <samples> del archivo UDDF ([ UddfExporter ]), para que los viewers
 *    (Subsurface, MacDive, etc.) puedan dibujar el gráfico.
 *
 * ── ¿Qué significa "simulado"? ──────────────────────────────────────────────────
 *
 *  La app NO tiene sensor de profundidad ni se conecta a una computadora de buceo.
 *  Un perfil real se mide con un sensor de presión cada 1-4 s y refleja lo que pasó
 *  de verdad: frenadas para compensar, subidas y bajadas en el fondo, velocidad
 *  real de ascenso, etc.
 *
 *  Este perfil, en cambio, es una curva "de libro" construida a partir de solo tres
 *  datos cargados a mano (profundidad máxima, tiempo de fondo y parada de seguridad).
 *  La forma de la curva es una ESTIMACIÓN y no debe usarse para sacar conclusiones
 *  sobre la inmersión (por ejemplo, si se respetó la velocidad de ascenso).
 *
 *  Por eso siempre se aclara que es simulado: en la UI con un texto debajo del
 *  gráfico, y en el UDDF con una línea en <notes>.
 *
 * ── Qué es real y qué es supuesto ───────────────────────────────────────────────
 *
 *  Dato del perfil                        Origen
 *  ─────────────────────────────────────  ──────────────────────────────────────
 *  Profundidad máxima (el pico)           REAL: profundidadMetros
 *  Duración del tramo de fondo (buceo)    REAL: tiempoFondo
 *  Duración total de la apnea (apnea)     REAL: tiempoFondo
 *  Duración de la parada de seguridad     REAL: paradaSeguridadMin
 *  Velocidad de descenso y ascenso        SUPUESTO (ver constantes)
 *  Profundidad de la parada (5 m)         SUPUESTO
 *  Fondo plano a la profundidad máxima    SUPUESTO
 *  Forma de las transiciones (curva)      SUPUESTO (interpolación coseno)
 *  Duración total del perfil              DERIVADO de los supuestos: puede no
 *                                         coincidir con horaSalida - horaEntrada
 *
 * ── Supuestos del modelo ────────────────────────────────────────────────────────
 *
 *  1. Tipo de inmersión:
 *     Se considera APNEA si [InmersionEntity.actividad] contiene "apnea" (sin
 *     importar mayúsculas) o si la parada de seguridad es 0. Si no, es BUCEO.
 *
 *  2. Buceo (5 tramos):
 *       descenso ─▶ fondo ─▶ ascenso a 5 m ─▶ parada ─▶ ascenso a superficie
 *     - Descenso a 18 m/min ([DESCENSO_BUCEO]).
 *     - Fondo: se queda en la profundidad máxima durante todoo el tiempoFondo.
 *     - Ascenso a 9 m/min ([ASCENSO_BUCEO]), valor habitual como velocidad máxima
 *       recomendada en cursos recreativos.
 *     - Parada de seguridad a 5 m ([PROF_PARADA_M]) durante paradaSeguridadMin.
 *       Si la profundidad máxima es ≤ 5 m, o la parada es 0, se omite y sube directo.
 *
 *  3. Apnea (3 tramos):
 *       bajada ─▶ fondo ─▶ subida
 *     - tiempoFondo se interpreta como la duración TOTAL de la apnea
 *       (bajar + quedarse + subir), no solo el tiempo quieto abajo.
 *     - Bajada y subida a 1 m/s ([VELOCIDAD_APNEA]).
 *     - El tiempo que sobra se asigna al fondo.
 *     - Si con 1 m/s no alcanza el tiempo total, se reducen proporcionalmente las
 *       velocidades y el fondo queda en 0 (perfil en "V").
 *     - Si tiempoFondo es 0, se usa solo bajada + subida a 1 m/s.
 *
 *  4. Fondo plano:
 *     Durante el tramo de fondo la profundidad es constante e igual a la máxima.
 *     En la realidad el buzo se mueve por el fondo y la profundidad varía; acá se
 *     simplifica porque no hay datos para saber cómo.
 *
 *  5. Transiciones suaves:
 *     Cada tramo se interpola con (1 - cos(πx)) / 2, que arranca y frena despacio,
 *     para evitar quiebres en ángulo recto. Es solo estético: no modela física real.
 *
 * ── Muestreo ────────────────────────────────────────────────────────────────────
 *
 *  - Un punto por segundo, salvo que el perfil dure mucho: se limita a ~[MAX_PUNTOS]
 *    puntos agrandando el paso (por ejemplo, un buceo de 45 min usa un punto cada 4 s).
 *  - Siempre se agregan los instantes exactos de inicio y fin de cada tramo, para
 *    que el pico y la parada queden bien marcados.
 *  - Se fuerza que el punto más profundo sea exactamente profundidadMetros (así
 *    coincide con <greatestdepth> del UDDF) y que el último punto sea 0 m.
 *  - Profundidades redondeadas a 2 decimales, en metros; tiempos en segundos (SI,
 *    como pide UDDF).
 *
 * ── Robustez ────────────────────────────────────────────────────────────────────
 *
 *  - [generar] nunca lanza excepciones: ante cualquier error devuelve lista vacía
 *    (runCatching), y la UI/exportador simplemente no muestran perfil.
 *  - Devuelve lista vacía si la profundidad no es un número válido o es ≤ 0, o si
 *    el perfil total dura menos de 1 s.
 *  - Los tramos con duración 0, negativa o no finita se descartan.
 *
 * ── Limitaciones / trabajo futuro ───────────────────────────────────────────────
 *
 *  - No reemplaza a un perfil real: si más adelante se importan archivos de una
 *    computadora de buceo, deberían usarse sus muestras y no este modelo.
 *  - Las velocidades son fijas; podrían pasar a ser campos cargables por el usuario.
 *  - No modela varias apneas dentro de una misma sesión: genera una sola inmersión.
 *
 * Referencias:
 *  - Estructura <samples>/<waypoint> (depth en m, divetime en s): especificación
 *    UDDF 3.2.1 — https://www.streit.cc/extern/uddf_v321/en/index.html
 *  - Velocidades de descenso/ascenso: valores de referencia de cursos de buceo
 *    recreativo y apnea; no provienen del material de la cátedra.
 */
object PerfilSimulado {
    /** SUPUESTO: velocidad de descenso en buceo, 18 m/min expresado en m/s. */
    const val DESCENSO_BUCEO = 18.0 / 60

    /** SUPUESTO: velocidad de ascenso en buceo, 9 m/min expresado en m/s. */
    const val ASCENSO_BUCEO = 9.0 / 60

    /** SUPUESTO: velocidad de bajada y subida en apnea, en m/s. */
    const val VELOCIDAD_APNEA = 1.0

    /** SUPUESTO: profundidad a la que se hace la parada de seguridad, en metros. */
    private const val PROF_PARADA_M = 5.0

    /** Tope de puntos del perfil, para no generar miles de waypoints en buceos largos. */
    private const val MAX_PUNTOS = 600

    private class Tramo(val desde: Double, val hasta: Double, val duracion: Double)

    fun esApnea(i: InmersionEntity): Boolean =
        i.actividad.contains("apnea", ignoreCase = true) || i.paradaSeguridadMin <= 0f

    /** Nunca crashea: si los datos no alcanzan, devuelve lista vacía. */
    fun generar(i: InmersionEntity): List<PuntoPerfil> = runCatching {
        val tramos = if (esApnea(i)) tramosApnea(i) else tramosBuceo(i)
        muestrear(tramos, i.profundidadMetros.toDouble())
    }.getOrElse { emptyList() }

    // Buceo: descenso → fondo → ascenso a 5 m → parada → superficie
    private fun tramosBuceo(i: InmersionEntity): List<Tramo> {
        val prof = i.profundidadMetros.toDouble()
        val paradaSeg = i.paradaSeguridadMin * 60.0
        val conParada = paradaSeg > 0 && prof > PROF_PARADA_M
        val profParada = if (conParada) PROF_PARADA_M else 0.0
        return listOf(
            Tramo(0.0, prof, prof / DESCENSO_BUCEO),
            Tramo(prof, prof, i.tiempoFondo.inWholeSeconds.toDouble()),
            Tramo(prof, profParada, (prof - profParada) / ASCENSO_BUCEO),
            Tramo(profParada, profParada, if (conParada) paradaSeg else 0.0),
            Tramo(profParada, 0.0, profParada / ASCENSO_BUCEO)
        )
    }

    // Apnea: tiempoFondo = duración total de la apnea (bajar + quedarse + subir)
    private fun tramosApnea(i: InmersionEntity): List<Tramo> {
        val prof = i.profundidadMetros.toDouble()
        val total = i.tiempoFondo.inWholeSeconds.toDouble()
        var bajada = prof / VELOCIDAD_APNEA
        var subida = prof / VELOCIDAD_APNEA
        val fondo = when {
            total <= 0.0 -> 0.0                          // sin tiempo cargado: solo bajar y subir
            bajada + subida > total -> {                 // no entra: escalamos las velocidades
                val f = total / (bajada + subida)
                bajada *= f; subida *= f
                0.0
            }
            else -> total - bajada - subida
        }
        return listOf(Tramo(0.0, prof, bajada), Tramo(prof, prof, fondo), Tramo(prof, 0.0, subida))
    }

    private fun muestrear(tramosCrudos: List<Tramo>, profMax: Double): List<PuntoPerfil> {
        if (!profMax.isFinite() || profMax <= 0.0) return emptyList()
        val tramos = tramosCrudos.filter { it.duracion.isFinite() && it.duracion > 0.0 }
        val total = tramos.sumOf { it.duracion }
        if (total < 1.0) return emptyList()

        fun suave(x: Double) = (1 - cos(PI * x)) / 2    // arranca y frena despacio, sin quiebres

        fun profundidadEn(t: Double): Double {
            var inicio = 0.0
            for (tr in tramos) {
                if (t <= inicio + tr.duracion) {
                    val x = ((t - inicio) / tr.duracion).coerceIn(0.0, 1.0)
                    return tr.desde + (tr.hasta - tr.desde) * suave(x)
                }
                inicio += tr.duracion
            }
            return 0.0
        }

        val totalSeg = ceil(total).toInt()
        val paso = (totalSeg / MAX_PUNTOS).coerceAtLeast(1)
        // Además del paso fijo, metemos los límites de cada tramo para que el pico y la parada queden exactos
        val limites = tramos.runningFold(0.0) { acc, tr -> acc + tr.duracion }.map { it.roundToInt() }
        val instantes = ((0..totalSeg step paso) + limites + totalSeg)
            .filter { it in 0..totalSeg }
            .distinct()
            .sorted()

        val puntos = instantes
            .map { s -> PuntoPerfil(s, redondear(profundidadEn(s.toDouble()))) }
            .toMutableList()
        puntos[puntos.lastIndex] = puntos.last().copy(profundidadM = 0.0)   // termina en superficie
        // El pico tiene que coincidir con la profundidad máxima cargada (greatestdepth)
        val iPico = puntos.indices.maxBy { puntos[it].profundidadM }
        puntos[iPico] = puntos[iPico].copy(profundidadM = redondear(profMax))
        return puntos
    }

    private fun redondear(v: Double) = (v * 100).roundToInt() / 100.0
}

