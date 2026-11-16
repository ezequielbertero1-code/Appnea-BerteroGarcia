package com.berterogarcia.appnea.data.ubicacion

import android.content.Context
import com.berterogarcia.appnea.dominio.ServicioUbicacion
import kotlin.time.Duration

/** Implementación real de [ServicioUbicacion] (LocationManager + Geocoder de Android). */
class UbicacionAndroid(private val context: Context) : ServicioUbicacion {
    override fun tienePermiso() = UbicacionHelper.tienePermiso(context)
    override fun ubicacionActivada() = UbicacionHelper.ubicacionActivada(context)
    override suspend fun actual(timeout: Duration) = UbicacionHelper.obtenerUbicacionActual(context, timeout)
    override fun ultima() = UbicacionHelper.obtenerUltimaUbicacion(context)
    override suspend fun nombreDelLugar(latitud: Double, longitud: Double) =
        Geocodificador.nombrePara(context, latitud, longitud)
}
