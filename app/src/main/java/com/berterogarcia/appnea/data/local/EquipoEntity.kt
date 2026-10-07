package com.berterogarcia.appnea.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Categorías de equipamiento. Se guardan en la base por NOMBRE (ver [PerfilConverters]),
 * así que se pueden reordenar o cambiar la etiqueta sin romper datos. Si algún día se
 * renombra/borra una constante, los registros viejos se leen como [OTRO] en vez de crashear.
 */
enum class CategoriaEquipo(val etiqueta: String) {
    MASCARA("Máscara"),
    SNORKEL("Snorkel"),
    ALETAS("Aletas"),
    TRAJE("Traje"),
    GUANTES_ESCARPINES("Guantes / escarpines"),
    LASTRE("Lastre / cinturón"),
    COMPUTADORA("Computadora / reloj"),
    REGULADOR("Regulador"),
    CHALECO("Chaleco (BCD)"),
    TUBO("Tubo"),
    BOYA("Boya"),
    CUCHILLO("Cuchillo"),
    LINTERNA("Linterna"),
    OTRO("Otro");

    companion object {
        fun desdeNombre(nombre: String?): CategoriaEquipo =
            entries.firstOrNull { it.name == nombre } ?: OTRO
    }
}

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
