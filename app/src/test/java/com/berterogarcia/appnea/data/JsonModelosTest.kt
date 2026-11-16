package com.berterogarcia.appnea.data

import com.berterogarcia.appnea.data.local.BajadaApnea
import com.berterogarcia.appnea.data.local.CondicionesRegistro
import com.berterogarcia.appnea.data.local.RegistroApnea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Lo que se guarda como JSON en la base tiene que volver idéntico al leerlo. */
class JsonModelosTest {

    @Test
    fun `registro de apnea ida y vuelta`() {
        val r = RegistroApnea(2, 15.5f, sincope = true, bajadas = listOf(BajadaApnea(10f, 45), BajadaApnea(15.5f, 70)))
        assertEquals(r, RegistroApnea.desdeJson(r.aJson()))
    }

    @Test
    fun `condiciones ida y vuelta con nulos`() {
        val c = CondicionesRegistro(temperaturaC = 18.5, olaM = null, estadoMarea = "Subiendo",
            faseLunar = "Luna llena", iluminacionLunaPorc = 98, conDatosOnline = true)
        assertEquals(c, CondicionesRegistro.desdeJson(c.aJson()))
    }

    @Test
    fun `json corrupto devuelve null en vez de crashear`() {
        assertNull(RegistroApnea.desdeJson("{no es json"))
        assertNull(CondicionesRegistro.desdeJson("[]"))
    }
}
