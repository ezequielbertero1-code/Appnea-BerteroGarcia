package com.berterogarcia.appnea.ui.condiciones

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berterogarcia.appnea.data.condiciones.ExtremoMarea
import com.berterogarcia.appnea.data.condiciones.HoraMar
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private val estiloEje = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)

private fun DrawScope.texto(medidor: TextMeasurer, t: String, x: Float, y: Float, estilo: TextStyle, centrado: Boolean = true) {
    val medida = medidor.measure(t, estilo)
    val ancho = medida.size.width.toFloat()
    val xFinal = (if (centrado) x - ancho / 2 else x).coerceIn(0f, (size.width - ancho).coerceAtLeast(0f))
    drawText(medida, topLeft = Offset(xFinal, y))
}

/**
 * Curva de marea de un día (0 a 24 h) con pleamares, bajamares, horas de luz y "ahora".
 */
@Composable
fun GraficoMarea(
    fecha: LocalDate,
    horas: List<HoraMar>,
    extremos: List<ExtremoMarea>,
    amanecer: LocalDateTime?,
    atardecer: LocalDateTime?,
    ahora: LocalDateTime?,
    modifier: Modifier = Modifier
) {
    val medidor = rememberTextMeasurer()
    val inicio = fecha.atStartOfDay()
    val fin = inicio.plusDays(1)
    val puntos = horas.filter { it.nivelMarM != null && !it.hora.isBefore(inicio) && !it.hora.isAfter(fin) }
    val extremosDia = extremos.filter { !it.hora.isBefore(inicio) && it.hora.isBefore(fin) }

    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        if (puntos.size < 2) return@Canvas
        val izq = 34.dp.toPx()
        val arriba = 26.dp.toPx()
        val abajo = size.height - 22.dp.toPx()
        val ancho = size.width - izq

        val valores = puntos.map { it.nivelMarM!! }
        val minV = valores.min()
        val maxV = valores.max()
        val margen = ((maxV - minV) * 0.25).coerceAtLeast(0.2)
        val yMin = minV - margen
        val yMax = maxV + margen

        fun x(t: LocalDateTime) = izq + ChronoUnit.MINUTES.between(inicio, t) / 1440f * ancho
        fun y(v: Double) = (abajo - (v - yMin) / (yMax - yMin) * (abajo - arriba)).toFloat()

        // Horas de luz
        if (amanecer != null && atardecer != null) {
            drawRect(
                color = ColorRegular.copy(alpha = 0.10f),
                topLeft = Offset(x(amanecer), arriba),
                size = Size(x(atardecer) - x(amanecer), abajo - arriba)
            )
        }

        // Curva suavizada
        val curva = Path().apply {
            moveTo(x(puntos[0].hora), y(valores[0]))
            for (i in 1 until puntos.size) {
                val x0 = x(puntos[i - 1].hora); val y0 = y(valores[i - 1])
                val x1 = x(puntos[i].hora); val y1 = y(valores[i])
                val xm = (x0 + x1) / 2
                cubicTo(xm, y0, xm, y1, x1, y1)
            }
        }
        val relleno = Path().apply {
            addPath(curva)
            lineTo(x(puntos.last().hora), abajo)
            lineTo(x(puntos.first().hora), abajo)
            close()
        }
        drawPath(relleno, color = Acento.copy(alpha = 0.30f))
        drawPath(curva, color = Acento, style = Stroke(width = 2.5.dp.toPx()))

        // Eje Y (min / max)
        texto(medidor, "%.1f m".format(maxV), 0f, y(maxV) - 8.dp.toPx(), estiloEje, centrado = false)
        texto(medidor, "%.1f m".format(minV), 0f, y(minV) - 8.dp.toPx(), estiloEje, centrado = false)

        // Eje X
        for (h in 0..24 step 4) {
            val xh = izq + h / 24f * ancho
            drawLine(Color.White.copy(alpha = 0.3f), Offset(xh, abajo), Offset(xh, abajo + 4.dp.toPx()))
            texto(medidor, "$h h", xh, abajo + 5.dp.toPx(), estiloEje)
        }

        // Ahora
        if (ahora != null && !ahora.isBefore(inicio) && ahora.isBefore(fin)) {
            val xa = x(ahora)
            drawLine(Color.White.copy(alpha = 0.7f), Offset(xa, arriba), Offset(xa, abajo), strokeWidth = 1.5.dp.toPx())
        }

        // Pleamares y bajamares
        extremosDia.forEach { e ->
            val color = if (e.esPleamar) Acento else ColorBajamar
            val px = x(e.hora)
            val py = y(e.alturaM)
            drawCircle(color, radius = 5.dp.toPx(), center = Offset(px, py))
            val etiqueta = hora(e.hora)
            val estilo = TextStyle(color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            val yTexto = if (e.esPleamar) py - 22.dp.toPx() else py + 8.dp.toPx()
            texto(medidor, etiqueta, px, yTexto, estilo)
        }
    }
}