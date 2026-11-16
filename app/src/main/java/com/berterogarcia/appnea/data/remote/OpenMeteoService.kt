package com.berterogarcia.appnea.data.remote

import com.berterogarcia.appnea.util.intentar
import com.berterogarcia.appnea.BuildConfig
import com.berterogarcia.appnea.data.condiciones.DiaClima
import com.berterogarcia.appnea.data.condiciones.HoraClima
import com.berterogarcia.appnea.data.condiciones.HoraMar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale

/** Pronóstico atmosférico horario + diario, con la zona horaria del lugar. */
data class PronosticoClima(
    val zona: ZoneOffset,
    val horas: List<HoraClima>,
    val dias: List<DiaClima>
)

/**
 * Cliente muy liviano para las APIs de Open-Meteo (https://open-meteo.com/): son gratuitas,
 * no requieren API key y tienen pronóstico atmosférico y marino (olas, mar de fondo,
 * nivel del mar con marea, temperatura del agua y corrientes).
 *
 * No se usa ninguna librería de red externa (Retrofit/OkHttp): alcanza con
 * HttpURLConnection + org.json, que ya vienen con Android.
 */
object OpenMeteoService {

    private const val TIMEOUT_MS = 10_000
    private const val USER_AGENT = "AppneaMe/${BuildConfig.VERSION_NAME} (Android)"

    private const val VARIABLES_HORA_CLIMA =
        "temperature_2m,apparent_temperature,relative_humidity_2m,dew_point_2m," +
            "precipitation_probability,precipitation,weather_code,pressure_msl,cloud_cover," +
            "visibility,wind_speed_10m,wind_direction_10m,wind_gusts_10m,uv_index,is_day"

    private const val VARIABLES_DIA_CLIMA =
        "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum," +
            "precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max," +
            "wind_direction_10m_dominant,uv_index_max"

    private const val VARIABLES_HORA_MAR =
        "wave_height,wave_direction,wave_period,wind_wave_height,swell_wave_height," +
            "swell_wave_direction,swell_wave_period,sea_level_height_msl,sea_surface_temperature," +
            "ocean_current_velocity,ocean_current_direction"

    /**
     * Si [fecha] es null trae el día anterior + 7 días de pronóstico (para tendencias y mareas).
     * Si se pasa una fecha, trae solo ese día (sirve para guardar las condiciones de un registro).
     */
    private fun rango(fecha: LocalDate?): String =
        if (fecha == null) "&past_days=1&forecast_days=7"
        else "&start_date=$fecha&end_date=$fecha"

    suspend fun obtenerPronostico(latitud: Double, longitud: Double, fecha: LocalDate? = null): Result<PronosticoClima> =
        withContext(Dispatchers.IO) {
            intentar("OpenMeteoService") {
                val url = String.format(
                    Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
                        "&hourly=$VARIABLES_HORA_CLIMA&daily=$VARIABLES_DIA_CLIMA&timezone=auto",
                    latitud, longitud
                ) + rango(fecha)
                val json = obtenerJson(url)
                val zona = ZoneOffset.ofTotalSeconds(json.optInt("utc_offset_seconds", 0))

                val h = json.getJSONObject("hourly")
                val tiempos = h.getJSONArray("time")
                val horas = (0 until tiempos.length()).map { i ->
                    HoraClima(
                        hora = LocalDateTime.parse(tiempos.getString(i)),
                        temperaturaC = h.num("temperature_2m", i),
                        sensacionTermicaC = h.num("apparent_temperature", i),
                        humedadPorc = h.num("relative_humidity_2m", i)?.toInt(),
                        puntoRocioC = h.num("dew_point_2m", i),
                        probabilidadLluviaPorc = h.num("precipitation_probability", i)?.toInt(),
                        precipitacionMm = h.num("precipitation", i),
                        codigoClima = h.num("weather_code", i)?.toInt(),
                        presionHpa = h.num("pressure_msl", i),
                        nubosidadPorc = h.num("cloud_cover", i)?.toInt(),
                        visibilidadM = h.num("visibility", i),
                        vientoKmh = h.num("wind_speed_10m", i),
                        direccionVientoGrados = h.num("wind_direction_10m", i),
                        rafagasKmh = h.num("wind_gusts_10m", i),
                        indiceUv = h.num("uv_index", i),
                        esDeDia = h.num("is_day", i)?.let { it >= 1.0 }
                    )
                }

                val d = json.getJSONObject("daily")
                val fechas = d.getJSONArray("time")
                val dias = (0 until fechas.length()).map { i ->
                    DiaClima(
                        fecha = LocalDate.parse(fechas.getString(i)),
                        codigoClima = d.num("weather_code", i)?.toInt(),
                        maximaC = d.num("temperature_2m_max", i),
                        minimaC = d.num("temperature_2m_min", i),
                        lluviaMm = d.num("precipitation_sum", i),
                        probabilidadLluviaPorc = d.num("precipitation_probability_max", i)?.toInt(),
                        vientoMaxKmh = d.num("wind_speed_10m_max", i),
                        rafagasMaxKmh = d.num("wind_gusts_10m_max", i),
                        direccionVientoGrados = d.num("wind_direction_10m_dominant", i),
                        uvMax = d.num("uv_index_max", i),
                        olaMaxM = null
                    )
                }
                PronosticoClima(zona, horas, dias)
            }
        }

