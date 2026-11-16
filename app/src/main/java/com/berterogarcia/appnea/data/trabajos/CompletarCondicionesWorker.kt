package com.berterogarcia.appnea.data.trabajos

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.berterogarcia.appnea.MyApplication
import com.berterogarcia.appnea.data.condiciones.CondicionesRepository
import com.berterogarcia.appnea.data.local.Ubicacion
import com.berterogarcia.appnea.dominio.ProgramadorCondiciones
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Completa clima y mar de una inmersión ya guardada. WorkManager lo corre cuando hay internet
 * (aunque se haya cerrado la app) y lo reintenta con espera creciente si Open-Meteo no responde.
 */
class CompletarCondicionesWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(CLAVE_ID, -1L).takeIf { it > 0 } ?: return Result.failure()
        val momento = inputData.getString(CLAVE_MOMENTO)?.let(LocalDateTime::parse) ?: return Result.failure()
        val lat = inputData.getDouble(CLAVE_LAT, Double.NaN)
        val lon = inputData.getDouble(CLAVE_LON, Double.NaN)
        if (lat.isNaN() || lon.isNaN()) return Result.failure()

        val registro = CondicionesRepository.registroPara(lat, lon, momento)
        if (!registro.conDatosOnline) {
            // Sin datos (red caída a mitad de camino, error del servidor...): se reintenta unas veces
            return if (runAttemptCount < MAX_INTENTOS) Result.retry() else Result.success()
        }
        val dao = (applicationContext as MyApplication).database.inmersionDao()
        dao.actualizarCondiciones(id, registro)
        return Result.success()
    }

    companion object {
        private const val CLAVE_ID = "id"
        private const val CLAVE_MOMENTO = "momento"
        private const val CLAVE_LAT = "lat"
        private const val CLAVE_LON = "lon"
        private const val MAX_INTENTOS = 5
    }

    /** Implementación real de [ProgramadorCondiciones]: encola el trabajo (uno por inmersión). */
    class Programador(private val context: Context) : ProgramadorCondiciones {
        override fun programar(idInmersion: Long, ubicacion: Ubicacion, momento: LocalDateTime) {
            val pedido = OneTimeWorkRequestBuilder<CompletarCondicionesWorker>()
                .setInputData(
                    workDataOf(
                        CLAVE_ID to idInmersion,
                        CLAVE_MOMENTO to momento.toString(),
                        CLAVE_LAT to ubicacion.latitud,
                        CLAVE_LON to ubicacion.longitud
                    )
                )
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            // Si se edita dos veces seguidas, gana el último pedido (REPLACE)
            WorkManager.getInstance(context)
                .enqueueUniqueWork("condiciones-$idInmersion", ExistingWorkPolicy.REPLACE, pedido)
        }
    }
}
