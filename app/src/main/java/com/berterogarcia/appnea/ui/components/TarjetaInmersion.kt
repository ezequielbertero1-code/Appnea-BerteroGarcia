package com.berterogarcia.appnea.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.data.local.CondicionesRegistro
import com.berterogarcia.appnea.data.local.InmersionEntity
import com.berterogarcia.appnea.data.remote.emojiClima
import com.berterogarcia.appnea.ui.condiciones.direccion
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.FondoTarjeta
import com.berterogarcia.appnea.ui.theme.TextoChip
import com.berterogarcia.appnea.ui.theme.TextoSecundario
import com.berterogarcia.appnea.ui.theme.VeloFuerte
import com.berterogarcia.appnea.ui.theme.VeloMedio
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration

private val LOCALE_ES: Locale = Locale.forLanguageTag("es-AR")
private val FORMATO_MES_CORTO: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", LOCALE_ES)

/** Tarjeta de una inmersión (pantalla Inmersiones e Inicio). Toda la tarjeta abre el detalle. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TarjetaInmersion(inmersion: InmersionEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta, contentColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Fecha en bloque
                Column(modifier = Modifier.width(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "%02d".format(inmersion.fecha.dayOfMonth),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(mesCorto(inmersion.fecha), style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        inmersion.lugar.ifBlank { "Sin lugar" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // "🤿 Buceo · Recreativo": el tipo en cian y la actividad al lado
                    // (si la actividad repite el tipo, como "Apnea", no se muestra dos veces)
                    val actividad = primeraLinea(inmersion.actividad)
                        ?.takeUnless { it.equals(inmersion.tipo.etiqueta, ignoreCase = true) }
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Acento, fontWeight = FontWeight.SemiBold)) {
                                append("${inmersion.tipo.emoji} ${inmersion.tipo.etiqueta}")
                            }
                            actividad?.let { append(" · $it") }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "%.1f m".format(inmersion.profundidadMetros),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Acento
                )
            }

            // Datos de la inmersión
            FlowRow(
                modifier = Modifier.padding(top = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                ChipDato("⏱ Fondo ${formatoDuracion(inmersion.tiempoFondo)}")
                ChipDato("🌡️ Agua ${numeroCorto(inmersion.temperaturaAguaC)} °C")
                if (inmersion.visibilidadMetros > 0f) {
                    ChipDato("👁 Visib. ${numeroCorto(inmersion.visibilidadMetros)} m")
                }
            }

            // Condiciones guardadas al registrar (las inmersiones viejas no tienen: no se muestra nada)
            inmersion.condiciones?.let { LineaCondiciones(it) }
        }
    }
}

@Composable
private fun ChipDato(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium,
        color = TextoChip,
        modifier = Modifier
            .background(VeloMedio, RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LineaCondiciones(c: CondicionesRegistro) {
    val datos = buildList {
        if (c.codigoClima != null || c.temperaturaC != null) {
            val emoji = c.codigoClima?.let { emojiClima(it) } ?: "🌡️"
            add("$emoji ${c.temperaturaC?.let { "${it.roundToInt()} °C" }.orEmpty()}".trim())
        }
        c.vientoKmh?.let { v ->
            // direccion() devuelve "-" si no hay dato: se recorta para que no quede "12 km/h -"
            add("💨 ${v.roundToInt()} km/h ${direccion(c.direccionVientoGrados)}".trimEnd('-', ' '))
        }
        c.olaM?.let { add("🌊 ${"%.1f".format(it)} m") }
        c.faseLunar.takeIf { it.isNotBlank() }?.let { add("🌙 $it") }
    }
    if (datos.isEmpty()) return

    HorizontalDivider(modifier = Modifier.padding(top = 9.dp), color = VeloFuerte)
    FlowRow(
        modifier = Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        datos.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = TextoSecundario) }
    }
}

/** "oct." → "oct" (según la versión de Android, el mes abreviado puede venir con punto). */
private fun mesCorto(fecha: LocalDate): String = fecha.format(FORMATO_MES_CORTO).removeSuffix(".")

/** La actividad es texto libre de varias líneas: en la tarjeta va solo la primera. */
private fun primeraLinea(texto: String): String? =
    texto.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }

/** 11.0 → "11", 11.5 → "11,5" */
private fun numeroCorto(v: Float): String =
    if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(v)

/** mm:ss */
private fun formatoDuracion(d: Duration): String =
    d.toComponents { minutos, segundos, _ -> "%d:%02d".format(minutos, segundos) }