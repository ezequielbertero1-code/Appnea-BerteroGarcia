package com.berterogarcia.appnea.entrenamiento

import com.berterogarcia.appnea.dominio.modelo.Fase
import com.berterogarcia.appnea.dominio.modelo.TablaApnea
import com.berterogarcia.appnea.dominio.modelo.TipoFase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class MotorEntrenamientoTest {

    private companion object {
        val INICIO: LocalDateTime = LocalDateTime.of(2026, 11, 16, 9, 30, 15)
    }

    private val tabla = TablaApnea(
        "Prueba",
        listOf(Fase(TipoFase.PREPARACION, 10), Fase(TipoFase.APNEA, 30), Fase(TipoFase.DESCANSO, 20))
    )

    private fun TestScope.motor(actividad: MutableList<Boolean> = mutableListOf()) =
        MotorEntrenamiento(
            scope = backgroundScope,
            sonido = null,
            reloj = { testScheduler.currentTime },
            ahora = { INICIO },
            alCambiarActividad = { actividad += it }
        )

    @Test
    fun tablaCompletaDaElResultadoYApagaElServicio() = runTest {
        val actividad = mutableListOf<Boolean>()
        val m = motor(actividad)
        m.iniciar(tabla)
        advanceTimeBy(120_000); runCurrent()
        val r = m.estado.value.resultado!!
        assertTrue(r.completada)
        assertEquals(3, r.faseAlcanzada)
        assertEquals(30, r.marcaSegundos)
        assertEquals(60, r.duracionSegundos)
        assertEquals(INICIO, r.inicio)
        assertFalse(m.estado.value.corriendo)
        assertEquals(listOf(true, false), actividad)
    }

    @Test
    fun abortarEnLaApneaMideHastaElToque() = runTest {
        val m = motor()
        m.iniciar(tabla)
        advanceTimeBy(10_000 + 12_500) // preparación completa + 12,5 s de apnea
        runCurrent()
        assertEquals(1, m.estado.value.indiceFase)
        m.abortar()
        advanceTimeBy(120_000); runCurrent()
        val r = m.estado.value.resultado!!
        assertFalse(r.completada)
        assertEquals(2, r.faseAlcanzada)
        assertEquals(12, r.marcaSegundos)
        assertEquals(22, r.duracionSegundos)
        assertEquals(INICIO, r.inicio)
    }

    @Test
    fun laCuentaRegresivaNoSeAtrasa() = runTest {
        val m = motor()
        m.iniciar(tabla)
        advanceTimeBy(4_000)
        runCurrent()
        assertEquals(6, m.estado.value.segundosRestantes)
    }

    @Test
    fun reiniciarLimpiaTodo() = runTest {
        val actividad = mutableListOf<Boolean>()
        val m = motor(actividad)
        m.iniciar(tabla)
        advanceTimeBy(5_000)
        m.reiniciar()
        advanceTimeBy(120_000); runCurrent()
        assertNull(m.estado.value.tabla)
        assertNull(m.estado.value.resultado)
        assertEquals(listOf(true, false), actividad)
    }

    @Test
    fun noArrancaDosVecesNiUnaTablaVacia() = runTest {
        val actividad = mutableListOf<Boolean>()
        val m = motor(actividad)
        m.iniciar(TablaApnea("Vacía", emptyList()))
        assertFalse(m.estado.value.corriendo)
        m.iniciar(tabla)
        m.iniciar(tabla)
        assertEquals(listOf(true), actividad)
    }
}
