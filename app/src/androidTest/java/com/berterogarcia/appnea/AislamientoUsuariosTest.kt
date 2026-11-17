package com.berterogarcia.appnea

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.berterogarcia.appnea.data.local.AppDatabase
import com.berterogarcia.appnea.dominio.modelo.CategoriaEquipo
import com.berterogarcia.appnea.data.local.EquipoEntity
import com.berterogarcia.appnea.dominio.modelo.Equipo
import com.berterogarcia.appnea.dominio.ProveedorUsuario
import com.berterogarcia.appnea.data.local.UsuarioEntity
import com.berterogarcia.appnea.data.repository.PerfilRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Un usuario no puede leer ni modificar los datos de otro, aunque conozca el id. */
@RunWith(AndroidJUnit4::class)
class AislamientoUsuariosTest {

    private lateinit var db: AppDatabase
    private val sesion = object : ProveedorUsuario {
        override val usuarioActual = MutableStateFlow<String?>("ana")
    }
    private lateinit var repo: PerfilRepository

    @Before
    fun crear() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        db.usuarioDao().insertar(UsuarioEntity("ana", "x"))
        db.usuarioDao().insertar(UsuarioEntity("beto", "x"))
        repo = PerfilRepository(db.perfilUsuarioDao(), db.equipoDao(), db.certificadoDao(), db.contactoEmergenciaDao(), sesion)
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun noSePuedeActualizarElEquipoDeOtro() = runBlocking {
        val idBeto = db.equipoDao().insertar(EquipoEntity(nombreUsuario = "beto", categoria = CategoriaEquipo.entries.first(), marca = "Mares"))
        // Ana intenta pisarlo cambiando el dueño en la entidad: el DAO no encuentra un equipo de "ana" con ese id
        assertFalse(db.equipoDao().actualizarSiEsDe(EquipoEntity(id = idBeto, nombreUsuario = "ana", categoria = CategoriaEquipo.entries.first(), marca = "Robado")))
        assertEquals("Mares", db.equipoDao().obtener(idBeto, "beto")!!.marca)
    }

    @Test
    fun sinSesionElRepositorioNoEscribe() = runBlocking {
        sesion.usuarioActual.value = null
        val g = runCatching { repo.guardarEquipo(Equipo(categoria = CategoriaEquipo.entries.first())) }
        assertTrue(g.exceptionOrNull() is IllegalStateException)
        assertEquals(0, db.equipoDao().observarTodos("ana").first().size)
    }

    @Test
    fun elRepositorioNoLeeNiBorraLoDeOtro() = runBlocking {
        val idBeto = db.equipoDao().insertar(EquipoEntity(nombreUsuario = "beto", categoria = CategoriaEquipo.entries.first(), marca = "Mares"))
        // Ana está logueada: el id de Beto no le devuelve nada ni se lo deja borrar
        assertEquals(null, repo.obtenerEquipo(idBeto))
        assertFalse(repo.eliminarEquipo(idBeto))
        assertEquals("Mares", db.equipoDao().obtener(idBeto, "beto")!!.marca)
    }

    @Test
    fun losFlujosActualesSiguenALaSesion() = runBlocking {
        repo.guardarEquipo(Equipo(categoria = CategoriaEquipo.entries.first(), marca = "De Ana"))
        assertEquals(listOf("De Ana"), repo.equipamientoActual.first().map { it.marca })
        sesion.usuarioActual.value = "beto"
        assertEquals(emptyList<String>(), repo.equipamientoActual.first().map { it.marca })
        sesion.usuarioActual.value = null
        assertEquals(emptyList<String>(), repo.equipamientoActual.first())
    }
}
