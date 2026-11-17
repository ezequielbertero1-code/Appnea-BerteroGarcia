package com.berterogarcia.appnea.ui

import com.berterogarcia.appnea.data.local.IntentoApneaDao
import com.berterogarcia.appnea.data.local.IntentoApneaEntity
import com.berterogarcia.appnea.data.repository.IntentoApneaRepository
import com.berterogarcia.appnea.data.sesion.ProveedorUsuario
import com.berterogarcia.appnea.ui.entrenamiento.CronometroViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

/** DAO en memoria: suficiente para probar el ViewModel sin Room. */
private class IntentosEnMemoria : IntentoApneaDao {
    val filas = MutableStateFlow<List<IntentoApneaEntity>>(emptyList())
    override suspend fun insertar(intento: IntentoApneaEntity): Long {
        filas.value = listOf(intento.copy(id = filas.value.size + 1L)) + filas.value
        return filas.value.size.toLong()
    }
    override fun obtenerTodos(nombreUsuario: String): Flow<List<IntentoApneaEntity>> =
        filas.map { l -> l.filter { it.nombreUsuario == nombreUsuario } }
    override fun obtenerDelDia(nombreUsuario: String, fecha: LocalDate): Flow<List<IntentoApneaEntity>> =
        filas.map { l -> l.filter { it.nombreUsuario == nombreUsuario && it.fecha == fecha } }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CronometroViewModelTest {

    private val dao = IntentosEnMemoria()
    private val sesion = object : ProveedorUsuario {
        override val usuarioActual: StateFlow<String?> = MutableStateFlow("ana")
    }
    private var relojMs = 0L
    private val fin = LocalDateTime.of(2026, 11, 16, 10, 0, 0)

    @Before fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun despues() = Dispatchers.resetMain()

    private fun viewModel() = CronometroViewModel(IntentoApneaRepository(dao, sesion), reloj = { relojMs }, ahora = { fin })

    @Test
    fun pararGuardaElIntentoConSuHoraDeInicio() {
        val vm = viewModel()
        vm.onIniciar()
        relojMs = 95_400
        vm.onParar()
        val guardado = dao.filas.value.single()
        assertEquals("ana", guardado.nombreUsuario)
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
