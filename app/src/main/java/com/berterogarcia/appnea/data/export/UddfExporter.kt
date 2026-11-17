package com.berterogarcia.appnea.data.export

import android.util.Xml
import com.berterogarcia.appnea.dominio.modelo.CategoriaEquipo
import com.berterogarcia.appnea.dominio.modelo.Inmersion
import com.berterogarcia.appnea.dominio.modelo.formatearDuracion
import com.berterogarcia.appnea.dominio.PerfilSimulado
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

    /** @param autor nombre que va en <owner> (el usuario logueado). */
    fun generar(i: Inmersion, autor: String = ""): String {
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

        // Equipo: se define en <diver>/<owner>/<equipment> y la inmersión lo referencia con <link>
        val equipo = piezasDeEquipo(i)
        if (equipo.isNotEmpty()) xml.bloque("diver") {
            bloque("owner", "id" to "owner") {
                bloque("personal") { dato("firstname", autor.ifBlank { "Appnea" }) }
                bloque("equipment") {
                    equipo.forEach { pieza ->
                        bloque(pieza.elemento, "id" to pieza.id) { dato("name", pieza.nombre) }
                    }
                }
            }
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
                    // Perfil simulado: es lo que usa el viewer (Subsurface, etc.) para dibujar el gráfico
                    val perfil = PerfilSimulado.generar(i)
                    if (perfil.isNotEmpty()) bloque("samples") {
                        perfil.forEach { p ->
                            bloque("waypoint") {
                                dato("depth", String.format(Locale.US, "%.2f", p.profundidadM))
                                dato("divetime", p.segundo.toString())
                            }
                        }
                    }
                    bloque("informationafterdive") {
                        dato("lowesttemperature", kelvin(i.temperaturaAguaC))
                        dato("greatestdepth", numero(i.profundidadMetros))
                        i.visibilidadMetros?.let { dato("visibility", numero(it)) }
                        dato("diveduration", duracionSegundos(i).toString())
                        bloque("equipmentused") {
                            i.lastreKg?.let { dato("leadquantity", numero(it)) }
                            equipo.forEach { vacio("link", "ref" to it.id) }
                        }
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
    private fun notas(i: Inmersion): List<String> = buildList {
        if (i.actividad.isNotBlank()) add("Actividad: ${i.actividad}")
        if (i.apnea == null || i.tiempoFondo.inWholeSeconds > 0)
            add("Tiempo de fondo: ${formatearDuracion(i.tiempoFondo.inWholeSeconds.toInt())}")
        add("Parada de seguridad: ${numero(i.paradaSeguridadMin)} min")
        i.proteccionMm?.let { add("Protección: ${numero(it)} mm") }
        i.visibilidad?.let { add("Visibilidad (escala): ${it.name.lowercase().replace('_', ' ')}") }
        i.apnea?.let { a ->
            add("Apnea: ${a.cantidadBajadas} bajadas, profundidad máxima ${numero(a.profundidadMaximaMetros)} m")
            a.bajadas.forEachIndexed { n, b ->
                add("Bajada ${n + 1}: ${numero(b.profundidadMetros)} m, ${formatearDuracion(b.tiempoSegundos)}")
            }
            add("Síncope: ${if (a.sincope) "sí" else "no"}")
        }
        if (i.equipamiento.isNotEmpty()) add("Equipamiento: ${i.equipamiento.joinToString()}")
        if (i.caracteristicasAgua.isNotEmpty()) add("Agua: ${i.caracteristicasAgua.joinToString()}")
        if (PerfilSimulado.generar(i).isNotEmpty())
            add("Perfil simulado a partir del resumen (no registrado por computadora de buceo)")
        if (i.comentarios.isNotBlank()) add("Comentarios: ${i.comentarios}")
    }

    // ---------------------------- Equipo ----------------------------

    /** Una pieza de equipo en UDDF: [elemento] es la etiqueta del tipo (fins, mask, suit...). */
    private data class PiezaUddf(val id: String, val elemento: String, val nombre: String)

    /** Categoría de la app → elemento de UDDF 3.2.1 (lo que no tiene equivalente va como variouspieces). */
    private val ELEMENTO_POR_CATEGORIA = mapOf(
        CategoriaEquipo.MASCARA to "mask",
        CategoriaEquipo.SNORKEL to "variouspieces",
        CategoriaEquipo.ALETAS to "fins",
        CategoriaEquipo.TRAJE to "suit",
        CategoriaEquipo.GUANTES_ESCARPINES to "gloves",
        CategoriaEquipo.LASTRE to "lead",
        CategoriaEquipo.COMPUTADORA to "divecomputer",
        CategoriaEquipo.REGULADOR to "regulator",
        CategoriaEquipo.CHALECO to "buoyancycontroldevice",
        CategoriaEquipo.TUBO to "tank",
        CategoriaEquipo.BOYA to "variouspieces",
        CategoriaEquipo.CUCHILLO to "knife",
        CategoriaEquipo.LINTERNA to "light",
        CategoriaEquipo.OTRO to "variouspieces"
    )

    /**
     * El equipo de la inmersión se guarda como texto ("Aletas · Mares Avanti (42-43)").
     * La categoría sale de lo que está antes del " · ". Las inmersiones viejas tienen texto
     * libre ("Semi seco", "Guantes"): ahí se adivina por palabras clave.
     */
    private fun piezasDeEquipo(i: Inmersion): List<PiezaUddf> =
        i.equipamiento.filter { it.isNotBlank() }.sorted().mapIndexed { n, etiqueta ->
            val prefijo = etiqueta.substringBefore(" · ").trim()
            val categoria = CategoriaEquipo.entries.firstOrNull { it.etiqueta.equals(prefijo, ignoreCase = true) }
            PiezaUddf(
                id = "eq_${i.id}_${n + 1}",
                elemento = categoria?.let { ELEMENTO_POR_CATEGORIA[it] } ?: elementoPorPalabra(etiqueta),
                nombre = etiqueta.trim()
            )
        }

    private fun elementoPorPalabra(texto: String): String {
        val t = texto.lowercase(Locale.ROOT)
        return when {
            "aleta" in t -> "fins"
            "máscara" in t || "mascara" in t -> "mask"
            "guante" in t -> "gloves"
            "bota" in t || "escarpin" in t || "escarpín" in t -> "boots"
            "traje" in t || "seco" in t || "dos piezas" in t || "neoprene" in t -> "suit"
            "lastre" in t || "cinturón" in t || "plomo" in t -> "lead"
            "computadora" in t || "reloj" in t -> "divecomputer"
            "regulador" in t -> "regulator"
            "chaleco" in t || "bcd" in t -> "buoyancycontroldevice"
            "tubo" in t || "tanque" in t -> "tank"
            "cuchillo" in t -> "knife"
            "linterna" in t -> "light"
            else -> "variouspieces"
        }
    }

    // Duración total en el agua = salida - entrada (si cruza la medianoche, suma 24 h)
    private fun duracionSegundos(i: Inmersion): Long {
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
 *  Inmersion          UDDF                                        Unidad
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
 *  equipamiento             <diver>/<owner>/<equipment>/<fins|mask|suit|...>
 *                           (la dive lo referencia con <equipmentused>/<link ref="eq_..."/>)
 *
 * ── Campos sin equivalente estándar en UDDF → van como líneas de <notes>/<para> ──
 *
 *  actividad                "Actividad: ..."
 *  tiempoFondo              "Tiempo de fondo: mm:ss"
 *  paradaSeguridadMin       "Parada de seguridad: x min"
 *  proteccionMm             "Protección: x mm"
 *  equipamiento             "Equipamiento: a, b, c" (además del bloque <equipment>,
 *                           para los viewers que no muestran equipo)
 *  caracteristicasAgua      "Agua: a, b, c"
 *  comentarios              "Comentarios: ..."
 *
 * ── Limitaciones ────────────────────────────────────────────────────────────────
 *
 *  - No se exporta perfil de inmersión (<samples>, profundidad a lo largo del tiempo),
 *    porque la app no lo registra: el archivo contiene solo el resumen.
 *  - Si la hora de salida es menor que la de entrada, se asume que la inmersión
 *    cruzó la medianoche y se suman 24 h a la duración.
 *  - El perfil (<samples>) es simulado con PerfilSimulado a partir de profundidad, tiempo de fondo y parada; no es un registro real.
 *
 */
