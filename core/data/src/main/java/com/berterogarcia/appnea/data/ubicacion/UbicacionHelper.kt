package com.berterogarcia.appnea.data.ubicacion

import com.berterogarcia.appnea.dominio.modelo.Ubicacion
import com.berterogarcia.appnea.util.intentar
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import com.berterogarcia.appnea.util.Registro
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Helper liviano para obtener la última ubicación conocida del dispositivo, sin depender
 * de Google Play Services: usa android.location.LocationManager directamente.
 *
 * Devuelve null si no hay permiso concedido o si ningún proveedor tiene una ubicación
 * conocida todavía (por ejemplo, si el GPS nunca se usó en el dispositivo).
 */
object UbicacionHelper {
    /** true si la ubicación del teléfono (GPS/red) está activada en los ajustes. */
    fun ubicacionActivada(context: Context): Boolean {
        val lm =
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return LocationManagerCompat.isLocationEnabled(lm)
    }

    fun tienePermiso(context: Context): Boolean {
        val fino =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val aproximado =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fino == PackageManager.PERMISSION_GRANTED || aproximado == PackageManager.PERMISSION_GRANTED
    }

    private const val TAG = "Ubicacion"

    /**
     * Pide una ubicación NUEVA a todos los proveedores activados a la vez (fused, gps, network)
     * y se queda con la primera que llegue. Si en [timeout] no responde ninguno, usa la
     * última conocida. Nunca lanza excepciones.
     */
    @SuppressLint("MissingPermission") // el permiso se verifica con tienePermiso()
    suspend fun obtenerUbicacionActual(context: Context, timeout: Duration = 30.seconds): Ubicacion? {
        if (!tienePermiso(context)) return null
        val lm =
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        val activados = intentar("UbicacionHelper") { lm.getProviders(true) }.getOrDefault(emptyList())
        // "passive" no genera ubicaciones propias, solo escucha las de otras apps
        val candidatos = activados.filter { it != LocationManager.PASSIVE_PROVIDER }
        Registro.d(TAG) { "activados=$activados → pide a $candidatos" }

        val actual: Location? = if (candidatos.isEmpty()) null else withTimeoutOrNull(timeout) {
            channelFlow {
                candidatos.forEach { proveedor ->
                    launch { send(pedirUbicacion(lm, proveedor, context)) }
                }
            }.filterNotNull().firstOrNull()   // al llegar la primera, se cancelan los demás pedidos
        }
        Registro.d(TAG) { "actual=$actual" }

        return actual?.let { Ubicacion(latitud = it.latitude, longitud = it.longitude) }
            ?: obtenerUltimaUbicacion(context)
    }

    /** Un pedido a un solo proveedor. Devuelve null si ese proveedor no pudo. */
    @SuppressLint("MissingPermission")
    private suspend fun pedirUbicacion(
        lm: LocationManager,
        proveedor: String,
        context: Context
    ): Location? =
        suspendCancellableCoroutine { cont ->
            val cancelar = CancellationSignal()
            cont.invokeOnCancellation { cancelar.cancel() }
            intentar("UbicacionHelper") {
                LocationManagerCompat.getCurrentLocation(
                    lm, proveedor, cancelar, ContextCompat.getMainExecutor(context)
                ) { location ->
                    Registro.d(TAG) { "$proveedor respondió: $location" }
                    if (cont.isActive) cont.resume(location)
                }
            }.onFailure {
                Registro.d(TAG) { "$proveedor falló: ${it.message}" }
                if (cont.isActive) cont.resume(null)
            }
        }

    /** Última ubicación guardada por CUALQUIER proveedor (incluye fused). */
    @SuppressLint("MissingPermission")
    fun obtenerUltimaUbicacion(context: Context): Ubicacion? {
        if (!tienePermiso(context)) return null
        val lm =
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        val proveedores = intentar("UbicacionHelper") { lm.allProviders }.getOrDefault(
            listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
        )
        val mejor = proveedores
            .mapNotNull { intentar("UbicacionHelper") { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }   // la más reciente

        Registro.d(TAG) { "ultima=$mejor" }
        return mejor?.let { Ubicacion(latitud = it.latitude, longitud = it.longitude) }
    }
}
