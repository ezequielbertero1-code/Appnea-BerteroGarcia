package com.berterogarcia.appnea.data

import com.berterogarcia.appnea.data.condiciones.HoraMar
import com.berterogarcia.appnea.data.condiciones.Mareas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import kotlin.math.PI
import kotlin.math.sin

class MareasTest {

    private val inicio = LocalDateTime.of(2026, 11, 16, 0, 0)

    private fun hora(h: Long, nivel: Double?) = HoraMar(
        hora = inicio.plusHours(h), olaM = null, direccionOlaGrados = null, periodoOlaS = null,
        olaVientoM = null, marDeFondoM = null, direccionMarDeFondoGrados = null, periodoMarDeFondoS = null,
        nivelMarM = nivel, temperaturaAguaC = null, corrienteKmh = null, direccionCorrienteGrados = null
    )

    /** Marea semidiurna sintética: período de 12 h, amplitud 2 m. */
    private val serie = (0L..48L).map { h -> hora(h, 2.0 * sin(2 * PI * h / 12.0)) }

    @Test
    fun `encuentra pleamares y bajamares alternadas`() {
        val extremos = Mareas.extremos(serie)
        assertEquals(8, extremos.size)
        extremos.zipWithNext().forEach { (a, b) -> assertTrue(a.esPleamar != b.esPleamar) }
        val pleamar = extremos.first { it.esPleamar }
        assertEquals(2.0, pleamar.alturaM, 0.1)
    }

    @Test
    fun `interpola la altura entre dos horas`() {
        val lista = listOf(hora(0, 0.0), hora(1, 1.0))
        assertEquals(0.5, Mareas.alturaEn(lista, inicio.plusMinutes(30))!!, 1e-9)
        assertNull(Mareas.alturaEn(lista, inicio.plusHours(5)))
    }

    @Test
    fun `sube antes de una pleamar y baja despues`() {
        val extremos = Mareas.extremos(serie)
        assertTrue(Mareas.estado(serie, extremos, inicio.plusHours(1))!!.subiendo)
        assertFalse(Mareas.estado(serie, extremos, inicio.plusHours(4))!!.subiendo)
    }

    @Test
    fun `sin datos de nivel no inventa extremos`() {
        assertTrue(Mareas.extremos(serie.map { it.copy(nivelMarM = null) }).isEmpty())
    }
}
