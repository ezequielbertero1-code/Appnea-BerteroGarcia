package com.berterogarcia.appnea

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.berterogarcia.appnea.data.local.AppDatabase
import com.berterogarcia.appnea.data.local.UsuarioEntity
import com.berterogarcia.appnea.data.local.aEntidad
import com.berterogarcia.appnea.data.local.aModelo
import com.berterogarcia.appnea.dominio.modelo.ContactoEmergencia
import com.berterogarcia.appnea.dominio.modelo.CondicionesRegistro
import com.berterogarcia.appnea.dominio.modelo.IntentoLibre
import com.berterogarcia.appnea.dominio.modelo.SesionApnea
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

/** Consultas de los DAOs contra una base Room en memoria (no toca los datos del teléfono). */
@RunWith(AndroidJUnit4::class)
class DaosTest {

    private lateinit var db: AppDatabase
    private val hoy = LocalDate.of(2026, 11, 16)

    @Before
    fun crear() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        db.usuarioDao().insertar(UsuarioEntity("ana", "x"))
        db.usuarioDao().insertar(UsuarioEntity("beto", "x"))
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun inmersionesSoloDelUsuarioYCondicionesQueSeCompletanDespues() = runBlocking {
        val dao = db.inmersionDao()
        val id = dao.insertar(inmersionDePrueba(lugar = "De Ana").aEntidad("ana"))
        dao.insertar(inmersionDePrueba(lugar = "De Beto").aEntidad("beto"))
        assertEquals(listOf("De Ana"), dao.obtenerTodas("ana").first().map { it.lugar })

        // El worker completa el clima por id, aunque la app esté cerrada
        val clima = CondicionesRegistro(temperaturaC = 17.5, olaM = 0.8, faseLunar = "Llena", iluminacionLunaPorc = 99, conDatosOnline = true)
        assertEquals(1, dao.actualizarCondiciones(id, clima))
        assertEquals(clima, dao.obtenerPorId(id, "ana").first()!!.condiciones)
    }

    @Test
    fun nadiePuedeEditarLaInmersionDeOtro() = runBlocking {
        val dao = db.inmersionDao()
        val id = dao.insertar(inmersionDePrueba(lugar = "De Beto").aEntidad("beto"))
        assertFalse(dao.actualizarSiEsDe(inmersionDePrueba(id = id, lugar = "Robada").aEntidad("ana"), "ana"))
        assertEquals(0, dao.eliminar(id, "ana"))
        assertEquals("De Beto", dao.obtenerPorId(id, "beto").first()!!.lugar)
    }

    @Test
    fun sesionesOrdenadasPorFechaYHoraConLasViejasAlFinal() = runBlocking {
        val dao = db.sesionApneaDao()
        fun sesion(nombre: String, fecha: LocalDate, hora: LocalTime?) = SesionApnea(
            fecha = fecha, nombreTabla = nombre, completada = true, faseAlcanzada = 1, totalFases = 1,
            marcaSegundos = 0, factores = emptySet(), sensaciones = "", horaInicio = hora
        ).aEntidad("ana")
        dao.insertar(sesion("vieja sin hora", hoy, null))
        dao.insertar(sesion("mañana", hoy, LocalTime.of(9, 0)))
        dao.insertar(sesion("noche", hoy, LocalTime.of(21, 0)))
        dao.insertar(sesion("ayer", hoy.minusDays(1), LocalTime.of(23, 0)))
        assertEquals(
            listOf("noche", "mañana", "vieja sin hora", "ayer"),
            dao.obtenerTodas("ana").first().map { it.nombreTabla }
        )
    }

    @Test
    fun intentosDelDia() = runBlocking {
        val dao = db.intentoApneaDao()
        dao.insertar(IntentoLibre(fecha = hoy, hora = LocalTime.of(10, 0), duracionMs = 60_000).aEntidad("ana"))
        dao.insertar(IntentoLibre(fecha = hoy, hora = LocalTime.of(11, 0), duracionMs = 90_000).aEntidad("ana"))
        dao.insertar(IntentoLibre(fecha = hoy.minusDays(1), hora = LocalTime.of(10, 0), duracionMs = 30_000).aEntidad("ana"))
        dao.insertar(IntentoLibre(fecha = hoy, hora = LocalTime.of(12, 0), duracionMs = 45_000).aEntidad("beto"))
        assertEquals(listOf(90_000L, 60_000L), dao.obtenerDelDia("ana", hoy).first().map { it.duracionMs })
        assertEquals(3, dao.obtenerTodos("ana").first().size)
    }

    @Test
    fun siempreQuedaUnContactoPrincipal() = runBlocking {
        val dao = db.contactoEmergenciaDao()
        val marta = dao.guardar(ContactoEmergencia(nombre = "Marta", telefono = "1").aEntidad("ana"))
        val beto = dao.guardar(ContactoEmergencia(nombre = "Beto", telefono = "2").aEntidad("ana"))
        fun principales() = runBlocking { dao.observarTodos("ana").first().filter { it.esPrincipal }.map { it.aModelo().nombre } }

        assertEquals("el primero queda como principal", listOf("Marta"), principales())
        dao.hacerPrincipal(beto, "ana")
        assertEquals(listOf("Beto"), principales())
        // Borrar el principal pasa la marca al que queda
        assertTrue(dao.eliminarYReasignar(beto, "ana") > 0)
        assertEquals(listOf("Marta"), principales())
        assertEquals(marta, dao.observarTodos("ana").first().single().id)
    }
}
