package com.berterogarcia.appnea.ui

import com.berterogarcia.appnea.dominio.modelo.TipoInmersion
import com.berterogarcia.appnea.ui.nuevainmersion.Campo
import com.berterogarcia.appnea.ui.nuevainmersion.Etapa
import com.berterogarcia.appnea.ui.nuevainmersion.EventoFormulario
import com.berterogarcia.appnea.ui.nuevainmersion.NuevaInmersionUiState
import com.berterogarcia.appnea.ui.nuevainmersion.filtrarTemperatura
import com.berterogarcia.appnea.ui.nuevainmersion.reducir
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReducirFormularioTest {

    private fun NuevaInmersionUiState.con(vararg eventos: EventoFormulario) =
        eventos.fold(this) { s, e -> reducir(s, e) }

    @Test
    fun `siguiente no avanza si la etapa tiene errores`() {
        val s = NuevaInmersionUiState(fecha = "", lugar = "").con(EventoFormulario.Siguiente)
        assertEquals(Etapa.GENERAL, s.etapa)
        assertTrue(Campo.LUGAR in s.errores)
    }

    @Test
    fun `ir a solo vuelve hacia atras`() {
        val s = NuevaInmersionUiState(etapa = Etapa.CONDICIONES)
        assertEquals(Etapa.CONDICIONES, s.con(EventoFormulario.IrA(Etapa.NOTAS)).etapa)
        assertEquals(Etapa.GENERAL, s.con(EventoFormulario.IrA(Etapa.GENERAL)).etapa)
    }

    @Test
    fun `activar el detalle de bajadas genera una fila por bajada`() {
        val s = NuevaInmersionUiState(tipo = TipoInmersion.APNEA)
            .con(EventoFormulario.CantidadBajadas("3"), EventoFormulario.DetalleBajadas(true))
        assertEquals(3, s.bajadas.size)
        val sinUna = s.con(EventoFormulario.QuitarBajada(0))
        assertEquals("2", sinUna.cantidadBajadas)
    }

    @Test
    fun `desactivar el lastre borra el valor`() {
        val s = NuevaInmersionUiState()
            .con(EventoFormulario.UsoLastre(true), EventoFormulario.Lastre("6kg"))
        assertEquals("6", s.lastreKg)
        assertEquals("", s.con(EventoFormulario.UsoLastre(false)).lastreKg)
    }

    @Test
    fun `la temperatura acepta coma y un solo signo`() {
        assertEquals("-12.5", filtrarTemperatura("-12,5"))
        assertEquals("125", filtrarTemperatura("1-2-5"))
        assertEquals("1.25", filtrarTemperatura("1.2.5"))
    }

    @Test
    fun `elegir un lugar en el mapa sin nombre conserva el escrito`() {
        val s = NuevaInmersionUiState(lugar = "Rada Tilly")
            .con(EventoFormulario.UbicacionElegida(-45.9, -67.5, "  "))
        assertEquals("Rada Tilly", s.lugar)
        assertEquals(-45.9, s.latitud!!, 0.0)
    }
}
