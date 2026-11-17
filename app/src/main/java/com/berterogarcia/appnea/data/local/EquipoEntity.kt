package com.berterogarcia.appnea.data.local

import com.berterogarcia.appnea.dominio.modelo.CategoriaEquipo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Un ítem del equipamiento del usuario (relación 1 a N con [UsuarioEntity]).
 */
@Entity(
    tableName = "equipamiento",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["nombreUsuario"],
            childColumns = ["nombreUsuario"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    // Índice sobre la FK: todas las consultas filtran por usuario y Room avisa si falta.
    indices = [Index(value = ["nombreUsuario"])]
)
data class EquipoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombreUsuario: String,
    val categoria: CategoriaEquipo,
    val marca: String = "",
    val modelo: String = "",
    /** Talle / medida / espesor, texto libre: "L", "42-43", "5 mm", "3 kg"... */
    val talle: String = "",
    val fechaCompra: LocalDate? = null,
    /** Próximo service / revisión (regulador, computadora, etc.). */
    val proximoMantenimiento: LocalDate? = null,
    /** false = retirado / vendido: se conserva el historial pero no se cuenta como "en uso". */
    val enUso: Boolean = true,
    val notas: String = ""
)

/** Texto corto para listas: "Aletas · Cressi Gara 3000". Extensión para que Room no la tome como columna. */
val EquipoEntity.descripcion: String
    get() {
        val marcaModelo = listOf(marca, modelo).filter { it.isNotBlank() }.joinToString(" ")
        return if (marcaModelo.isBlank()) categoria.etiqueta else "${categoria.etiqueta} · $marcaModelo"
    }
