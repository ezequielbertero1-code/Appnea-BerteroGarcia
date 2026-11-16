package com.berterogarcia.appnea.ui

import com.berterogarcia.appnea.data.local.TipoInmersion
import com.berterogarcia.appnea.ui.nuevainmersion.BajadaForm
import com.berterogarcia.appnea.ui.nuevainmersion.Campo
import com.berterogarcia.appnea.ui.nuevainmersion.NuevaInmersionUiState
import com.berterogarcia.appnea.ui.nuevainmersion.validarInmersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorInmersionTest {

    private val buceoValido = NuevaInmersionUiState(
        tipo = TipoInmersion.BUCEO,
        fecha = "10/11/26",
        lugar = "Punta Marqués",
        profundidadMetros = "18",
        paradaSeguridadMin = "3",
        tiempoFondo = "3000",
        horaEntrada = "10:00",
        horaSalida = "10:45",
        temperaturaAireC = "20",
        temperaturaAguaC = "14"
    )

    private val apneaValida = buceoValido.copy(
        tipo = TipoInmersion.APNEA, paradaSeguridadMin = "", tiempoFondo = "", cantidadBajadas = "3"
    )

    @Test
    fun `un buceo completo no tiene errores`() {
        assertTrue(validarInmersion(buceoValido).isEmpty())
    }

    @Test
    fun `una apnea sin parada ni tiempo de fondo es valida`() {
        assertTrue(validarInmersion(apneaValida).isEmpty())
    }

    @Test
    fun `campos obligatorios vacios`() {
        val errores = validarInmersion(NuevaInmersionUiState())
        assertTrue(errores.keys.containsAll(listOf(Campo.FECHA, Campo.LUGAR, Campo.PROFUNDIDAD,
            Campo.HORA_ENTRADA, Campo.HORA_SALIDA, Campo.TEMP_AIRE, Campo.TEMP_AGUA)))
    }

    @Test
    fun `en buceo la parada minima es 3 minutos`() {
        assertEquals(setOf(Campo.PARADA), validarInmersion(buceoValido.copy(paradaSeguridadMin = "2")).keys)
    }

    @Test
    fun `tiempo de fondo con segundos mayores a 59 es invalido`() {
        assertEquals(setOf(Campo.TIEMPO_FONDO), validarInmersion(buceoValido.copy(tiempoFondo = "3075")).keys)
    }

    @Test
    fun `la salida no puede ser antes que la entrada`() {
        assertEquals(setOf(Campo.HORA_SALIDA), validarInmersion(buceoValido.copy(horaSalida = "09:00")).keys)
    }

    @Test
    fun `temperaturas fuera de rango`() {
        val errores = validarInmersion(buceoValido.copy(temperaturaAireC = "60", temperaturaAguaC = "-5"))
        assertEquals(setOf(Campo.TEMP_AIRE, Campo.TEMP_AGUA), errores.keys)
    }

    @Test
    fun `lastre solo se valida si se eligio usarlo`() {
        assertTrue(validarInmersion(buceoValido.copy(usoLastre = false, lastreKg = "")).isEmpty())
        assertEquals(setOf(Campo.LASTRE), validarInmersion(buceoValido.copy(usoLastre = true, lastreKg = "")).keys)
    }

    @Test
    fun `apnea con detalle exige bajadas validas`() {
        val conDetalle = apneaValida.copy(cargarDetalleBajadas = true, profundidadMetros = "",
            bajadas = listOf(BajadaForm("10", "0045"), BajadaForm("", "0100")))
        assertEquals(setOf(Campo.BAJADAS), validarInmersion(conDetalle.copy(cantidadBajadas = "2")).keys)
        assertTrue(validarInmersion(conDetalle.copy(cantidadBajadas = "1")).isEmpty())
    }
}
