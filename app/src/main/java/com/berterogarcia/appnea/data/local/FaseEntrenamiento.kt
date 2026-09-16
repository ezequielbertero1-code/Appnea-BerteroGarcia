package com.berterogarcia.appnea.data.local

enum class TipoFase { PREPARACION, APNEA, DESCANSO }

data class Fase(
    val tipo: TipoFase,
    val duracionSegundos: Int
)
