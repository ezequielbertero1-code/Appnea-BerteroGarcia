package com.berterogarcia.appnea.ui

import com.berterogarcia.appnea.dominio.modelo.TipoInmersion
import com.berterogarcia.appnea.inmersion
import com.berterogarcia.appnea.ui.historial.PeriodoFiltro
import com.berterogarcia.appnea.ui.historial.armarEstado
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class HistorialArmarEstadoTest {

    private val hoy = LocalDate.of(2026, 11, 16)
    private val lista = listOf(
        inmersion(id = 1, fecha = LocalDate.of(2026, 11, 14), lugar = "Punta Marqués"),
        inmersion(id = 2, fecha = LocalDate.of(2026, 11, 2), lugar = "Rada Tilly", tipo = TipoInmersion.APNEA),
        inmersion(id = 3, fecha = LocalDate.of(2026, 6, 1), lugar = "Caleta Córdova")
    )

    @Test
    fun `agrupa por mes del mas nuevo al mas viejo`() {
        val e = armarEstado(lista, PeriodoFiltro.TODAS, "", hoy)
        assertEquals(listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 6)), e.grupos.map { it.mes })
        assertEquals(listOf(1L, 2L), e.grupos.first().inmersiones.map { it.id })
        assertEquals(3, e.totalGeneral)
    }

    @Test
    fun `filtra por periodo`() {
        assertEquals(1, armarEstado(lista, PeriodoFiltro.SIETE_DIAS, "", hoy).cantidadFiltrada)
        assertEquals(2, armarEstado(lista, PeriodoFiltro.UN_MES, "", hoy).cantidadFiltrada)
    }

    @Test
    fun `busqueda ignora tildes y mayusculas`() {
        val e = armarEstado(lista, PeriodoFiltro.TODAS, "MARQUES", hoy)
        assertEquals(listOf(1L), e.grupos.flatMap { it.inmersiones }.map { it.id })
        assertEquals(1, armarEstado(lista, PeriodoFiltro.TODAS, "córdova", hoy).cantidadFiltrada)
    }

    @Test
    fun `filtra por tipo y mantiene el total general`() {
        val e = armarEstado(lista, PeriodoFiltro.TODAS, "", hoy, TipoInmersion.APNEA)
        assertEquals(1, e.cantidadFiltrada)
        assertEquals(3, e.totalGeneral)
    }
}
