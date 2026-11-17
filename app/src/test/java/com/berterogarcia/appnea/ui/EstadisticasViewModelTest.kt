package com.berterogarcia.appnea.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.berterogarcia.appnea.ReglaDispatcherMain
import com.berterogarcia.appnea.dominio.modelo.FactorEntrenamiento
import com.berterogarcia.appnea.dominio.modelo.IntentoLibre
import com.berterogarcia.appnea.dominio.modelo.SesionApnea
import com.berterogarcia.appnea.dominio.modelo.TipoInmersion
import com.berterogarcia.appnea.fakes.FakeInmersiones
import com.berterogarcia.appnea.fakes.FakeIntentos
import com.berterogarcia.appnea.fakes.FakeSesionesApnea
import com.berterogarcia.appnea.inmersion
import com.berterogarcia.appnea.ui.estadisticas.EstadisticasUiState
import com.berterogarcia.appnea.ui.estadisticas.EstadisticasViewModel
import com.berterogarcia.appnea.ui.estadisticas.PeriodoEstadisticas
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class EstadisticasViewModelTest {

    @get:Rule val main = ReglaDispatcherMain()

    private val hoy = LocalDate.of(2026, 11, 16)
    private val reloj = Clock.fixed(Instant.parse("2026-11-16T15:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))

    private val inmersiones = FakeInmersiones(
        listOf(
            inmersion(id = 1, fecha = hoy.minusDays(3), profundidad = 20f),
            inmersion(id = 2, fecha = hoy.minusDays(10), profundidad = 30f, tipo = TipoInmersion.APNEA),
            inmersion(id = 3, fecha = hoy.minusMonths(6), profundidad = 40f)
        )
    )
    private val sesiones = FakeSesionesApnea()
    private val intentos = FakeIntentos()

    private fun crear() = EstadisticasViewModel(inmersiones, sesiones, intentos, reloj, main.dispatcher)

    private fun sesion(completada: Boolean, factores: Set<FactorEntrenamiento>, diasAtras: Long = 1) = SesionApnea(
        fecha = hoy.minusDays(diasAtras), nombreTabla = "CO2", completada = completada, faseAlcanzada = 4,
        totalFases = 8, marcaSegundos = 60, factores = factores, sensaciones = "", duracionSegundos = 300
    )

    /** Saltea el estado inicial "cargando" del stateIn. */
    private suspend fun ReceiveTurbine<EstadisticasUiState>.calculado(): EstadisticasUiState {
        var s = awaitItem()
        while (s.cargando) s = awaitItem()
        return s
    }

    @Test
    fun elPeriodoFiltraLasInmersiones() = runTest {
        val vm = crear()
        vm.uiState.test {
            val tresMeses = calculado()
            assertEquals(PeriodoEstadisticas.TRES_MESES, tresMeses.periodo)
            assertEquals(2, tresMeses.inmersiones.cantidad)
            assertEquals(3, tresMeses.totalInmersiones)

            vm.onPeriodoChange(PeriodoEstadisticas.TODO)
            assertEquals(40f, awaitItem().inmersiones.record!!.profundidadMetros)

            vm.onPeriodoChange(PeriodoEstadisticas.UN_MES)
            assertEquals(2, awaitItem().inmersiones.cantidad)
        }
    }

    @Test
    fun elTipoFiltraSoloLasInmersiones() = runTest {
        sesiones.filas.value = listOf(sesion(true, emptySet()))
        val vm = crear()
        vm.uiState.test {
            calculado()
            vm.onTipoChange(TipoInmersion.APNEA)
            val s = awaitItem()
            assertEquals(1, s.inmersiones.cantidad)
            assertEquals(1, s.entrenamiento.sesiones)
        }
    }

    @Test
    fun analizaComoInfluyeCadaFactor() = runTest {
        val acompanado = setOf(FactorEntrenamiento.ACOMPANADO)
        val cansado = setOf(FactorEntrenamiento.CANSANCIO)
        sesiones.filas.value = List(3) { sesion(true, acompanado) } + List(3) { sesion(false, cansado) } +
            sesion(false, setOf(FactorEntrenamiento.HIDRATACION)) // con 1 sola sesión no se informa
        val vm = crear()
        vm.uiState.test {
            val e = calculado().entrenamiento
            assertEquals(43, e.porcentajeCompletas)
            assertEquals(
                listOf(FactorEntrenamiento.ACOMPANADO to 100, FactorEntrenamiento.CANSANCIO to 0),
                e.factores.map { it.factor to it.porcentajeCompletas }
            )
            assertEquals(FactorEntrenamiento.ACOMPANADO, e.factorDestacado!!.factor)
        }
    }

    @Test
    fun sumaElTiempoDeTablasYCronometro() = runTest {
        sesiones.filas.value = listOf(sesion(true, emptySet()))
        intentos.filas.value = listOf(IntentoLibre(fecha = hoy, hora = LocalTime.NOON, duracionMs = 95_000))
        val vm = crear()
        vm.uiState.test {
            val e = calculado().entrenamiento
            assertEquals(300 + 95, e.tiempoEntrenadoSegundos)
            assertEquals(95, e.mejorIntentoSegundos)
            assertEquals(1, e.sesiones)
            assertEquals(1, e.intentosCronometro)
        }
    }

    @Test
    fun sinDatosQuedaVacio() = runTest {
        inmersiones.filas.value = emptyList()
        crear().uiState.test {
            val s = calculado()
            assertTrue(s.entrenamiento.vacio)
            assertEquals(0, s.totalInmersiones)
        }
    }
}