    suspend fun obtenerMar(latitud: Double, longitud: Double, fecha: LocalDate? = null): Result<List<HoraMar>> =
        withContext(Dispatchers.IO) {
            intentar("OpenMeteoService") {
                val url = String.format(
                    Locale.US,
                    "https://marine-api.open-meteo.com/v1/marine?latitude=%.4f&longitude=%.4f" +
                        "&hourly=$VARIABLES_HORA_MAR&timezone=auto",
                    latitud, longitud
                ) + rango(fecha)
                val json = obtenerJson(url)
                val h = json.getJSONObject("hourly")
                val tiempos = h.getJSONArray("time")
                (0 until tiempos.length()).map { i ->
                    HoraMar(
                        hora = LocalDateTime.parse(tiempos.getString(i)),
                        olaM = h.num("wave_height", i),
                        direccionOlaGrados = h.num("wave_direction", i),
                        periodoOlaS = h.num("wave_period", i),
                        olaVientoM = h.num("wind_wave_height", i),
                        marDeFondoM = h.num("swell_wave_height", i),
                        direccionMarDeFondoGrados = h.num("swell_wave_direction", i),
                        periodoMarDeFondoS = h.num("swell_wave_period", i),
                        nivelMarM = h.num("sea_level_height_msl", i),
                        temperaturaAguaC = h.num("sea_surface_temperature", i),
                        corrienteKmh = h.num("ocean_current_velocity", i),
                        direccionCorrienteGrados = h.num("ocean_current_direction", i)
                    )
                }.also { horas ->
                    // Si el punto está lejos del mar, Open-Meteo devuelve todo en null
                    if (horas.none { it.olaM != null || it.nivelMarM != null || it.temperaturaAguaC != null }) {
                        throw IllegalStateException("Sin datos marinos para este punto")
                    }
                }
            }
        }

    /** Valor numérico de la serie [nombre] en la posición [i], o null si no viene o es null. */
    private fun JSONObject.num(nombre: String, i: Int): Double? {
        val arreglo: JSONArray = optJSONArray(nombre) ?: return null
        if (i >= arreglo.length() || arreglo.isNull(i)) return null
        return arreglo.optDouble(i).takeIf { !it.isNaN() }
    }

    private fun obtenerJson(urlStr: String): JSONObject {
        val conexion = URL(urlStr).openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = "GET"
            // Sin esto Android manda "Dalvik/2.1.0 (...)", el mismo que miles de apps: Open-Meteo bloquea
            // ese User-Agent en algunos pedidos (403 de nginx si se pide pressure_msl). Con uno propio responde bien.
            conexion.setRequestProperty("User-Agent", USER_AGENT)
            conexion.connectTimeout = TIMEOUT_MS
            conexion.readTimeout = TIMEOUT_MS
            val codigo = conexion.responseCode
            if (codigo !in 200..299) {
                // Open-Meteo explica el motivo en el cuerpo ({"error":true,"reason":"..."}): va al mensaje para el log
                val detalle = intentar("OpenMeteoService") { conexion.errorStream?.bufferedReader()?.use { it.readText() } }.getOrNull()
                throw RuntimeException("Open-Meteo respondió con código $codigo: ${detalle?.take(500)} | URL: $urlStr")
            }
            val texto = conexion.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(texto)
        } finally {
            conexion.disconnect()
        }
    }
}
