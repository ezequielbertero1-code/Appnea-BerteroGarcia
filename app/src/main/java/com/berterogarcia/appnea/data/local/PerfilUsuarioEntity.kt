package com.berterogarcia.appnea.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Datos personales del usuario.
 *
 * Relación 1 a 1 con [UsuarioEntity]: la PK es el mismo `nombreUsuario`, así es
 * imposible que un usuario tenga dos perfiles. Si se borra el usuario, se borra
 * su perfil (CASCADE).
 *
 * Se separa de `usuarios` a propósito: la tabla de usuarios queda solo para
 * autenticación (usuario + hash) y el perfil puede crecer sin tocar el login.
 */
@Entity(
    tableName = "perfiles_usuario",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["nombreUsuario"],
            childColumns = ["nombreUsuario"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class PerfilUsuarioEntity(
    @PrimaryKey val nombreUsuario: String,
    val nombre: String,
    val apellido: String = "",
    val email: String = "",
    val telefono: String = "",
    val fechaNacimiento: LocalDate? = null,
    val ciudad: String = "",
    val companeroHabitual: String = "",
    /** Epoch millis de la última modificación (útil para mostrar "actualizado el..."). */
    val actualizadoEn: Long = System.currentTimeMillis()
)

// Extensión (y no propiedad dentro de la entidad) para que Room no intente mapearla a una columna.
val PerfilUsuarioEntity.nombreCompleto: String
    get() = listOf(nombre, apellido).filter { it.isNotBlank() }.joinToString(" ")
