package com.berterogarcia.appnea.dominio.modelo

/** Contacto a quien avisar en una emergencia. */
data class ContactoEmergencia(
    val id: Long = 0,
    val nombre: String,
    /** Hermana, pareja, compañero de buceo... */
    val relacion: String = "",
    val telefono: String,
    val telefonoAlternativo: String = "",
    val esPrincipal: Boolean = false,
    /** Info útil para quien atienda la emergencia (obra social, aclaraciones, etc.). */
    val notas: String = ""
)
