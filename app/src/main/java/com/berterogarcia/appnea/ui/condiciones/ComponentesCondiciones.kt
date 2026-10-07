package com.berterogarcia.appnea.ui.condiciones

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berterogarcia.appnea.data.condiciones.Aptitud
import com.berterogarcia.appnea.data.remote.direccionCardinal
import com.berterogarcia.appnea.ui.theme.ColorBueno
import com.berterogarcia.appnea.ui.theme.ColorMalo
import com.berterogarcia.appnea.ui.theme.ColorRegular
import com.berterogarcia.appnea.ui.theme.Divisor
import com.berterogarcia.appnea.ui.theme.FondoTarjeta
import com.berterogarcia.appnea.ui.theme.FondoTarjetaResaltada
import com.berterogarcia.appnea.ui.theme.TextoSecundario
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val LOCALE_AR: Locale = Locale.forLanguageTag("es-AR")
private val HH_MM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DD_MM: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")

// ===================== Formatos =====================
fun hora(t: LocalDateTime?): String = t?.format(HH_MM) ?: "--:--"

fun diaCorto(fecha: LocalDate, hoy: LocalDate): String = when (fecha) {
    hoy -> "Hoy"
    hoy.plusDays(1) -> "Mañana"
    else -> fecha.dayOfWeek.getDisplayName(TextStyle.SHORT, LOCALE_AR)
        .replaceFirstChar { it.uppercase() } + " " + fecha.format(DD_MM)
}

fun diaLargo(fecha: LocalDate): String =
    fecha.dayOfWeek.getDisplayName(TextStyle.FULL, LOCALE_AR).replaceFirstChar { it.uppercase() } +
        " " + fecha.dayOfMonth + " de " + fecha.month.getDisplayName(TextStyle.FULL, LOCALE_AR)

fun duracion(minutos: Long): String {
    val m = if (minutos < 0) 0 else minutos
    return if (m >= 60) "${m / 60} h ${m % 60} min" else "$m min"
}

fun num(v: Double?, decimales: Int = 0, unidad: String = ""): String =
    v?.let { (if (decimales == 0) it.roundToInt().toString() else "%.${decimales}f".format(it)) + unidad } ?: "-"

fun direccion(grados: Double?): String = grados?.let { direccionCardinal(it) } ?: "-"

fun colorAptitud(a: Aptitud): Color = when (a) {
    Aptitud.BUENA -> ColorBueno
    Aptitud.REGULAR -> ColorRegular
    Aptitud.MALA -> ColorMalo
}

// ===================== Componentes =====================

/** Tarjeta con el estilo de la app (fondo oscuro, texto blanco) y un título opcional. */
@Suppress("ModifierParameter") // título primero para poder llamarla como TarjetaCondicion("Mar") { }
@Composable
fun TarjetaCondicion(
    titulo: String? = null,
    modifier: Modifier = Modifier,
    resaltada: Boolean = false,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (resaltada) FondoTarjetaResaltada else FondoTarjeta,
            contentColor = Color.White
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (titulo != null) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }
            contenido()
        }
    }
}

/** Fila "etiqueta ........ valor". */
@Composable
fun FilaDato(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            valor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Dato grande con etiqueta abajo (como "10:09 h / BAJAMAR"). */
@Composable
fun DatoGrande(valor: String, etiqueta: String, modifier: Modifier = Modifier, color: Color = Color.White) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
        Text(etiqueta.uppercase(), style = MaterialTheme.typography.labelMedium, color = TextoSecundario)
    }
}


/** Punto de color + texto (semáforo). */
@Composable
fun Semaforo(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(texto, fontWeight = FontWeight.Bold, color = color, fontSize = 18.sp)
    }
}

/**
 * Medio círculo de progreso (0..1), como el indicador de marea / sol de las apps de pesca.
 */
@Composable
fun MedioCirculo(progreso: Float, color: Color, modifier: Modifier = Modifier, ancho: Dp = 120.dp) {
    Canvas(modifier = modifier.size(width = ancho, height = ancho / 2 + 8.dp)) {
        val grosor = 14.dp.toPx()
        val diametro = size.width - grosor
        val arriba = grosor / 2
        val izquierda = grosor / 2
        drawArc(
            color = Divisor,
            startAngle = 180f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(izquierda, arriba), size = Size(diametro, diametro),
            style = Stroke(width = grosor, cap = StrokeCap.Round)
        )
        drawArc(
            color = color,
            startAngle = 180f, sweepAngle = 180f * progreso.coerceIn(0f, 1f), useCenter = false,
            topLeft = Offset(izquierda, arriba), size = Size(diametro, diametro),
            style = Stroke(width = grosor, cap = StrokeCap.Round)
        )
    }
}