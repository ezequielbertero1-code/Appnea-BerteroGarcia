package com.berterogarcia.appnea.data.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

/**
 * Resultado simple de una ubicación: latitud, longitud y (opcionalmente) un nombre legible.
 */
data class Ubicacion(
    val latitud: Double,
    val longitud: Double,
    val nombre: String? = null
)

/** Comodoro Rivadavia, Chubut. Se usa como ubicación por defecto para el Clima. */
val UBICACION_COMODORO_RIVADAVIA = Ubicacion(
    latitud = -45.8641,
    longitud = -67.4966,
    nombre = "Comodoro Rivadavia"
)

/**
 * Helper liviano para obtener la última ubicación conocida del dispositivo, sin depender
 * de Google Play Services: usa android.location.LocationManager directamente.
 *
 * Devuelve null si no hay permiso concedido o si ningún proveedor tiene una ubicación
 * conocida todavía (por ejemplo, si el GPS nunca se usó en el dispositivo).
 */
object UbicacionHelper {

    fun tienePermiso(context: Context): Boolean {
        val fino = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val aproximado = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fino == PackageManager.PERMISSION_GRANTED || aproximado == PackageManager.PERMISSION_GRANTED
    }

    fun obtenerUltimaUbicacion(context: Context): Ubicacion? {
        if (!tienePermiso(context)) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val proveedores = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        var mejorUbicacion: Location? = null
        for (proveedor in proveedores) {
            val ubicacion = runCatching { locationManager.getLastKnownLocation(proveedor) }.getOrNull()
                ?: continue
            if (mejorUbicacion == null || ubicacion.time > mejorUbicacion!!.time) {
                mejorUbicacion = ubicacion
            }
        }

        return mejorUbicacion?.let { Ubicacion(latitud = it.latitude, longitud = it.longitude) }
    }
}
