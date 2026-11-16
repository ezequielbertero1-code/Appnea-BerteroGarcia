package com.berterogarcia.appnea.data

import com.berterogarcia.appnea.data.local.EstadoIntentos
import com.berterogarcia.appnea.data.local.PoliticaIntentos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PoliticaIntentosTest {

    private fun fallar(veces: Int, desde: EstadoIntentos = EstadoIntentos(), ahora: Long = 0L) =
        (1..veces).fold(desde) { e, _ -> PoliticaIntentos.registrarFallo(e, ahora) }

    @Test
    fun `menos fallos que el maximo no bloquea`() {
        val e = fallar(PoliticaIntentos.MAX_FALLIDOS - 1)
        assertEquals(0L, PoliticaIntentos.restanteMs(e, 0L))
        assertEquals(1, PoliticaIntentos.intentosRestantes(e))
    }

    @Test
    fun `al llegar al maximo bloquea 30 segundos`() {
        val e = fallar(PoliticaIntentos.MAX_FALLIDOS)
        assertEquals(30_000L, PoliticaIntentos.restanteMs(e, 0L))
        assertEquals(0, e.fallidos)
        assertEquals(1, e.bloqueos)
    }

    @Test
    fun `cada bloqueo seguido dura el doble`() {
        val primero = fallar(PoliticaIntentos.MAX_FALLIDOS)
        val segundo = fallar(PoliticaIntentos.MAX_FALLIDOS, primero, ahora = 30_000L)
        assertEquals(60_000L, PoliticaIntentos.restanteMs(segundo, 30_000L))
    }

    @Test
    fun `la espera nunca supera 15 minutos`() {
        var e = EstadoIntentos()
        repeat(20) { e = fallar(PoliticaIntentos.MAX_FALLIDOS, e) }
        assertTrue(PoliticaIntentos.restanteMs(e, 0L) <= 15 * 60_000L)
    }

    @Test
    fun `atrasar el reloj no alarga el bloqueo mas alla del maximo`() {
        val e = EstadoIntentos(bloqueadoHastaMs = Long.MAX_VALUE / 2)
        assertEquals(15 * 60_000L, PoliticaIntentos.restanteMs(e, 0L))
    }

    @Test
    fun `despues de 24 horas la escalada vuelve a empezar`() {
        val bloqueado = fallar(PoliticaIntentos.MAX_FALLIDOS)
        val unDiaDespues = 30_000L + 25 * 60 * 60_000L
        val nuevo = fallar(PoliticaIntentos.MAX_FALLIDOS, bloqueado, ahora = unDiaDespues)
        assertEquals(30_000L, PoliticaIntentos.restanteMs(nuevo, unDiaDespues))
    }
}
