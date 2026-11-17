package com.berterogarcia.appnea.dominio.modelo

import java.time.LocalDate

/** Datos personales del usuario. */
data class PerfilUsuario(
    val nombre: String,
    val apellido: String = "",
    val email: String = "",
    val telefono: String = "",
    val fechaNacimiento: LocalDate? = null,
    val ciudad: String = "",
    val companeroHabitual: String = "",
    /** Epoch millis de la última modificación. */
    val actualizadoEn: Long = System.currentTimeMillis()
) {
    val nombreCompleto: String
        get() = listOf(nombre, apellido).filter { it.isNotBlank() }.joinToString(" ")
}
