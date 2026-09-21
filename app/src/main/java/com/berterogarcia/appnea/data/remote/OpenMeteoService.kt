package com.berterogarcia.appnea.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Cliente muy liviano para la API de Open-Meteo (https://open-meteo.com/): es gratuita,
 * no requiere API key y tiene tanto pronóstico de clima "normal" como un endpoint marino
 * (altura/dirección/período de olas), ideal para planificar una salida de pesca submarina.
 *
 * No se usa ninguna librería de red externa (Retrofit/OkHttp): alcanza con
 * HttpURLConnection + org.json, que ya vienen con Android.
 */
object OpenMeteoService {

    private const val TIMEOUT_MS = 10_000

    suspend fun obtenerClimaActual(latitud: Double, longitud: Double): Result<ClimaActual> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = String.format(
                    Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
                        "&current=temperature_2m,apparent_temperature,relative_humidity_2m," +
                        "wind_speed_10m,wind_direction_10m,weather_code&timezone=auto",
                    latitud, longitud
                )
                val json = obtenerJson(url)
                val actual = json.getJSONObject("current")
                ClimaActual(
                    temperaturaC = actual.getDouble("temperature_2m"),
                    sensacionTermicaC = actual.optDouble("apparent_temperature").takeIf { !it.isNaN() },
                    humedadPorc = actual.optInt("relative_humidity_2m", -1).takeIf { it >= 0 },
                    vientoKmh = actual.getDouble("wind_speed_10m"),
                    direccionVientoGrados = actual.getDouble("wind_direction_10m"),
                    codigoClima = actual.getInt("weather_code")
                )
            }
        }

    suspend fun obtenerMarActual(latitud: Double, longitud: Double): Result<MarActual> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = String.format(
                    Locale.US,
                    "https://marine-api.open-meteo.com/v1/marine?latitude=%.4f&longitude=%.4f" +
                        "&current=wave_height,wave_direction,wave_period&timezone=auto",
                    latitud, longitud
                )
                val json = obtenerJson(url)
                val actual = json.getJSONObject("current")
                MarActual(
                    alturaOlaM = actual.getDouble("wave_height"),
                    direccionOlaGrados = actual.optDouble("wave_direction").takeIf { !it.isNaN() },
                    periodoOlaS = actual.optDouble("wave_period").takeIf { !it.isNaN() }
                )
            }
        }

    private fun obtenerJson(urlStr: String): JSONObject {
        val conexion = URL(urlStr).openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = "GET"
            conexion.connectTimeout = TIMEOUT_MS
            conexion.readTimeout = TIMEOUT_MS
            val codigo = conexion.responseCode
            if (codigo !in 200..299) {
                throw RuntimeException("Open-Meteo respondió con código $codigo")
            }
            val texto = conexion.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(texto)
        } finally {
            conexion.disconnect()
        }
    }
}
