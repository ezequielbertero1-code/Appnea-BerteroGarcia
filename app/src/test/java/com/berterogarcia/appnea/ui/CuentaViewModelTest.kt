package com.berterogarcia.appnea.ui

import app.cash.turbine.test
import com.berterogarcia.appnea.ReglaDispatcherMain
import com.berterogarcia.appnea.dominio.modelo.CategoriaEquipo
import com.berterogarcia.appnea.dominio.modelo.ContactoEmergencia
import com.berterogarcia.appnea.dominio.modelo.Equipo
import com.berterogarcia.appnea.dominio.modelo.PerfilUsuario
import com.berterogarcia.appnea.dominio.modelo.SesionApnea
import com.berterogarcia.appnea.fakes.FakeInmersiones
import com.berterogarcia.appnea.fakes.FakePerfil
import com.berterogarcia.appnea.fakes.FakeSesion
import com.berterogarcia.appnea.fakes.FakeSesionesApnea
import com.berterogarcia.appnea.inmersion
import com.berterogarcia.appnea.ui.cuenta.CuentaViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class CuentaViewModelTest {

    @get:Rule val main = ReglaDispatcherMain()

    private val sesion = FakeSesion("ana")
    private val perfil = FakePerfil(
        perfil = PerfilUsuario(nombre = "Ana", apellido = "Paz"),
        equipos = listOf(
            Equipo(id = 1, categoria = CategoriaEquipo.entries.first(), proximoMantenimiento = LocalDate.now().minusDays(1)),
            Equipo(id = 2, categoria = CategoriaEquipo.entries.first()),
            Equipo(id = 3, categoria = CategoriaEquipo.entries.first(), enUso = false, proximoMantenimiento = LocalDate.now().minusDays(1))
        ),
        contactos = listOf(
            ContactoEmergencia(id = 1, nombre = "Beto", telefono = "1"),
            ContactoEmergencia(id = 2, nombre = "Marta", telefono = "2", esPrincipal = true)
        )
    )
    private val inmersiones = FakeInmersiones(listOf(inmersion(id = 1, profundidad = 22.3f), inmersion(id = 2, profundidad = 12f)))
    private val sesiones = FakeSesionesApnea(
        listOf(
            SesionApnea(fecha = LocalDate.now(), nombreTabla = "CO2", completada = true, faseAlcanzada = 8, totalFases = 8, marcaSegundos = 95, factores = emptySet(), sensaciones = ""),
            SesionApnea(fecha = LocalDate.now(), nombreTabla = "O2", completada = false, faseAlcanzada = 2, totalFases = 8, marcaSegundos = 40, factores = emptySet(), sensaciones = "")
        )
    )
    private val entrenando = MutableStateFlow(false)

    private fun crear() = CuentaViewModel(sesion, perfil, inmersiones, sesiones, entrenando)

    @Test
    fun estadisticasConLosDatosReales() = runTest {
        crear().estadisticas.test {
            var e = awaitItem()
            if (e.inmersiones == 0) e = awaitItem()
            assertEquals(2, e.inmersiones)
            assertEquals("22,3", e.profundidadMaxima)
            assertEquals("60 min", e.tiempoTotalFondo)
            assertEquals(2, e.sesionesEntrenamiento)
            assertEquals(1, e.sesionesCompletas)
            assertEquals("01:35", e.mejorApnea)
        }
    }

    @Test
    fun resumenDelPerfil() = runTest {
        crear().resumen.test {
            var r = awaitItem()
            if (r.cargando) r = awaitItem()
            assertEquals("Ana Paz", r.perfil!!.nombreCompleto)
            assertEquals(2, r.equiposEnUso.size)
            // El que está guardado (no en uso) no cuenta aunque tenga el service vencido
            assertEquals(1, r.equiposConServicePendiente)
            assertEquals("Marta", r.contactoPrincipal!!.nombre)
        }
    }

    @Test
    fun cerrarSesionYAvisoSiHayUnEntrenamiento() {
        val vm = crear()
        assertFalse(vm.entrenamientoEnCurso.value)
        entrenando.value = true
        assertTrue(vm.entrenamientoEnCurso.value)

        vm.cerrarSesion()
        assertNull(vm.usuario.value)
    }
}
