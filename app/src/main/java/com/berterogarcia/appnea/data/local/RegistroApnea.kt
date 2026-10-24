package com.berterogarcia.appnea.data.local

import org.json.JSONArray
import org.json.JSONObject

/** Una bajada individual de una inmersión de apnea. */
data class BajadaApnea(
    val profundidadMetros: Float,
    val tiempoSegundos: Int
)

/**
 * Datos de apnea de una inmersión (se guardan como JSON en una columna, ver Converters).
 *
 * El usuario siempre carga [cantidadBajadas] y [profundidadMaximaMetros]; el detalle de cada
 * bajada ([bajadas]) es opcional. Cuando hay detalle, el resumen se calcula desde él.
 */
data class RegistroApnea(
    val cantidadBajadas: Int,
    val profundidadMaximaMetros: Float,
    val sincope: Boolean = false,
    val bajadas: List<BajadaApnea> = emptyList()
) {
    /** Se calcula siempre, nunca se guarda: así no puede quedar inconsistente. */
    fun resumen(): ResumenApnea {
        if (bajadas.isEmpty()) {
            return ResumenApnea(cantidadBajadas, profundidadMaximaMetros, null, null, null, null)
        }
        val indiceMejor = bajadas.indices.maxWith(compareBy({ bajadas[it].profundidadMetros }, { -bajadas[it].tiempoSegundos }))
        val masProfunda = bajadas[indiceMejor]
        val total = bajadas.sumOf { it.tiempoSegundos }
        return ResumenApnea(
            cantidad = bajadas.size,
            profundidadMaximaMetros = masProfunda.profundidadMetros,
            tiempoMejorBajadaSegundos = masProfunda.tiempoSegundos,
            tiempoTotalSegundos = total,
            tiempoPromedioSegundos = total / bajadas.size,
            indiceMejorBajada = indiceMejor
        )
    }

    fun aJson(): String = JSONObject().apply {
        put("cantidadBajadas", cantidadBajadas)
        put("profundidadMaximaMetros", profundidadMaximaMetros.toDouble())
        put("sincope", sincope)
        put("bajadas", JSONArray().apply {
            bajadas.forEach {
                put(JSONObject().apply {
                    put("profundidadMetros", it.profundidadMetros.toDouble())
                    put("tiempoSegundos", it.tiempoSegundos)
                })
            }
        })
    }.toString()

    companion object {
        fun desdeJson(texto: String): RegistroApnea? = runCatching {
            val j = JSONObject(texto)
            val lista = j.optJSONArray("bajadas") ?: JSONArray()
            RegistroApnea(
                cantidadBajadas = j.getInt("cantidadBajadas"),
                profundidadMaximaMetros = j.getDouble("profundidadMaximaMetros").toFloat(),
                sincope = j.optBoolean("sincope", false),
                bajadas = List(lista.length()) { n ->
                    val b = lista.getJSONObject(n)
                    BajadaApnea(b.getDouble("profundidadMetros").toFloat(), b.getInt("tiempoSegundos"))
                }
            )
        }.getOrNull()
    }
}

/**
 * Resumen de una inmersión de apnea. Los tiempos son null cuando el usuario no cargó
 * el detalle de las bajadas.
 */
data class ResumenApnea(
    val cantidad: Int,
    val profundidadMaximaMetros: Float,
    val tiempoMejorBajadaSegundos: Int?,
    val tiempoTotalSegundos: Int?,
    val tiempoPromedioSegundos: Int?,
    /** Posición (desde 0) de la bajada más profunda; null sin detalle. */
    val indiceMejorBajada: Int?
)
