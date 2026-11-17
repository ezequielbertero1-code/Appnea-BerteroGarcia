package com.berterogarcia.appnea.dominio.modelo

enum class TipoFase { PREPARACION, APNEA, DESCANSO }

data class Fase(
    val tipo: TipoFase,
    val duracionSegundos: Int
)
