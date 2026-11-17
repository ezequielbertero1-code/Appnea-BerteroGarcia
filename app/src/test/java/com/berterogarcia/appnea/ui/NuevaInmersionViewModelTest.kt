package com.berterogarcia.appnea.ui

import androidx.lifecycle.SavedStateHandle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ReglaDispatcherMain
import com.berterogarcia.appnea.dominio.RegistrarInmersion
import com.berterogarcia.appnea.dominio.modelo.CondicionesRegistro
import com.berterogarcia.appnea.dominio.modelo.UBICACION_COMODORO_RIVADAVIA
import com.berterogarcia.appnea.dominio.modelo.Ubicacion
import com.berterogarcia.appnea.fakes.FakeCondiciones
import com.berterogarcia.appnea.fakes.FakeInmersiones
import com.berterogarcia.appnea.fakes.FakePerfil
import com.berterogarcia.appnea.fakes.FakeProgramador
import com.berterogarcia.appnea.fakes.FakeUbicacion
import com.berterogarcia.appnea.inmersion
import com.berterogarcia.appnea.ui.components.textoUi
import com.berterogarcia.appnea.ui.nuevainmersion.Campo
import com.berterogarcia.appnea.ui.nuevainmersion.Etapa
import com.berterogarcia.appnea.ui.nuevainmersion.EventoFormulario
import com.berterogarcia.appnea.ui.nuevainmersion.NuevaInmersionUiState
import com.berterogarcia.appnea.ui.nuevainmersion.NuevaInmersionViewModel
import com.berterogarcia.appnea.ui.nuevainmersion.aBorradorJson
import com.berterogarcia.appnea.ui.nuevainmersion.conInmersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class NuevaInmersionViewModelTest {

    @get:Rule val main = ReglaDispatcherMain()

    private val repo = FakeInmersiones()
    private val programador = FakeProgramador()
    private val gps = FakeUbicacion()
    private val reloj = Clock.fixed(Instant.parse("2026-11-16T13:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))

    private fun crear(idEditar: Long? = null, estado: SavedStateHandle = SavedStateHandle()) = NuevaInmersionViewModel(
        repo, RegistrarInmersion(repo, FakeCondiciones(), programador), FakePerfil(), gps, idEditar, estado, reloj
    )

    /** Formulario ya completo (como si el usuario lo hubiera llenado), cargado por el borrador. */
    private fun completo() = SavedStateHandle(
        mapOf("borrador_inmersion" to NuevaInmersionUiState().conInmersion(inmersion(id = 0)).aBorradorJson())
    )

    @Test
    fun arrancaConLaFechaDeHoy() {
        assertEquals("16/11/26", crear().uiState.value.fecha)
    }

    @Test
    fun guardarNuevaSinInternetLaGuardaYProgramaElClima() {
        val vm = crear(estado = completo())
        vm.onEvento(EventoFormulario.Guardar)

        assertTrue(vm.uiState.value.guardadoOk)
        val guardada = repo.filas.value.single()
        assertEquals("Punta Marqués", guardada.lugar)
        assertFalse(guardada.condiciones!!.conDatosOnline)
        // Sin lugar marcado ni GPS: se usa Comodoro para el clima
        assertEquals(UBICACION_COMODORO_RIVADAVIA, programador.programados.single().second)
    }

    @Test
    fun conLugarMarcadoSePidenSusCondiciones() {
        val vm = crear(estado = completo())
        vm.onEvento(EventoFormulario.UbicacionElegida(-45.9, -67.5, "Rada Tilly"))
        vm.onEvento(EventoFormulario.Guardar)
        assertEquals("Rada Tilly", repo.filas.value.single().lugar)
        assertEquals(Ubicacion(-45.9, -67.5), programador.programados.single().second)
    }

    @Test
    fun guardarConErroresVuelveALaPrimeraEtapaConProblemas() {
        val vm = crear()
        vm.onEvento(EventoFormulario.Guardar)
        val s = vm.uiState.value
        assertEquals(Etapa.GENERAL, s.etapa)
        assertTrue(Campo.LUGAR in s.errores)
        assertTrue(repo.filas.value.isEmpty())
    }

    @Test
    fun siFallaLaBaseSeMuestraElError() {
        repo.falla = true
        val vm = crear(estado = completo())
        vm.onEvento(EventoFormulario.Guardar)
        assertFalse(vm.uiState.value.guardadoOk)
        assertFalse(vm.uiState.value.guardando)
        assertEquals(textoUi(R.string.nueva_error_guardar), vm.uiState.value.errorGuardado)
    }

    @Test
    fun editarCargaLaInmersionYConservaSusCondiciones() {
        val completas = CondicionesRegistro(temperaturaC = 17.0, faseLunar = "Nueva", iluminacionLunaPorc = 0, conDatosOnline = true)
        repo.filas.value = listOf(inmersion(id = 5).copy(condiciones = completas))
        val vm = crear(idEditar = 5)
        assertEquals("Punta Marqués", vm.uiState.value.lugar)
        assertTrue(vm.uiState.value.editando)

        vm.onEvento(EventoFormulario.Comentarios("Mucha corriente"))
        vm.onEvento(EventoFormulario.Guardar)

        val guardada = repo.filas.value.single()
        assertEquals(5L, guardada.id)
        assertEquals("Mucha corriente", guardada.comentarios)
        assertEquals(completas, guardada.condiciones)
        assertTrue(programador.programados.isEmpty())
    }

    @Test
    fun editarUnaQueNoExisteAvisa() {
        val vm = crear(idEditar = 99)
        assertFalse(vm.uiState.value.cargandoEdicion)
        assertEquals(textoUi(R.string.nueva_error_no_encontrada), vm.uiState.value.errorCarga)
    }

    @Test
    fun miUbicacionCompletaElLugarVacio() {
        gps.actual = Ubicacion(-45.8, -67.4)
        gps.nombre = "Caleta Córdova"
        val vm = crear()
        vm.onEvento(EventoFormulario.UsarMiUbicacion)
        val s = vm.uiState.value
        assertEquals(-45.8, s.latitud!!, 0.0)
        assertEquals("Caleta Córdova", s.lugar)
        assertFalse(s.buscandoUbicacion)
    }

    @Test
    fun sinSenialDeGpsAvisaSegunSiEstaActivado() {
        gps.activada = false
        val vm = crear()
        vm.onEvento(EventoFormulario.UsarMiUbicacion)
        assertNull(vm.uiState.value.latitud)
        assertEquals(textoUi(R.string.nueva_aviso_activar_ubicacion), vm.uiState.value.avisoUbicacion)
    }
}
