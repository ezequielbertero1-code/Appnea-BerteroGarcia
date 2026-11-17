package com.berterogarcia.appnea

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.berterogarcia.appnea.dominio.RegistrarInmersion
import com.berterogarcia.appnea.entrenamiento.MotorEntrenamiento
import com.berterogarcia.appnea.ui.entrenamiento.CronometroScreen
import com.berterogarcia.appnea.ui.entrenamiento.CronometroViewModel
import com.berterogarcia.appnea.ui.entrenamiento.EntrenamientoApneaViewModel
import com.berterogarcia.appnea.ui.entrenamiento.EntrenamientoUiState
import com.berterogarcia.appnea.ui.nuevainmersion.NuevaInmersionScreen
import com.berterogarcia.appnea.ui.nuevainmersion.NuevaInmersionUiState
import com.berterogarcia.appnea.ui.nuevainmersion.NuevaInmersionViewModel
import com.berterogarcia.appnea.ui.nuevainmersion.aBorradorJson
import com.berterogarcia.appnea.ui.nuevainmersion.conInmersion
import com.berterogarcia.appnea.ui.theme.AppneaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Flujos críticos de punta a punta en la UI, con repositorios en memoria. */
@RunWith(AndroidJUnit4::class)
class FlujosUiTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val scopeMotor = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @After
    fun cortarMotor() = scopeMotor.cancel()

    private fun texto(@StringRes id: Int) = compose.activity.getString(id)

    @Test
    fun registrarUnaInmersion() {
        val repo = InmersionesEnMemoria()
        var guardado = false
        // Las horas se eligen con el reloj de Material (difícil de manejar desde un test): el resto
        // del formulario viene de un borrador y en la pantalla se cambia el lugar y se recorren las etapas.
        val borrador = NuevaInmersionUiState().conInmersion(inmersionDePrueba()).aBorradorJson()
        val vm = NuevaInmersionViewModel(
            repo, RegistrarInmersion(repo, CondicionesFijas(), { _, _, _ -> }), PerfilVacio(), SinGps(),
            idEditar = null, estadoGuardado = SavedStateHandle(mapOf("borrador_inmersion" to borrador))
        )
        compose.setContent {
            AppneaTheme { NuevaInmersionScreen(viewModel = vm, onGuardado = { guardado = true }, onCargarEquipo = {}) }
        }

        compose.onNodeWithText("Punta Marqués").performTextReplacement("Rada Tilly")
        repeat(4) { compose.onNodeWithText(texto(R.string.nueva_siguiente)).performClick() }
        compose.onNodeWithText(texto(R.string.accion_guardar)).performClick()

        compose.waitUntil(5_000) { guardado }
        assertEquals("Rada Tilly", repo.filas.value.single().lugar)
    }

    @Test
    fun iniciarYAbortarUnaTablaYGuardarla() {
        val sesiones = SesionesEnMemoria()
        val motor = MotorEntrenamiento(scopeMotor, sonido = null)
        val entrenamiento = EntrenamientoApneaViewModel(TablasEnMemoria(), sesiones, motor)
        val cronometro = CronometroViewModel(IntentosEnMemoria())
        val tabla = EntrenamientoUiState().tablasPredefinidas.first()
        compose.setContent {
            AppneaTheme {
                CronometroScreen(entrenamientoViewModel = entrenamiento, onCrearTabla = {}, cronometroViewModel = cronometro)
            }
        }

        compose.onAllNodes(hasText(texto(R.string.entrenar_modo_tablas))).onFirst().performClick() // el selector (también hay un título "Tablas")
        compose.onAllNodes(hasText(tabla.nombre)).onFirst().performScrollTo().performClick()
        compose.onNodeWithText(texto(R.string.entrenar_acompanado)).performScrollTo().performClick()
        // Mientras corre, el anillo de la cuenta regresiva se anima sin parar y Compose nunca queda
        // "quieto": el reloj de las animaciones se avanza a mano hasta que termina la tabla.
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText(texto(R.string.entrenar_iniciar)).performScrollTo().performClick()
        compose.waitUntil(5_000) { motor.estado.value.corriendo }
        compose.mainClock.advanceTimeBy(1_000)

        // "Abortar" corta directo (la confirmación es solo para el botón atrás)
        compose.onNodeWithText(texto(R.string.entrenar_abortar)).performClick()
        compose.waitUntil(5_000) { motor.estado.value.resultado != null }
        assertFalse(motor.estado.value.corriendo)
        compose.mainClock.autoAdvance = true

        compose.onNodeWithText(texto(R.string.accion_guardar)).performClick()
        compose.waitUntil(5_000) { sesiones.filas.value.isNotEmpty() }
        val guardada = sesiones.filas.value.single()
        assertEquals(tabla.nombre, guardada.nombreTabla)
        assertFalse(guardada.completada)
        assertTrue(guardada.horaInicio != null)
    }
}
