package com.berterogarcia.appnea.ui

import androidx.lifecycle.SavedStateHandle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ReglaDispatcherMain
import com.berterogarcia.appnea.dominio.modelo.CuentaGoogle
import com.berterogarcia.appnea.fakes.FakeCuentasGoogle
import com.berterogarcia.appnea.fakes.FakeRegistro
import com.berterogarcia.appnea.fakes.FakeSesion
import com.berterogarcia.appnea.ui.components.textoUi
import com.berterogarcia.appnea.ui.registro.CampoRegistro
import com.berterogarcia.appnea.ui.registro.PasoRegistro
import com.berterogarcia.appnea.ui.registro.RegistroViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class RegistroViewModelTest {

    @get:Rule val main = ReglaDispatcherMain()

    private val registro = FakeRegistro(mutableSetOf("ocupado"))
    private val google = FakeCuentasGoogle()
    private val sesion = FakeSesion(usuario = null)

    private fun crear(cuenta: CuentaGoogle? = null, estado: SavedStateHandle = SavedStateHandle()) =
        RegistroViewModel(registro, google, sesion, cuenta, estado)

    private fun RegistroViewModel.completarCuenta(usuario: String = "leo") {
        onUsuarioChange(usuario)
        onPasswordChange("clave1234")
        onConfirmacionChange("clave1234")
    }

    private fun RegistroViewModel.completarResto() {
        onNombreChange("Leo")
        onApellidoChange("Sur")
        onEmailChange("leo@mail.com")
        onTelefonoChange("+54 297 4111111")
        onNacimientoChange(LocalDate.of(1995, 3, 1))
        siguiente()
        onContactoNombreChange("Marta")
        onContactoTelefonoChange("+54 297 4222222")
        siguiente()
        onAceptaAvisoChange(true)
    }

    @Test
    fun pasoConErroresNoAvanza() {
        val vm = crear()
        vm.onPasswordChange("corta")
        vm.siguiente()
        val s = vm.uiState.value
        assertEquals(PasoRegistro.CUENTA, s.paso)
        assertTrue(CampoRegistro.USUARIO in s.errores)
        assertTrue(CampoRegistro.PASSWORD in s.errores)
        assertTrue(CampoRegistro.CONFIRMACION in s.errores)
    }

    @Test
    fun usuarioOcupadoSeAvisaEnElPrimerPaso() {
        val vm = crear()
        vm.completarCuenta("ocupado")
        vm.siguiente()
        assertEquals(PasoRegistro.CUENTA, vm.uiState.value.paso)
        assertEquals(textoUi(R.string.registro_error_usuario_existe), vm.uiState.value.errores[CampoRegistro.USUARIO])
    }

    @Test
    fun registroCompletoCreaLaCuentaYEntra() {
        val vm = crear()
        vm.completarCuenta()
        vm.siguiente()
        assertEquals(PasoRegistro.PERSONALES, vm.uiState.value.paso)
        vm.completarResto()
        assertEquals(PasoRegistro.CONFIRMAR, vm.uiState.value.paso)

        vm.crearCuenta()

        assertTrue(vm.uiState.value.registroOk)
        assertEquals("leo", sesion.usuarioActual.value)
        val creado = registro.registros.single()
        assertEquals("Leo", creado.perfil.nombre)
        assertTrue(creado.contacto.esPrincipal)
    }

    @Test
    fun elContactoNoPuedeSerElPropioTelefono() {
        val vm = crear()
        vm.completarCuenta()
        vm.siguiente()
        vm.completarResto()
        vm.onContactoTelefonoChange("+54 297 4111111")
        vm.crearCuenta()
        assertFalse(vm.uiState.value.registroOk)
        assertEquals(PasoRegistro.EMERGENCIA, vm.uiState.value.paso)
        assertEquals(textoUi(R.string.registro_error_contacto_propio), vm.uiState.value.errores[CampoRegistro.CONTACTO_TELEFONO])
    }

    @Test
    fun errorAlCrearLaCuentaMuestraUnAviso() {
        registro.falla = true
        val vm = crear()
        vm.completarCuenta()
        vm.siguiente()
        vm.completarResto()
        vm.crearCuenta()
        assertFalse(vm.uiState.value.registroOk)
        assertEquals(textoUi(R.string.registro_error_crear), vm.uiState.value.errorGeneral)
        assertEquals(null, sesion.usuarioActual.value)
    }

    @Test
    fun elBorradorSobreviveSinLasContrasenias() {
        val estado = SavedStateHandle()
        crear(estado = estado).apply {
            completarCuenta()
            siguiente()
            onNombreChange("Leo")
        }
        // Android mató la app: se crea otro ViewModel con el mismo SavedStateHandle
        val s = crear(estado = estado).uiState.value
        assertEquals("leo", s.usuario)
        assertEquals("Leo", s.nombre)
        assertEquals("", s.password)
        assertEquals("", s.confirmacion)
    }

    @Test
    fun conGoogleEmpiezaEnDatosPersonalesYRegistraLaCuenta() {
        val vm = crear(CuentaGoogle("leo@gmail.com", "Leo", "Sur"))
        assertEquals(PasoRegistro.PERSONALES, vm.uiState.value.paso)
        assertEquals("Leo", vm.uiState.value.nombre)

        vm.onTelefonoChange("+54 297 4111111")
        vm.onNacimientoChange(LocalDate.of(1995, 3, 1))
        vm.siguiente()
        vm.onContactoNombreChange("Marta")
        vm.onContactoTelefonoChange("+54 297 4222222")
        vm.siguiente()
        vm.onAceptaAvisoChange(true)
        vm.crearCuenta()

        assertTrue(vm.uiState.value.registroOk)
        assertEquals("leo@gmail.com", google.registradas.single().first)
        assertEquals("leo@gmail.com", sesion.usuarioActual.value)
        assertTrue("no crea un usuario con contraseña", registro.registros.isEmpty())
    }
}
