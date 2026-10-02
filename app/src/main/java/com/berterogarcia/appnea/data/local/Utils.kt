package com.berterogarcia.appnea.data.local

// Se eliminó hashearPassword (SHA-256 sin salt). Ahora se usa SeguridadPassword.hashear.

// Formatea segundos totales como mm:ss, para mostrar tiempos de fases y marcas de apnea
fun formatearDuracion(totalSegundos: Int): String {
    val minutos = totalSegundos / 60
    val segundos = totalSegundos % 60
    return "%02d:%02d".format(minutos, segundos)
}