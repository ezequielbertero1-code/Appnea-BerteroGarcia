package com.berterogarcia.appnea.ui

import com.berterogarcia.appnea.data.local.IntentoApneaEntity
import com.berterogarcia.appnea.data.local.SesionApneaEntity
import com.berterogarcia.appnea.dominio.FactorEntrenamiento
import com.berterogarcia.appnea.ui.estadisticas.PeriodoEstadisticas
import com.berterogarcia.appnea.ui.estadisticas.resumirEntrenamiento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ResumenEntrenamientoTest {

    private val hoy = LocalDate.of(2026, 11, 16)

    private fun sesion(
        id: Long, fecha: LocalDate, marca: Int, duracion: Int?, hora: LocalTime? = null, completada: Boolean = true
    ) = SesionApneaEntity(
        id = id, fecha = fecha, nombreTabla = "T", completada = completada, faseAlcanzada = 1, totalFases = 8,
        marcaSegundos = marca, factores = emptySet<FactorEntrenamiento>(), sensaciones = "",
        horaInicio = hora, duracionSegundos = duracion
    )

    private fun intento(fecha: LocalDate, ms: Long) =
        IntentoApneaEntity(fecha = fecha, hora = LocalTime.NOON, duracionMs = ms)

    @Test
    fun sumaElTiempoYCuentaLasSesionesSinApnea() {
        val sesiones = listOf(
            sesion(1, hoy, marca = 0, duracion = 40, completada = false),   // cortada en la preparación
            sesion(2, hoy.minusDays(2), marca = 90, duracion = 600),
            sesion(3, hoy.minusDays(3), marca = 75, duracion = null)        // vieja: sin duración
        )
        val r = resumirEntrenamiento(sesiones, PeriodoEstadisticas.UN_MES, hoy)
        assertEquals(3, r.sesiones)
        assertEquals(1, r.sinApnea)
        assertEquals(listOf(3L, 2L), r.serie.map { it.id }) // sin la de marca 0, de la más vieja a la más nueva
        assertEquals(640, r.tiempoEntrenadoSegundos)
    }

    @Test
    fun ordenaLasSesionesDelMismoDiaPorHora() {
        val sesiones = listOf(
            sesion(1, hoy, marca = 60, duracion = 300, hora = LocalTime.of(18, 0)),
            sesion(2, hoy, marca = 70, duracion = 300, hora = LocalTime.of(9, 0))
        )
        val r = resumirEntrenamiento(sesiones, PeriodoEstadisticas.UN_MES, hoy)
        assertEquals(listOf(2L, 1L), r.serie.map { it.id })
    }

    @Test
    fun resumeLosIntentosDelCronometroPorDia() {
        val intentos = listOf(
            intento(hoy, 95_400),
            intento(hoy, 120_900),
            intento(hoy.minusDays(1), 80_000),
            intento(hoy.minusMonths(2), 200_000) // fuera del período
        )
        val r = resumirEntrenamiento(emptyList(), PeriodoEstadisticas.UN_MES, hoy, intentos)
        assertFalse(r.vacio)
        assertEquals(3, r.intentosCronometro)
        assertEquals(120, r.mejorIntentoSegundos)
        assertEquals(listOf(hoy.minusDays(1) to 80, hoy to 120), r.serieCronometro)
        assertEquals(296, r.tiempoEntrenadoSegundos) // 95,4 + 120,9 + 80 = 296,3 s
    }

    @Test
    fun sinSesionesNiIntentosEstaVacio() {
        assertTrue(resumirEntrenamiento(emptyList(), PeriodoEstadisticas.UN_MES, hoy).vacio)
    }
}
