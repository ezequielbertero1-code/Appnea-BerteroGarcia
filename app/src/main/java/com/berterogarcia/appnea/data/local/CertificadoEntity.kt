package com.berterogarcia.appnea.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Tipos de certificado. El apto médico se modela como un certificado más porque
 * comparte toodo: entidad que lo emite, fecha de emisión y, sobre todoo, VENCIMIENTO.
 * Se guarda por nombre (ver [PerfilConverters]); un valor desconocido se lee como [OTRO].
 */
enum class TipoCertificado(val etiqueta: String) {
    BUCEO("Buceo"),
    APNEA("Apnea"),
    PESCA_SUBMARINA("Pesca submarina"),
    PRIMEROS_AUXILIOS("Primeros auxilios / RCP"),
    APTO_MEDICO("Apto médico"),
    OTRO("Otro");

    companion object {
        fun desdeNombre(nombre: String?): TipoCertificado =
            entries.firstOrNull { it.name == nombre } ?: OTRO
    }
}

/**
 * Certificación / credencial del usuario (relación 1 a N con [UsuarioEntity]).
 */
@Entity(
    tableName = "certificados",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["nombreUsuario"],
            childColumns = ["nombreUsuario"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["nombreUsuario"])]
)
data class CertificadoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombreUsuario: String,
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
