package com.berterogarcia.appnea.dominio.modelo

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
