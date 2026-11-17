package com.berterogarcia.appnea.ui

import com.berterogarcia.appnea.dominio.modelo.TipoInmersion
import com.berterogarcia.appnea.dominio.modelo.Visibilidad
import com.berterogarcia.appnea.ui.nuevainmersion.BajadaForm
import com.berterogarcia.appnea.ui.nuevainmersion.Etapa
import com.berterogarcia.appnea.ui.nuevainmersion.NuevaInmersionUiState
import com.berterogarcia.appnea.ui.nuevainmersion.aBorradorJson
import com.berterogarcia.appnea.ui.nuevainmersion.conBorrador
import org.junit.Assert.assertEquals
import org.junit.Test

class BorradorInmersionTest {

    @Test
    fun `el borrador recupera todo lo que escribio el usuario`() {
        val escrito = NuevaInmersionUiState(
            etapa = Etapa.CONDICIONES, tipo = TipoInmersion.APNEA, fecha = "10/11/26", lugar = "Rada Tilly",
            latitud = -45.9, longitud = -67.5, profundidadMetros = "12", horaEntrada = "10:00", horaSalida = "11:00",
            caracteristicasAgua = setOf("Calma", "Clara"), usoLastre = true, lastreKg = "3",
            equipamiento = setOf("Aletas · Mares"), temperaturaAireC = "-2", temperaturaAguaC = "11",
            visibilidad = Visibilidad.entries.first(), visibilidadMetros = "8", cantidadBajadas = "2",
            cargarDetalleBajadas = true, bajadas = listOf(BajadaForm("10", "0045"), BajadaForm("12", "0100")),
            sincope = true, actividad = "Peso constante", comentarios = "Bien, con \"comillas\""
        )
        val recuperado = NuevaInmersionUiState().conBorrador(escrito.aBorradorJson())
        assertEquals(escrito, recuperado)
    }

    @Test
    fun `un borrador roto no cambia nada`() {
        val estado = NuevaInmersionUiState(lugar = "x")
        assertEquals(estado, estado.conBorrador("{roto"))
    }
}
