package com.berterogarcia.appnea.ui

import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ReglaDispatcherMain
import com.berterogarcia.appnea.dominio.EstadoIntentos
import com.berterogarcia.appnea.dominio.PoliticaIntentos
import com.berterogarcia.appnea.dominio.modelo.CuentaGoogle
import com.berterogarcia.appnea.dominio.modelo.ResultadoGoogle
import com.berterogarcia.appnea.fakes.FakeCuentasGoogle
import com.berterogarcia.appnea.fakes.FakePreferenciasLogin
import com.berterogarcia.appnea.fakes.FakeSesion
import com.berterogarcia.appnea.fakes.FakeUsuarios
import com.berterogarcia.appnea.ui.components.textoUi
import com.berterogarcia.appnea.ui.login.LoginViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule val main = ReglaDispatcherMain(StandardTestDispatcher())

    private val usuarios = FakeUsuarios()
    private val google = FakeCuentasGoogle(mutableSetOf("ana@gmail.com"))
    private val sesion = FakeSesion(usuario = null)
    private val preferencias = FakePreferenciasLogin()

    private fun TestScope.crear() =
        LoginViewModel(usuarios, google, sesion, preferencias, reloj = { testScheduler.currentTime })

    private fun TestScope.intentar(vm: LoginViewModel, usuario: String, password: String) {
        vm.onUsuarioChange(usuario)
        vm.onPasswordChange(password)
        vm.login()
        runCurrent()
    }

    @Test
    fun loginCorrectoAbreLaSesionYRecuerdaElUsuario() = runTest {
        preferencias.intentos = EstadoIntentos(fallidos = 3)
        val vm = crear()
        intentar(vm, "ana ", "clave1234") // el espacio del autocompletado no molesta

        assertTrue(vm.uiState.value.exitoso)
        assertEquals("ana", sesion.usuarioActual.value)
        assertEquals("ana", preferencias.ultimoUsuario)
        assertEquals(EstadoIntentos(), preferencias.intentos)
    }

    @Test
    fun camposVaciosNoConsultanLaBase() = runTest {
        usuarios.falla = true // si consultara, mostraría el error de verificación
        val vm = crear()
        vm.login()
        runCurrent()
        assertEquals(textoUi(R.string.login_error_usuario), vm.uiState.value.errorUsuario)
        assertEquals(textoUi(R.string.login_error_password), vm.uiState.value.errorPassword)
        assertNull(vm.uiState.value.errorGeneral)
    }

    @Test
    fun cincoFallosBloqueanTreintaSegundos() = runTest {
        val vm = crear()
        repeat(3) { intentar(vm, "ana", "mal") }
        assertNotNull("avisa que quedan 2 intentos", vm.uiState.value.aviso)
        repeat(2) { intentar(vm, "ana", "mal") }

        assertEquals(30, vm.uiState.value.bloqueadoSegundos)
        assertFalse(vm.uiState.value.habilitado)
        // Bloqueado: ni la contraseña correcta entra
        intentar(vm, "ana", "clave1234")
        assertFalse(vm.uiState.value.exitoso)

        advanceTimeBy(30_001)
        runCurrent()
        assertEquals(0, vm.uiState.value.bloqueadoSegundos)
        intentar(vm, "ana", "clave1234")
        assertTrue(vm.uiState.value.exitoso)
    }

    @Test
    fun elBloqueoSigueAunqueSeCierreLaApp() = runTest {
        repeat(PoliticaIntentos.MAX_FALLIDOS) { intentar(crear(), "ana", "mal") }
        // Pantalla nueva (la app se cerró): lee el bloqueo guardado
        val vm = crear()
        runCurrent()
        assertTrue(vm.uiState.value.bloqueado)
    }

    @Test
    fun errorDeLaBaseMuestraUnAvisoYNoCuentaComoFallo() = runTest {
        usuarios.falla = true
        val vm = crear()
        intentar(vm, "ana", "clave1234")
        assertEquals(textoUi(R.string.login_error_verificar), vm.uiState.value.errorGeneral)
        assertEquals(EstadoIntentos(), preferencias.intentos)
    }

    @Test
    fun googleConCuentaRegistradaEntraDirecto() = runTest {
        val vm = crear()
        vm.loginConGoogle { ResultadoGoogle.Ok(CuentaGoogle("ana@gmail.com", "Ana", "Paz")) }
        runCurrent()
        assertTrue(vm.uiState.value.exitoso)
        assertEquals("ana@gmail.com", sesion.usuarioActual.value)
        assertNull("no pisa el último usuario de contraseña", preferencias.ultimoUsuario)
    }

    @Test
    fun googleConCuentaNuevaVaAlRegistro() = runTest {
        val vm = crear()
        val cuenta = CuentaGoogle("nuevo@gmail.com", "Leo", "Sur")
        vm.loginConGoogle { ResultadoGoogle.Ok(cuenta) }
        runCurrent()
        assertEquals(cuenta, vm.uiState.value.cuentaParaRegistro)
        assertNull(sesion.usuarioActual.value)
    }

    @Test
    fun googleCanceladoNoMuestraErrorYSinCuentasSi() = runTest {
        val vm = crear()
        vm.loginConGoogle { ResultadoGoogle.Cancelado }
        runCurrent()
        assertNull(vm.uiState.value.errorGeneral)
        assertFalse(vm.uiState.value.cargandoGoogle)

        vm.loginConGoogle { ResultadoGoogle.SinCuentas }
        runCurrent()
        assertEquals(textoUi(R.string.login_error_google_sin_cuentas), vm.uiState.value.errorGeneral)
    }

    @Test
    fun googleFuncionaAunqueLaContraseniaEsteBloqueada() = runTest {
        val vm = crear()
        repeat(PoliticaIntentos.MAX_FALLIDOS) { intentar(vm, "ana", "mal") }
        vm.loginConGoogle { ResultadoGoogle.Ok(CuentaGoogle("ana@gmail.com", "Ana", "Paz")) }
        runCurrent()
        assertTrue(vm.uiState.value.exitoso)
    }
}
