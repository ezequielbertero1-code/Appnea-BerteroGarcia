package com.berterogarcia.appnea.data.local

import java.security.MessageDigest

fun hashearPassword(password: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

// Formatea segundos totales como mm:ss, para mostrar tiempos de fases y marcas de apnea
fun formatearDuracion(totalSegundos: Int): String {
    val minutos = totalSegundos / 60
    val segundos = totalSegundos % 60
    return "%02d:%02d".format(minutos, segundos)
}
