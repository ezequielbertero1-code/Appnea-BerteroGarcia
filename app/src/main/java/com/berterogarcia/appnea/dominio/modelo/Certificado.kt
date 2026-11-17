package com.berterogarcia.appnea.dominio.modelo

import java.time.LocalDate

/** Certificación / credencial del usuario (también el apto médico). */
data class Certificado(
    val id: Long = 0,
    val tipo: TipoCertificado,
    /** Nombre del nivel: "Buceador 2 Estrellas", "AIDA 2", "Apto médico"... */
    val titulo: String,
    /** Organización que certifica: CMAS, PADI, SSI, AIDA, Molchanovs... */
    val entidad: String = "",
    val escuelaInstructor: String = "",
    val numero: String = "",
    val fechaEmision: LocalDate? = null,
    /** null = no vence (la mayoría de las credenciales de buceo). */
    val fechaVencimiento: LocalDate? = null,
    val notas: String = ""
)
