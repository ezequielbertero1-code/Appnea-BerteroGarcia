package com.berterogarcia.appnea.data.export

import android.util.Xml
import com.berterogarcia.appnea.data.local.InmersionEntity
import com.berterogarcia.appnea.data.local.formatearDuracion
import org.xmlpull.v1.XmlSerializer
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Genera un archivo UDDF 3.2 (Universal Dive Data Format) a partir de una inmersión. */
object UddfExporter {
    private const val NS = "http://www.streit.cc/uddf/3.2/"
    private const val CERO_ABSOLUTO = 273.15
    private val ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun generar(i: InmersionEntity): String {
        val writer = StringWriter()
        val xml = Xml.newSerializer()
        xml.setOutput(writer)
        xml.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
        xml.startDocument("UTF-8", null)
        xml.setPrefix("", NS)
        xml.startTag(NS, "uddf")
        xml.attribute(null, "version", "3.2.1")

        xml.bloque("generator") {
            dato("name", "Appnea")
            dato("type", "logbook")
            dato("version", "1.0")
            dato("datetime", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(ISO))
        }

        val siteId = "site_${i.id}"
        xml.bloque("divesite") {
            bloque("site", "id" to siteId) { dato("name", i.lugar) }
        }

        xml.bloque("profiledata") {
            bloque("repetitiongroup", "id" to "rg_${i.id}") {
                bloque("dive", "id" to "dive_${i.id}") {
                    bloque("informationbeforedive") {
                        vacio("link", "ref" to siteId)
                        dato("divenumber", i.id.toString())
                        dato("airtemperature", kelvin(i.temperaturaAireC))
                        dato("datetime", LocalDateTime.of(i.fecha, i.horaEntrada).format(ISO))
                    }
                    bloque("informationafterdive") {
                        dato("lowesttemperature", kelvin(i.temperaturaAguaC))
                        dato("greatestdepth", numero(i.profundidadMetros))
                        dato("visibility", numero(i.visibilidadMetros))
                        dato("diveduration", duracionSegundos(i).toString())
                        bloque("equipmentused") { dato("leadquantity", numero(i.lastreKg)) }
                        bloque("notes") { notas(i).forEach { dato("para", it) } }
                    }
                }
            }
        }

        xml.endTag(NS, "uddf")
        xml.endDocument()
        return writer.toString()
    }

    // Datos que UDDF no tiene como campo propio van como líneas de <notes>
    private fun notas(i: InmersionEntity): List<String> = buildList {
        if (i.actividad.isNotBlank()) add("Actividad: ${i.actividad}")
        add("Tiempo de fondo: ${formatearDuracion(i.tiempoFondo.inWholeSeconds.toInt())}")
        add("Parada de seguridad: ${numero(i.paradaSeguridadMin)} min")
        add("Protección: ${numero(i.proteccionMm)} mm")
        if (i.equipamiento.isNotEmpty()) add("Equipamiento: ${i.equipamiento.joinToString()}")
        if (i.caracteristicasAgua.isNotEmpty()) add("Agua: ${i.caracteristicasAgua.joinToString()}")
        if (i.comentarios.isNotBlank()) add("Comentarios: ${i.comentarios}")
    }

    // Duración total en el agua = salida - entrada (si cruza la medianoche, suma 24 h)
    private fun duracionSegundos(i: InmersionEntity): Long {
        val s = java.time.Duration.between(i.horaEntrada, i.horaSalida).seconds
        return if (s >= 0) s else s + 24 * 3600
    }

    // Locale.US para que el decimal sea "." y no "," (el XML lo exige)
    private fun numero(v: Float) = String.format(Locale.US, "%.1f", v)
    private fun kelvin(celsius: Float) = String.format(Locale.US, "%.2f", celsius + CERO_ABSOLUTO)

    // --- helpers para escribir XML más prolijo ---
    private fun XmlSerializer.bloque(
        nombre: String,
        vararg atributos: Pair<String, String>,
        contenido: XmlSerializer.() -> Unit
    ) {
        startTag(NS, nombre)
        atributos.forEach { (k, v) -> attribute(null, k, v) }
        contenido()
        endTag(NS, nombre)
    }

    private fun XmlSerializer.dato(nombre: String, valor: String) {
        startTag(NS, nombre); text(valor); endTag(NS, nombre)
    }

    private fun XmlSerializer.vacio(nombre: String, vararg atributos: Pair<String, String>) {
        startTag(NS, nombre)
        atributos.forEach { (k, v) -> attribute(null, k, v) }
        endTag(NS, nombre)
    }
}
/**
 * Genera un archivo UDDF 3.2 (Universal Dive Data Format) a partir de una inmersión.
 *
 * UDDF es un XML estándar para intercambiar inmersiones entre apps y computadoras
 * de buceo (Subsurface, MacDive, etc.). Todos los valores van en unidades SI:
 * metros, segundos, kilogramos y Kelvin (K = °C + 273.15).
 * Especificación: https://www.streit.cc/extern/uddf_v321/en/index.html
 *
 * ── Mapeo de campos ─────────────────────────────────────────────────────────────
 *
 *  InmersionEntity          UDDF                                        Unidad
 *  ───────────────────────  ──────────────────────────────────────────  ───────
 *  id                       <dive id="dive_{id}">                       —
 *                           <informationbeforedive>/<divenumber>        —
 *  lugar                    <divesite>/<site>/<name>                    —
 *                           (la dive lo referencia con <link ref="site_{id}"/>)
 *  fecha + horaEntrada      <informationbeforedive>/<datetime>          ISO 8601
 *  temperaturaAireC         <informationbeforedive>/<airtemperature>    K
 *  temperaturaAguaC         <informationafterdive>/<lowesttemperature>  K
 *  profundidadMetros        <informationafterdive>/<greatestdepth>      m
 *  horaSalida - horaEntrada <informationafterdive>/<diveduration>       s
 *  visibilidadMetros        <informationafterdive>/<visibility>         m
 *  lastreKg                 <informationafterdive>/<equipmentused>/
 *                             <leadquantity>                            kg
 *
 * ── Campos sin equivalente estándar en UDDF → van como líneas de <notes>/<para> ──
 *
 *  actividad                "Actividad: ..."
 *  tiempoFondo              "Tiempo de fondo: mm:ss"
 *  paradaSeguridadMin       "Parada de seguridad: x min"
 *  proteccionMm             "Protección: x mm"
 *  equipamiento             "Equipamiento: a, b, c"
 *  caracteristicasAgua      "Agua: a, b, c"
 *  comentarios              "Comentarios: ..."
 *
 * ── Limitaciones ────────────────────────────────────────────────────────────────
 *
 *  - No se exporta perfil de inmersión (<samples>, profundidad a lo largo del tiempo),
 *    porque la app no lo registra: el archivo contiene solo el resumen.
 *  - Si la hora de salida es menor que la de entrada, se asume que la inmersión
 *    cruzó la medianoche y se suman 24 h a la duración.
 */