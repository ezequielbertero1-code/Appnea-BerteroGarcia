package com.berterogarcia.appnea.ui

import com.berterogarcia.appnea.dominio.repositorios.RepositorioIntentos
import com.berterogarcia.appnea.dominio.modelo.IntentoLibre
import com.berterogarcia.appnea.ui.entrenamiento.CronometroViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Repositorio en memoria: suficiente para probar el ViewModel sin Room. */
private class IntentosEnMemoria : RepositorioIntentos {
    val filas = MutableStateFlow<List<IntentoLibre>>(emptyList())
    override val todosLosIntentos: Flow<List<IntentoLibre>> = filas
    override fun delDia(fecha: LocalDate): Flow<List<IntentoLibre>> = filas.map { l -> l.filter { it.fecha == fecha } }
    override suspend fun guardar(intento: IntentoLibre): Boolean {
        filas.value = listOf(intento.copy(id = filas.value.size + 1L)) + filas.value
        return true
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CronometroViewModelTest {

    private val dao = IntentosEnMemoria()
    private var relojMs = 0L
    private val fin = LocalDateTime.of(2026, 11, 16, 10, 0, 0)

    @Before fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun despues() = Dispatchers.resetMain()

    private fun viewModel() = CronometroViewModel(dao, reloj = { relojMs }, ahora = { fin })

    @Test
    fun pararGuardaElIntentoConSuHoraDeInicio() {
        val vm = viewModel()
        vm.onIniciar()
        relojMs = 95_400
        vm.onParar()
        val guardado = dao.filas.value.single()
        assertEquals(95_400, guardado.duracionMs)
        assertEquals(LocalTime.of(9, 58, 24), guardado.hora) // 10:00:00 menos 1:35,4
        assertEquals(1, vm.uiState.value.intentos.size)
        assertEquals(95_400L, vm.uiState.value.mejorMs)
    }

    @Test
    fun unToqueDeMenosDeUnSegundoNoSeGuarda() {
        val vm = viewModel()
        vm.onIniciar()
        relojMs = 600
        vm.onParar()
        assertTrue(dao.filas.value.isEmpty())
    }

    @Test
    fun losIntentosSeNumeranDelMasViejoAlMasNuevo() {
        val vm = viewModel()
        repeat(2) { i ->
            vm.onIniciar()
            relojMs += 10_000L * (i + 1)
            vm.onParar()
        }
        assertEquals(listOf(2, 1), vm.uiState.value.intentos.map { it.numero })
    }
}
