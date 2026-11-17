package com.berterogarcia.appnea

import com.berterogarcia.appnea.fakes.FakeCondiciones
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.berterogarcia.appnea.data.trabajos.CompletarCondicionesWorker
import com.berterogarcia.appnea.dominio.modelo.CondicionesRegistro
import com.berterogarcia.appnea.dominio.modelo.Ubicacion
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/** El worker con dependencias falsas: no usa la red ni la base real del teléfono. */
@RunWith(AndroidJUnit4::class)
class CompletarCondicionesWorkerTest {

    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val condiciones = FakeCondiciones()
    private val guardados = mutableListOf<Pair<Long, CondicionesRegistro>>()

    private val fabrica = object : WorkerFactory() {
        override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
            CompletarCondicionesWorker(appContext, workerParameters, condiciones) { id, r -> guardados += id to r }
    }

    private fun worker(intento: Int = 0, datos: androidx.work.Data =
        CompletarCondicionesWorker.datos(7, Ubicacion(-45.86, -67.48), LocalDateTime.of(2026, 11, 16, 10, 0))
    ) = TestListenableWorkerBuilder<CompletarCondicionesWorker>(contexto)
        .setWorkerFactory(fabrica)
        .setInputData(datos)
        .setRunAttemptCount(intento)
        .build()

    @Test
    fun conInternetGuardaElClimaEnLaInmersion() = runBlocking {
        condiciones.conDatos = true
        val resultado = worker().doWork()
        assertEquals(ListenableWorker.Result.success(), resultado)
        assertEquals(7L, guardados.single().first)
        assertTrue(guardados.single().second.conDatosOnline)
    }

    @Test
    fun sinDatosReintentaYDespuesSeRinde() = runBlocking {
        condiciones.conDatos = false
        assertEquals(ListenableWorker.Result.retry(), worker(intento = 0).doWork())
        assertEquals(ListenableWorker.Result.success(), worker(intento = CompletarCondicionesWorker.MAX_INTENTOS).doWork())
        assertTrue("sin datos no pisa lo que ya había", guardados.isEmpty())
    }

    @Test
    fun datosIncompletosFallanSinReintentar() = runBlocking {
        assertEquals(ListenableWorker.Result.failure(), worker(datos = workDataOf("id" to 7L)).doWork())
    }
}
