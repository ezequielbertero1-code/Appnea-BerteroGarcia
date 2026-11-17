package com.berterogarcia.appnea.dominio.modelo

import com.berterogarcia.appnea.util.intentar
import org.json.JSONObject

/**
 * "Foto" de las condiciones en el momento de una inmersión.
 * Se guarda automáticamente al registrar (como JSON en una columna, ver Converters).
 *
 * Lo astronómico (luna) se calcula siempre, aunque no haya internet.
 * Lo meteorológico/marino viene de Open-Meteo y puede faltar ([conDatosOnline] = false).
 */
data class CondicionesRegistro(
    val temperaturaC: Double? = null,
    val sensacionTermicaC: Double? = null,
    val humedadPorc: Int? = null,
    val presionHpa: Double? = null,
    val vientoKmh: Double? = null,
    val direccionVientoGrados: Double? = null,
    val rafagasKmh: Double? = null,
    val codigoClima: Int? = null,
    val nubosidadPorc: Int? = null,
    val olaM: Double? = null,
    val periodoOlaS: Double? = null,
    val direccionOlaGrados: Double? = null,
    val marDeFondoM: Double? = null,
    val temperaturaAguaC: Double? = null,
    val corrienteKmh: Double? = null,
    val alturaMareaM: Double? = null,
    val estadoMarea: String? = null,
    val faseLunar: String,
    val iluminacionLunaPorc: Int,
    val aptitudAgua: String? = null,
    val conDatosOnline: Boolean
) {
    fun aJson(): String = JSONObject().apply {
        putOpt("temperaturaC", temperaturaC)
        putOpt("sensacionTermicaC", sensacionTermicaC)
        putOpt("humedadPorc", humedadPorc)
        putOpt("presionHpa", presionHpa)
        putOpt("vientoKmh", vientoKmh)
        putOpt("direccionVientoGrados", direccionVientoGrados)
        putOpt("rafagasKmh", rafagasKmh)
        putOpt("codigoClima", codigoClima)
        putOpt("nubosidadPorc", nubosidadPorc)
        putOpt("olaM", olaM)
        putOpt("periodoOlaS", periodoOlaS)
        putOpt("direccionOlaGrados", direccionOlaGrados)
        putOpt("marDeFondoM", marDeFondoM)
        putOpt("temperaturaAguaC", temperaturaAguaC)
        putOpt("corrienteKmh", corrienteKmh)
        putOpt("alturaMareaM", alturaMareaM)
        putOpt("estadoMarea", estadoMarea)
        put("faseLunar", faseLunar)
        put("iluminacionLunaPorc", iluminacionLunaPorc)
        putOpt("aptitudAgua", aptitudAgua)
        put("conDatosOnline", conDatosOnline)
    }.toString()

    companion object {
        fun desdeJson(texto: String): CondicionesRegistro? = intentar("CondicionesRegistro") {
            val j = JSONObject(texto)
            fun d(n: String) = if (j.has(n) && !j.isNull(n)) j.getDouble(n) else null
            fun i(n: String) = if (j.has(n) && !j.isNull(n)) j.getInt(n) else null
            fun s(n: String) = if (j.has(n) && !j.isNull(n)) j.getString(n) else null
            CondicionesRegistro(
                temperaturaC = d("temperaturaC"),
                sensacionTermicaC = d("sensacionTermicaC"),
                humedadPorc = i("humedadPorc"),
                presionHpa = d("presionHpa"),
                vientoKmh = d("vientoKmh"),
                direccionVientoGrados = d("direccionVientoGrados"),
                rafagasKmh = d("rafagasKmh"),
                codigoClima = i("codigoClima"),
                nubosidadPorc = i("nubosidadPorc"),
                olaM = d("olaM"),
                periodoOlaS = d("periodoOlaS"),
                direccionOlaGrados = d("direccionOlaGrados"),
                marDeFondoM = d("marDeFondoM"),
                temperaturaAguaC = d("temperaturaAguaC"),
                corrienteKmh = d("corrienteKmh"),
                alturaMareaM = d("alturaMareaM"),
                estadoMarea = s("estadoMarea"),
                faseLunar = s("faseLunar") ?: "",
                iluminacionLunaPorc = i("iluminacionLunaPorc") ?: 0,
                aptitudAgua = s("aptitudAgua"),
                conDatosOnline = j.optBoolean("conDatosOnline", false)
            )
        }.getOrNull()
    }
}
