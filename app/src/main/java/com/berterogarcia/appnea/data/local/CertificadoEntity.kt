package com.berterogarcia.appnea.data.local

import com.berterogarcia.appnea.dominio.modelo.TipoCertificado
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

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
