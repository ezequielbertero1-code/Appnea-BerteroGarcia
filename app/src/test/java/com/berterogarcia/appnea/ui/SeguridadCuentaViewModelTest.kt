package com.berterogarcia.appnea.ui

import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ReglaDispatcherMain
import com.berterogarcia.appnea.dominio.modelo.TipoCuenta
import com.berterogarcia.appnea.dominio.repositorios.RepositorioCuenta
import com.berterogarcia.appnea.dominio.repositorios.ResultadoCuenta
import com.berterogarcia.appnea.ui.components.textoUi
import com.berterogarcia.appnea.ui.perfil.seguridad.CampoSeguridad
import com.berterogarcia.appnea.ui.perfil.seguridad.SeguridadCuentaViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SeguridadCuentaViewModelTest {

    @get:Rule val main = ReglaDispatcherMain()

    private class CuentaFalsa(var tipo: TipoCuenta? = TipoCuenta.LOCAL, var password: String = "clave1234") : RepositorioCuenta {
        var borrada = false
        override suspend fun tipoActual() = tipo
        override suspend fun cambiarPassword(actual: String, nueva: String) =
            if (actual != password) ResultadoCuenta.PasswordIncorrecta else { password = nueva; ResultadoCuenta.Ok }
        override suspend fun borrarCuenta(password: String?): ResultadoCuenta {
            if (tipo == TipoCuenta.LOCAL && password != this.password) return ResultadoCuenta.PasswordIncorrecta
            borrada = true
            return ResultadoCuenta.Ok
        }
    }

    private val cuenta = CuentaFalsa()

    @Test
    fun validaAntesDeCambiar() {
        val vm = SeguridadCuentaViewModel(cuenta)
        vm.onNuevaChange("corta")
        vm.onConfirmacionChange("otra")
        vm.cambiarPassword()
        val e = vm.uiState.value.errores
        assertEquals(setOf(CampoSeguridad.ACTUAL, CampoSeguridad.NUEVA, CampoSeguridad.CONFIRMACION), e.keys)
        assertEquals("clave1234", cuenta.password)
    }

    @Test
    fun laNuevaTieneQueSerDistinta() {
        val vm = SeguridadCuentaViewModel(cuenta)
        vm.onActualChange("clave1234")
        vm.onNuevaChange("clave1234")
        vm.onConfirmacionChange("clave1234")
        vm.cambiarPassword()
        assertEquals(textoUi(R.string.seguridad_error_igual), vm.uiState.value.errores[CampoSeguridad.NUEVA])
    }

    @Test
    fun cambiaLaContraseniaYLimpiaLosCampos() {
        val vm = SeguridadCuentaViewModel(cuenta)
        vm.onActualChange("clave1234")
        vm.onNuevaChange("nueva5678")
        vm.onConfirmacionChange("nueva5678")
        vm.cambiarPassword()
        val s = vm.uiState.value
        assertTrue(s.passwordCambiada)
        assertEquals("", s.actual)
        assertEquals("nueva5678", cuenta.password)
    }

    @Test
    fun contraseniaActualIncorrecta() {
        val vm = SeguridadCuentaViewModel(cuenta)
        vm.onActualChange("mal")
        vm.onNuevaChange("nueva5678")
        vm.onConfirmacionChange("nueva5678")
        vm.cambiarPassword()
        assertEquals(textoUi(R.string.seguridad_error_actual_incorrecta), vm.uiState.value.errores[CampoSeguridad.ACTUAL])
    }

    @Test
    fun borrarUnaCuentaLocalPideLaContrasenia() {
        val vm = SeguridadCuentaViewModel(cuenta)
        vm.pedirBorrado()
        vm.borrarCuenta()
        assertTrue(CampoSeguridad.PASSWORD_BORRAR in vm.uiState.value.errores)
        vm.onPasswordBorradoChange("mal")
        vm.borrarCuenta()
        assertFalse(cuenta.borrada)
        vm.onPasswordBorradoChange("clave1234")
        vm.borrarCuenta()
        assertTrue(cuenta.borrada)
        assertTrue(vm.uiState.value.cuentaBorrada)
    }

    @Test
    fun laCuentaDeGoogleSeBorraSinContrasenia() {
        cuenta.tipo = TipoCuenta.GOOGLE
        val vm = SeguridadCuentaViewModel(cuenta)
        assertFalse(vm.uiState.value.esLocal)
        vm.cambiarPassword() // no aplica: no hace nada
        vm.pedirBorrado()
        vm.borrarCuenta()
        assertTrue(cuenta.borrada)
    }
}
