package com.berterogarcia.appnea.ui.condiciones

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.condiciones.Condiciones
import com.berterogarcia.appnea.ui.components.nombre
import com.berterogarcia.appnea.ui.components.nombreFaseLunar
import com.berterogarcia.appnea.ui.theme.ColorRegular
import com.berterogarcia.appnea.ui.theme.Divisor
import com.berterogarcia.appnea.ui.theme.SuperficieTonalAlta
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

// Página Sol y Luna de Condiciones.

@Composable
internal fun PaginaSolLuna(c: Condiciones) {
    val a = c.astroHoy
    TarjetaCondicion(stringResource(R.string.condiciones_sol)) {
        val amanecer = a.amanecer
        val atardecer = a.atardecer
        val esDeDia = amanecer != null && atardecer != null && c.ahora.isAfter(amanecer) && c.ahora.isBefore(atardecer)
        val progreso = if (amanecer != null && atardecer != null) {
            (ChronoUnit.MINUTES.between(amanecer, c.ahora).toFloat() /
                ChronoUnit.MINUTES.between(amanecer, atardecer).toFloat()).coerceIn(0f, 1f)
        } else 0f
        Text(
            stringResource(if (esDeDia) R.string.condiciones_dia else R.string.condiciones_noche),
            color = TextoSuperficieVariante,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DatoGrande(hora(amanecer), stringResource(R.string.condiciones_salida), color = ColorRegular)
            MedioCirculo(progreso, ColorRegular, ancho = 110.dp)
            DatoGrande(hora(atardecer), stringResource(R.string.condiciones_puesta), color = ColorRegular)
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Divisor)
        if (amanecer != null && atardecer != null) {
            if (esDeDia) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    DatoGrande(duracion(ChronoUnit.MINUTES.between(amanecer, c.ahora)), stringResource(R.string.condiciones_desde_salida))
                    DatoGrande(duracion(ChronoUnit.MINUTES.between(c.ahora, atardecer)), stringResource(R.string.condiciones_para_puesta))
                }
                Spacer(Modifier.height(8.dp))
            }
            FilaDato(stringResource(R.string.condiciones_duracion_dia), duracion(ChronoUnit.MINUTES.between(amanecer, atardecer)))
        }
        FilaDato(stringResource(R.string.condiciones_primera_luz), hora(a.inicioCrepusculo))
        FilaDato(stringResource(R.string.condiciones_ultima_luz), hora(a.finCrepusculo))
        FilaDato(stringResource(R.string.condiciones_mediodia), hora(a.mediodiaSolar))
        FilaDato(stringResource(R.string.condiciones_altura_sol), "${a.alturaMaximaSol.roundToInt()}°")
        FilaDato(stringResource(R.string.condiciones_uv_max), num(c.dias.firstOrNull()?.uvMax, 1))
    }

    TarjetaCondicion(stringResource(R.string.detalle_luna)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FaseLunar(iluminacionPorc = a.iluminacionPorc, fase = a.fase, tamanio = 52.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(nombreFaseLunar(a.fase), fontSize = 22.sp, color = TextoSuperficie)
                Text(
                    stringResource(R.string.condiciones_luna_iluminada, a.iluminacionPorc, "%.1f".format(a.edadLunaDias)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuperficieVariante
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        FilaDato(stringResource(R.string.condiciones_salida_luna), hora(a.salidaLuna))
        FilaDato(stringResource(R.string.condiciones_luna_alto), hora(a.transitoLuna))
        FilaDato(stringResource(R.string.condiciones_puesta_luna), hora(a.puestaLuna))
        FilaDato(stringResource(R.string.condiciones_luna_bajo), hora(a.antitransitoLuna))
        FilaDato(stringResource(R.string.condiciones_proxima_llena), diaCorto(a.proximaLunaLlena, c.ahora.toLocalDate()))
        FilaDato(stringResource(R.string.condiciones_proxima_nueva), diaCorto(a.proximaLunaNueva, c.ahora.toLocalDate()))
    }

    TarjetaCondicion(stringResource(R.string.condiciones_fases_semana)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            c.astroDias.forEach { d ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FaseLunar(iluminacionPorc = d.iluminacionPorc, fase = d.fase, tamanio = 22.dp)
                    Text("${d.iluminacionPorc}%", style = MaterialTheme.typography.bodySmall, color = TextoSuperficie, modifier = Modifier.padding(top = 4.dp))
                    Text(
                        diaCorto(d.fecha, c.ahora.toLocalDate()).take(3),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextoSuperficieVariante
                    )
                }
            }
        }
    }
}

/**
 * Dibujo de la fase lunar (reemplaza a los emojis 🌒🌓…): disco oscuro con la parte iluminada.
 * Creciente = iluminada a la derecha (como se ve desde el hemisferio norte y en los íconos
 * habituales); menguante = a la izquierda. TalkBack lee el nombre de la fase.
 */
@Composable
fun FaseLunar(iluminacionPorc: Int, fase: String, modifier: Modifier = Modifier, tamanio: Dp = 24.dp) {
    val fraccion = (iluminacionPorc.coerceIn(0, 100)) / 100f
    val creciente = fase.contains("creciente", ignoreCase = true)
    val descripcionFase = nombreFaseLunar(fase)
    Canvas(modifier.size(tamanio).semantics { contentDescription = descripcionFase }) {
        val r = size.minDimension / 2
        val centro = Offset(size.width / 2, size.height / 2)
        drawCircle(SuperficieTonalAlta, radius = r, center = centro)
        if (fraccion <= 0.01f) return@Canvas
        if (fraccion >= 0.99f) {
            drawCircle(TextoSuperficie, radius = r, center = centro)
            return@Canvas
        }
        // Media luna iluminada (derecha) + elipse del terminador: hacia afuera si es menos de la mitad
        val ancho = r * abs(1 - 2 * fraccion)
        val circulo = Rect(centro.x - r, centro.y - r, centro.x + r, centro.y + r)
        val elipse = Rect(centro.x - ancho, centro.y - r, centro.x + ancho, centro.y + r)
        val iluminada = Path().apply {
            arcTo(circulo, -90f, 180f, forceMoveTo = true)                       // arriba → derecha → abajo
            arcTo(elipse, 90f, if (fraccion < 0.5f) -180f else 180f, forceMoveTo = false)
            close()
        }
        // Menguante: espejo horizontal
        scale(scaleX = if (creciente) 1f else -1f, scaleY = 1f, pivot = centro) {
            drawPath(iluminada, TextoSuperficie)
        }
    }
}

// ============================== 7 DÍAS ==============================
