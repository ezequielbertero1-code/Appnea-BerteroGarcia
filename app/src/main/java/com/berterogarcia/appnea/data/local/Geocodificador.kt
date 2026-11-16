package com.berterogarcia.appnea.data.local

import com.berterogarcia.appnea.util.intentar
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
 * Coordenadas → nombre general de la zona ("Rada Tilly") con el Geocoder de Android.
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
                    intentar("Geocodificador") {
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
                    intentar("Geocodificador") { geocoder.getFromLocation(latitud, longitud, 1)?.firstOrNull() }.getOrNull()
                }
            }
        }
        return direccion?.let(::nombreDesde)
    }

    /**
     * Nombre GENERAL de la zona: la localidad ("Rada Tilly", "Comodoro Rivadavia").
     * No se usa la calle ni el número: para un punto de buceo no sirven.
     * Si el punto cae lejos de una localidad, se usa el departamento o la provincia.
     */
    private fun nombreDesde(a: Address): String? =
        listOf(a.locality, a.subAdminArea, a.adminArea)
            .firstOrNull { !it.isNullOrBlank() }
            ?.trim()
}