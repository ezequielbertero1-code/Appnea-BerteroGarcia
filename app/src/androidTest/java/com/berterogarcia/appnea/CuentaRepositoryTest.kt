package com.berterogarcia.appnea

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.berterogarcia.appnea.data.local.AppDatabase
import com.berterogarcia.appnea.data.local.UsuarioEntity
import com.berterogarcia.appnea.data.local.aEntidad
import com.berterogarcia.appnea.data.repository.CuentaRepository
import com.berterogarcia.appnea.data.repository.UsuarioRepository
import com.berterogarcia.appnea.data.seguridad.SeguridadPassword
import com.berterogarcia.appnea.dominio.EstadoIntentos
import com.berterogarcia.appnea.dominio.GestorSesion
import com.berterogarcia.appnea.dominio.PreferenciasLogin
import com.berterogarcia.appnea.dominio.modelo.ContactoEmergencia
import com.berterogarcia.appnea.dominio.modelo.TipoCuenta
import com.berterogarcia.appnea.dominio.repositorios.ResultadoCuenta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Cambiar la contraseña y borrar la cuenta contra Room en memoria (no toca los datos del teléfono). */
@RunWith(AndroidJUnit4::class)
class CuentaRepositoryTest {

    private lateinit var db: AppDatabase
    private val sesion = object : GestorSesion {
        override val usuarioActual = MutableStateFlow<String?>("ana")
        override fun guardarUsuario(nombreUsuario: String) { usuarioActual.value = nombreUsuario }
        override fun obtenerUsuario() = usuarioActual.value
        override fun cerrarSesion() { usuarioActual.value = null }
    }
    private val preferencias = object : PreferenciasLogin {
        override var ultimoUsuario: String? = "ana"
        var intentos = EstadoIntentos(fallidos = 2)
        override fun leerIntentos() = intentos
        override fun guardarIntentos(estado: EstadoIntentos) { intentos = estado }
    }
    private lateinit var repo: CuentaRepository

    @Before
    fun crear() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        db.usuarioDao().insertar(UsuarioEntity("ana", SeguridadPassword.hashear("clave1234")))
        db.usuarioDao().insertar(UsuarioEntity("leo@gmail.com", "", TipoCuenta.GOOGLE))
        repo = CuentaRepository(db.usuarioDao(), sesion, preferencias)
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun cambiarPasswordPideLaActual() = runBlocking {
        assertEquals(ResultadoCuenta.PasswordIncorrecta, repo.cambiarPassword("mal", "nueva5678"))
        assertEquals(ResultadoCuenta.Ok, repo.cambiarPassword("clave1234", "nueva5678"))
        val login = UsuarioRepository(db.usuarioDao())
        assertTrue(login.validar("ana", "nueva5678"))
        assertFalse(login.validar("ana", "clave1234"))
    }

    @Test
    fun borrarLaCuentaBorraSusDatosYCierraLaSesion() = runBlocking {
        db.inmersionDao().insertar(inmersionDePrueba().aEntidad("ana"))
        db.contactoEmergenciaDao().guardar(ContactoEmergencia(nombre = "Marta", telefono = "1").aEntidad("ana"))
        db.inmersionDao().insertar(inmersionDePrueba(lugar = "De Leo").aEntidad("leo@gmail.com"))

        assertEquals(ResultadoCuenta.PasswordIncorrecta, repo.borrarCuenta("mal"))
        assertEquals("ana", sesion.usuarioActual.value)

        assertEquals(ResultadoCuenta.Ok, repo.borrarCuenta("clave1234"))
        assertNull(sesion.usuarioActual.value)
        assertNull(preferencias.ultimoUsuario)
        assertEquals(EstadoIntentos(), preferencias.intentos)
        assertFalse(db.usuarioDao().existeUsuario("ana"))
        assertTrue(db.inmersionDao().obtenerTodas("ana").first().isEmpty())
        assertTrue(db.contactoEmergenciaDao().observarTodos("ana").first().isEmpty())
        // Lo de otra cuenta no se toca
        assertEquals(1, db.inmersionDao().obtenerTodas("leo@gmail.com").first().size)
    }

    @Test
    fun laCuentaDeGoogleNoTieneContraseniaYSeBorraSinPedirla() = runBlocking {
        sesion.usuarioActual.value = "leo@gmail.com"
        assertEquals(TipoCuenta.GOOGLE, repo.tipoActual())
        assertFalse("no se entra escribiendo una contraseña", UsuarioRepository(db.usuarioDao()).validar("leo@gmail.com", ""))
        assertEquals(ResultadoCuenta.PasswordIncorrecta, repo.cambiarPassword("", "nueva5678"))
        assertEquals(ResultadoCuenta.Ok, repo.borrarCuenta(null))
        assertFalse(db.usuarioDao().existeUsuario("leo@gmail.com"))
    }
}
