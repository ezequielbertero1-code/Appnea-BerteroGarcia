package com.berterogarcia.appnea.ui

import androidx.lifecycle.SavedStateHandle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ReglaDispatcherMain
import com.berterogarcia.appnea.dominio.modelo.Fase
import com.berterogarcia.appnea.dominio.modelo.FactorEntrenamiento
import com.berterogarcia.appnea.dominio.modelo.TablaPersonalizada
import com.berterogarcia.appnea.dominio.modelo.TipoFase
import com.berterogarcia.appnea.entrenamiento.MotorEntrenamiento
import com.berterogarcia.appnea.fakes.FakeSesionesApnea
import com.berterogarcia.appnea.fakes.FakeTablas
import com.berterogarcia.appnea.ui.components.textoUi
import com.berterogarcia.appnea.ui.entrenamiento.EntrenamientoApneaViewModel
import com.berterogarcia.appnea.ui.entrenamiento.EntrenamientoUiState
import com.berterogarcia.appnea.ui.entrenamiento.EtapaEntrenamiento
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class EntrenamientoApneaViewModelTest {

    // Standard: el motor y el ViewModel comparten el reloj virtual de runTest
    @get:Rule val main = ReglaDispatcherMain(StandardTestDispatcher())

    private val inicio = LocalDateTime.of(2026, 11, 16, 21, 1, 26, 500_000_000)
    private val tablas = FakeTablas()
    private val sesiones = FakeSesionesApnea()
    private val propia = TablaPersonalizada(id = 1, nombre = "Mía", fases = listOf(Fase(TipoFase.PREPARACION, 10), Fase(TipoFase.APNEA, 20)))

    private fun TestScope.crear(estado: SavedStateHandle = SavedStateHandle()): EntrenamientoApneaViewModel {
        val motor = MotorEntrenamiento(backgroundScope, sonido = null, reloj = { testScheduler.currentTime }, ahora = { inicio })
        return EntrenamientoApneaViewModel(tablas, sesiones, motor, estado).also { runCurrent() }
    }

    /** El motor arma el resultado en su próximo tick, después del toque. */
    private fun TestScope.abortar(vm: EntrenamientoApneaViewModel) {
        vm.onAbortar()
        advanceTimeBy(2_000)
        runCurrent()
    }

    @Test
    fun abortarYGuardarRegistraHoraYDuracion() = runTest {
        tablas.filas.value = listOf(propia)
        val vm = crear()
        vm.onSeleccionarTabla(propia.aTablaApnea())
        vm.onAcompanadoChange(true)
        vm.onIniciar()
        runCurrent()
        assertEquals(EtapaEntrenamiento.EN_CURSO, vm.uiState.value.etapa)

        advanceTimeBy(10_000 + 7_200) // preparación + 7,2 s de apnea
        runCurrent()
        abortar(vm)
        assertEquals(EtapaEntrenamiento.GUARDANDO, vm.uiState.value.etapa)

        vm.onSensacionesChange("  Bien  ")
        vm.onGuardarSesion()
        runCurrent()

        assertTrue(vm.uiState.value.guardadoOk)
        val s = sesiones.filas.value.single()
        assertEquals("Mía", s.nombreTabla)
        assertFalse(s.completada)
        assertEquals(inicio.toLocalDate(), s.fecha)
        assertEquals(LocalTime.of(21, 1, 26), s.horaInicio)
        assertEquals(17, s.duracionSegundos)
        assertEquals(7, s.marcaSegundos)
        assertEquals(setOf(FactorEntrenamiento.ACOMPANADO), s.factores)
        assertEquals("Bien", s.sensaciones)
    }

    @Test
    fun unDobleToqueNoGuardaDosVeces() = runTest {
        val vm = crear()
        vm.onSeleccionarTabla(EntrenamientoUiState().tablasPredefinidas.first())
        vm.onIniciar()
        runCurrent()
        abortar(vm)
        vm.onGuardarSesion()
        vm.onGuardarSesion()
        runCurrent()
        assertEquals(1, sesiones.filas.value.size)
    }

    @Test
    fun siFallaLaBaseAvisa() = runTest {
        sesiones.falla = true
        val vm = crear()
        vm.onSeleccionarTabla(EntrenamientoUiState().tablasPredefinidas.first())
        vm.onIniciar()
        runCurrent()
        abortar(vm)
        vm.onGuardarSesion()
        runCurrent()
        assertEquals(textoUi(R.string.entrenar_error_guardar), vm.uiState.value.errorGuardado)
        assertFalse(vm.uiState.value.guardadoOk)
    }

    @Test
    fun recuperaLaTablaElegidaSiAndroidCerroLaApp() = runTest {
        val predefinida = EntrenamientoUiState().tablasPredefinidas.last()
        assertEquals(predefinida, crear(SavedStateHandle(mapOf("tabla_elegida" to predefinida.nombre))).uiState.value.tablaSeleccionada)

        // Una propia se recupera cuando llega la lista de la base
        tablas.filas.value = listOf(propia)
        assertEquals(propia.aTablaApnea(), crear(SavedStateHandle(mapOf("tabla_elegida" to "Mía"))).uiState.value.tablaSeleccionada)
    }

    @Test
    fun borrarLaTablaElegidaLaDeselecciona() = runTest {
        tablas.filas.value = listOf(propia)
        val estado = SavedStateHandle()
        val vm = crear(estado)
        vm.onSeleccionarTabla(propia.aTablaApnea())
        vm.onEliminarTablaPersonalizada(propia)
        runCurrent()
        assertNull(vm.uiState.value.tablaSeleccionada)
        assertTrue(tablas.filas.value.isEmpty())
        assertNull(estado.get<String>("tabla_elegida"))
    }
}
