package com.berterogarcia.appnea.data.remote

/**
 * Traduce el "weather code" (estándar WMO, el que usa Open-Meteo) a una descripción corta
 * en español. No cubre todos los códigos posibles, solo los más comunes.
 */
fun descripcionClima(codigo: Int): String = when (codigo) {
    0 -> "Despejado"
    1 -> "Mayormente despejado"
    2 -> "Parcialmente nublado"
    3 -> "Nublado"
    45, 48 -> "Niebla"
    51, 53, 55 -> "Llovizna"
    56, 57 -> "Llovizna helada"
    61, 63, 65 -> "Lluvia"
    66, 67 -> "Lluvia helada"
    71, 73, 75 -> "Nieve"
    77 -> "Granizo fino"
    80, 81, 82 -> "Chubascos"
    85, 86 -> "Chubascos de nieve"
    95 -> "Tormenta"
    96, 99 -> "Tormenta con granizo"
    else -> "Sin datos"
}

/** Emoji aproximado para el código WMO (de día / de noche). */
fun emojiClima(codigo: Int?, esDeDia: Boolean = true): String = when (val c = codigo ?: -1) {
    -1 -> "❔"
    0 -> if (esDeDia) "☀️" else "🌙"
    1 -> if (esDeDia) "🌤️" else "🌙"
    2 -> "⛅"
    3 -> "☁️"
    45, 48 -> "🌫️"
    in 51..57 -> "🌦️"
    in 61..67, in 80..82 -> "🌧️"
    in 71..77, 85, 86 -> "🌨️"
    in 95..99 -> "⛈️"
    else -> "🌡️"
}

/** Convierte grados (0-360) a un punto cardinal en español, para viento y oleaje. */
fun direccionCardinal(grados: Double): String {
    val puntos = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    val indice = (((grados % 360) + 360) % 360 / 45.0).let { Math.round(it).toInt() % 8 }
    return puntos[indice]
}
