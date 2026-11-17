package com.berterogarcia.appnea.dominio

import com.berterogarcia.appnea.dominio.modelo.CondicionesRegistro
import com.berterogarcia.appnea.dominio.modelo.Ubicacion
import com.berterogarcia.appnea.fakes.FakeCondiciones
import com.berterogarcia.appnea.fakes.FakeInmersiones
import com.berterogarcia.appnea.fakes.FakeProgramador
import com.berterogarcia.appnea.inmersion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class RegistrarInmersionTest {

    private val repo = FakeInmersiones()
    private val condiciones = FakeCondiciones()
    private val programador = FakeProgramador()
    private val registrar = RegistrarInmersion(repo, condiciones, programador)
    private val lugar = Ubicacion(-45.86, -67.48)

    /** Condiciones completas (con datos de internet) que ya tenía una inmersión guardada. */
    private val completas = CondicionesRegistro(temperaturaC = 18.0, faseLunar = "Nueva", iluminacionLunaPorc = 0, conDatosOnline = true)

    @Test
    fun nuevaSeGuardaAlInstanteYProgramaElClima() = runTest {
        val i = inmersion(id = 0)
        assertTrue(registrar(i, anterior = null, ubicacion = lugar))

        val guardada = repo.filas.value.single()
        // Sin internet todavía: solo la fase lunar
        assertFalse(guardada.condiciones!!.conDatosOnline)
        assertEquals(Triple(guardada.id, lugar, i.fecha.atTime(i.horaEntrada)), programador.programados.single())
    }

    @Test
    fun editarSinCambiarMomentoNiLugarConservaLasCondiciones() = runTest {
        val anterior = inmersion(id = 1).copy(condiciones = completas)
        repo.filas.value = listOf(anterior)

        assertTrue(registrar(anterior.copy(comentarios = "Cambié el comentario"), anterior, lugar))

        val guardada = repo.filas.value.single()
        assertEquals("Cambié el comentario", guardada.comentarios)
        assertEquals(completas, guardada.condiciones)
        assertTrue(programador.programados.isEmpty())
        assertTrue(condiciones.pedidos.isEmpty())
    }

    @Test
    fun editarLaHoraVuelveAPedirLasCondiciones() = runTest {
        val anterior = inmersion(id = 1).copy(condiciones = completas)
        repo.filas.value = listOf(anterior)

        assertTrue(registrar(anterior.copy(horaEntrada = LocalTime.of(15, 0)), anterior, lugar))

        assertFalse(repo.filas.value.single().condiciones!!.conDatosOnline)
        assertEquals(1L, programador.programados.single().first)
    }

    @Test
    fun editarUnaQueYaNoExisteNoProgramaNada() = runTest {
        val anterior = inmersion(id = 7)
        assertFalse(registrar(anterior.copy(lugar = "Otro"), anterior, lugar))
        assertTrue(programador.programados.isEmpty())
    }

    @Test
    fun sinSesionNoSeGuarda() = runTest {
        repo.sinSesion = true
        assertFalse(registrar(inmersion(id = 0), null, lugar))
        assertTrue(programador.programados.isEmpty())
    }
}
