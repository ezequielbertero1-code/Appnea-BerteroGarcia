package com.berterogarcia.appnea.dominio.modelo

import java.time.LocalDate

/** Un elemento del equipamiento del usuario. */
data class Equipo(
    val id: Long = 0,
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
) {
    /** Texto corto para listas: "Aletas · Cressi Gara 3000". */
    val descripcion: String
        get() {
            val marcaModelo = listOf(marca, modelo).filter { it.isNotBlank() }.joinToString(" ")
            return if (marcaModelo.isBlank()) categoria.etiqueta else "${categoria.etiqueta} · $marcaModelo"
        }
}
