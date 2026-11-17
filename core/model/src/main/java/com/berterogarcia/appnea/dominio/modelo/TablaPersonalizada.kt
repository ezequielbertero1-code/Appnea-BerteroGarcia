package com.berterogarcia.appnea.dominio.modelo

/** Una tabla de entrenamiento creada por el usuario. */
data class TablaPersonalizada(
    val id: Long = 0,
    val nombre: String,
    val fases: List<Fase>
) {
    fun aTablaApnea(): TablaApnea = TablaApnea(nombre, fases)
}
