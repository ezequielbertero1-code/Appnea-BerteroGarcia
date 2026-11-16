package com.berterogarcia.appnea.data.seguridad

import com.berterogarcia.appnea.data.seguridad.PoliticaIntentos.ESPERA_MAX_MS


/** Intentos fallidos de login guardados en el dispositivo. */
data class EstadoIntentos(
    /** Fallos seguidos desde el último bloqueo o login correcto. */
    val fallidos: Int = 0,
    /** Cuántos bloqueos hubo seguidos (cada uno dura el doble que el anterior). */
    val bloqueos: Int = 0,
    /** Hasta cuándo está bloqueado (epoch millis). 0 = no bloqueado. */
    val bloqueadoHastaMs: Long = 0
)

/**
 * Regla anti fuerza bruta del login. Kotlin puro (sin Android) para poder testearla.
 *
 *  - Cada [MAX_FALLIDOS] fallos seguidos → bloqueo temporal.
 *  - El bloqueo dura 30 s, después 1 min, 2 min, 4 min... hasta [ESPERA_MAX_MS].
 *  - Un login correcto lo resetea toodo. Si pasan 24 h sin fallar, la escalada vuelve a empezar.
 */
object PoliticaIntentos {
    const val MAX_FALLIDOS = 5
    private const val ESPERA_BASE_MS = 30_000L
    private const val ESPERA_MAX_MS = 15 * 60_000L
    private const val OLVIDO_MS = 24 * 60 * 60_000L

    fun registrarFallo(estado: EstadoIntentos, ahora: Long): EstadoIntentos {
        val olvidado = estado.bloqueadoHastaMs != 0L && ahora - estado.bloqueadoHastaMs > OLVIDO_MS
        val bloqueosPrevios = if (olvidado) 0 else estado.bloqueos
        val fallidos = estado.fallidos + 1
        if (fallidos < MAX_FALLIDOS) return estado.copy(fallidos = fallidos, bloqueos = bloqueosPrevios)

        val bloqueos = bloqueosPrevios + 1
        val espera = (ESPERA_BASE_MS shl (bloqueos - 1).coerceAtMost(10)).coerceAtMost(ESPERA_MAX_MS)
        return EstadoIntentos(fallidos = 0, bloqueos = bloqueos, bloqueadoHastaMs = ahora + espera)
    }

    /**
     * Milisegundos que faltan para poder intentar de nuevo (0 = puede intentar).
     * Se limita a [ESPERA_MAX_MS] por si alguien atrasa el reloj del celular.
     */
    fun restanteMs(estado: EstadoIntentos, ahora: Long): Long =
        (estado.bloqueadoHastaMs - ahora).coerceIn(0L, ESPERA_MAX_MS)

    /** Intentos que quedan antes del próximo bloqueo. */
    fun intentosRestantes(estado: EstadoIntentos): Int = MAX_FALLIDOS - estado.fallidos
}