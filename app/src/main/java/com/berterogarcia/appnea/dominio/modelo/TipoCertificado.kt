package com.berterogarcia.appnea.dominio.modelo

/**
 * Tipos de certificado. El apto médico se modela como un certificado más porque
 * comparte toodo: entidad que lo emite, fecha de emisión y, sobre todoo, VENCIMIENTO.
 * Se guarda por nombre (ver [PerfilConverters]); un valor desconocido se lee como [OTRO].
 */
enum class TipoCertificado(val etiqueta: String) {
    BUCEO("Buceo"),
    APNEA("Apnea"),
    PRIMEROS_AUXILIOS("Primeros auxilios / RCP"),
    APTO_MEDICO("Apto médico"),
    OTRO("Otro");

    companion object {
        fun desdeNombre(nombre: String?): TipoCertificado =
            entries.firstOrNull { it.name == nombre } ?: OTRO
    }
}
