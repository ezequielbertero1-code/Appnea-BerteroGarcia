package com.berterogarcia.appnea.ui

import com.berterogarcia.appnea.inmersion
import com.berterogarcia.appnea.ui.nuevainmersion.NuevaInmersionUiState
import com.berterogarcia.appnea.ui.nuevainmersion.aInmersion
import com.berterogarcia.appnea.ui.nuevainmersion.conInmersion
import org.junit.Assert.assertEquals
import org.junit.Test

class InmersionFormMapperTest {

    @Test
    fun `editar y guardar sin cambios deja la inmersion igual`() {
        val original = inmersion(id = 0).copy(
            lastreKg = 4f,
            proteccionMm = 7f,
            caracteristicasAgua = setOf("Calma"),
            equipamiento = setOf("Aletas · Mares"),
            visibilidadMetros = 10f,
            latitud = -45.86,
            longitud = -67.48,
            comentarios = "Todo bien"
        )
        val vuelta = NuevaInmersionUiState().conInmersion(original).aInmersion()
        assertEquals(original, vuelta)
    }
}
