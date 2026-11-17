package com.berterogarcia.appnea.dominio

import com.berterogarcia.appnea.dominio.modelo.Condiciones
import com.berterogarcia.appnea.dominio.modelo.CondicionesRegistro
import com.berterogarcia.appnea.dominio.modelo.Inmersion
import com.berterogarcia.appnea.dominio.modelo.Ubicacion
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
    /** Solo lo que se calcula sin internet (fase lunar): para guardar al instante. */
    fun registroAstronomico(latitud: Double, longitud: Double, momento: LocalDateTime): CondicionesRegistro
}

/** Pide (más tarde, cuando haya internet) el clima y el mar de una inmersión ya guardada. */
fun interface ProgramadorCondiciones {
    fun programar(idInmersion: Long, ubicacion: Ubicacion, momento: LocalDateTime)
}

/** Exportar una inmersión a un archivo que eligió el usuario (UDDF, el formato de las computadoras de buceo). */
interface ExportadorInmersiones {
    /**
     * @param destino dirección del archivo (la `Uri` que devolvió el selector de archivos, como texto).
     * @return true si se escribió bien.
     */
    suspend fun exportar(inmersion: Inmersion, destino: String): Boolean
}
