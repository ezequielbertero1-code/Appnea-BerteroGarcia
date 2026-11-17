package com.berterogarcia.appnea.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Contacto de emergencia (relación 1 a N con [UsuarioEntity]).
 *
 * Se permite más de uno, pero SIEMPRE hay exactamente un principal si existe al menos
 * un contacto. Esa regla la mantiene [ContactoEmergenciaDao] dentro de transacciones.
 */
@Entity(
    tableName = "contactos_emergencia",
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
data class ContactoEmergenciaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombreUsuario: String,
    val nombre: String,
    /** Hermana, pareja, compañero de buceo... */
    val relacion: String = "",
    val telefono: String,
    val telefonoAlternativo: String = "",
    val esPrincipal: Boolean = false,
    /** Info útil para quien atienda la emergencia (obra social, aclaraciones, etc.). */
    val notas: String = ""
)
