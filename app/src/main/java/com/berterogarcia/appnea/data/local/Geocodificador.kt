package com.berterogarcia.appnea.data.local

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

/**
 * Coordenadas → nombre legible ("Punta Marqués, Rada Tilly") con el Geocoder de Android.
 * No usa API key. Devuelve null si no hay servicio de geocodificación en el teléfono,
 * si no hay internet o si tarda más de 5 s. Nunca lanza excepciones.
 */
object Geocodificador {

    @Suppress("DEPRECATION") // el getFromLocation sincrónico solo se usa antes de Android 13
    suspend fun nombrePara(context: Context, latitud: Double, longitud: Double): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.forLanguageTag("es-AR"))

        val direccion: Address? = withTimeoutOrNull(5.seconds) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+: versión asincrónica con listener
                suspendCancellableCoroutine { cont ->
                    runCatching {
                        geocoder.getFromLocation(latitud, longitud, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (cont.isActive) cont.resume(addresses.firstOrNull())
                            }
                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(null)
                            }
                        })
                    }.onFailure { if (cont.isActive) cont.resume(null) }
                }
            } else {
                // Android 7–12: versión bloqueante, por eso va en IO
                withContext(Dispatchers.IO) {
                    runCatching { geocoder.getFromLocation(latitud, longitud, 1)?.firstOrNull() }.getOrNull()
                }
            }
        }
        return direccion?.let(::nombreDesde)
    }

    /**
     * Arma "lugar, zona". featureName a veces es solo un número de calle o un Plus Code
     * ("8GQ4+2X"): en esos casos se usa la calle.
     */
    private fun nombreDesde(a: Address): String? {
        val feature = a.featureName?.trim()
            ?.takeUnless { it.isBlank() || it.any(Char::isDigit) || it.contains('+') }
        val principal = feature ?: a.thoroughfare
        val zona = a.subLocality ?: a.locality ?: a.subAdminArea ?: a.adminArea
        return listOfNotNull(principal, zona)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(", ")
            .ifBlank { null }
    }
}