package com.berterogarcia.appnea.dominio

import com.berterogarcia.appnea.data.condiciones.Condiciones
import com.berterogarcia.appnea.data.local.CondicionesRegistro
import com.berterogarcia.appnea.data.ubicacion.Ubicacion
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/*
 * "Puertos": lo que la app necesita del mundo exterior (GPS, geocoder, API del clima), como
 * interfaces. Los ViewModels dependen de esto y no de objetos de Android, así se pueden testear
 * con implementaciones falsas y cambiar el proveedor (otra API de clima) sin tocar la UI.
 * Las implementaciones reales están en data/ (UbicacionAndroid, CondicionesRepository).
 */

/** GPS del teléfono + nombre del lugar (geocoder). */
interface ServicioUbicacion {
    fun tienePermiso(): Boolean
    fun ubicacionActivada(): Boolean
    suspend fun actual(timeout: Duration = 30.seconds): Ubicacion?
    /** Última ubicación conocida (instantánea, puede ser vieja o null). */
    fun ultima(): Ubicacion?
    /** "Rada Tilly, Chubut" para unas coordenadas, o null si no hay geocoder o red. */
    suspend fun nombreDelLugar(latitud: Double, longitud: Double): String?
}

/** Clima, mar y astronomía. */
interface FuenteCondiciones {
    /** Condiciones actuales y pronóstico para la pantalla de Clima. */
    suspend fun obtener(ubicacion: Ubicacion): Result<Condiciones>
    /** Foto de las condiciones de un momento, para guardar con una inmersión. */
    suspend fun registroPara(latitud: Double, longitud: Double, momento: LocalDateTime): CondicionesRegistro
}
