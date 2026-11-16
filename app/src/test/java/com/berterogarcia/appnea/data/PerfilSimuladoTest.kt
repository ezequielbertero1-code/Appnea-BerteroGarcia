package com.berterogarcia.appnea.data

import com.berterogarcia.appnea.data.local.BajadaApnea
import com.berterogarcia.appnea.data.local.RegistroApnea
import com.berterogarcia.appnea.data.local.TipoInmersion
import com.berterogarcia.appnea.data.perfil.PerfilSimulado
import com.berterogarcia.appnea.inmersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PerfilSimuladoTest {

    @Test
    fun `buceo empieza y termina en superficie y llega a la profundidad maxima`() {
        val perfil = PerfilSimulado.generar(inmersion(profundidad = 18f))
        assertEquals(0.0, perfil.first().profundidadM, 0.01)
        assertEquals(0.0, perfil.last().profundidadM, 0.01)
        assertEquals(18.0, perfil.maxOf { it.profundidadM }, 0.01)
        perfil.zipWithNext().forEach { (a, b) -> assertTrue(b.segundo >= a.segundo) }
    }

    @Test
    fun `apnea con varias bajadas llega a la mas profunda`() {
        val apnea = RegistroApnea(2, 12f, bajadas = listOf(BajadaApnea(8f, 40), BajadaApnea(12f, 60)))
        val perfil = PerfilSimulado.generar(inmersion(profundidad = 12f, tipo = TipoInmersion.APNEA).copy(apnea = apnea))
        assertEquals(12.0, perfil.maxOf { it.profundidadM }, 0.01)
        // vuelve a superficie entre bajadas
        assertTrue(perfil.count { it.profundidadM == 0.0 } >= 3)
    }

    @Test
    fun `datos imposibles devuelven lista vacia en vez de crashear`() {
        val perfil = PerfilSimulado.generar(inmersion(profundidad = Float.NaN))
        assertTrue(perfil.isEmpty() || perfil.all { !it.profundidadM.isNaN() })
    }
}
