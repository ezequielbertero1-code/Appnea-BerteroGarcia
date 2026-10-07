package com.berterogarcia.appnea.ui.detalleinmersion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berterogarcia.appnea.data.local.formatearDuracion
import com.berterogarcia.appnea.data.perfil.PuntoPerfil
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorBajamar
import com.berterogarcia.appnea.ui.theme.Contorno
import com.berterogarcia.appnea.ui.theme.Divisor
import com.berterogarcia.appnea.ui.theme.TextoTerciario
import kotlin.math.roundToInt

private val estiloEje = TextStyle(color = TextoTerciario, fontSize = 11.sp)

private fun DrawScope.texto(
    medidor: TextMeasurer, t: String, x: Float, y: Float,
    estilo: TextStyle = estiloEje, centrado: Boolean = true
) {
    val medida = medidor.measure(t, estilo)
    val ancho = medida.size.width.toFloat()
    val xFinal = (if (centrado) x - ancho / 2 else x).coerceIn(0f, (size.width - ancho).coerceAtLeast(0f))
    drawText(medida, topLeft = Offset(xFinal, y))
}

/** Perfil profundidad/tiempo: superficie arriba, profundidad hacia abajo. */
@Composable
fun GraficoPerfil(puntos: List<PuntoPerfil>, modifier: Modifier = Modifier, progreso: Float = 1f) {
    val medidor = rememberTextMeasurer()
    Canvas(modifier = modifier.fillMaxWidth().height(240.dp)) {
        if (puntos.size < 2) return@Canvas
        val izq = 42.dp.toPx()
        val der = 8.dp.toPx()
        val arriba = 10.dp.toPx()
        val abajo = size.height - 22.dp.toPx()
        val ancho = size.width - izq - der

        val tMax = puntos.last().segundo.coerceAtLeast(1)
        val pMax = puntos.maxOf { it.profundidadM }.coerceAtLeast(1.0)
        val escala = pMax * 1.15   // deja lugar abajo del pico para la etiqueta

        fun x(s: Int) = izq + s.toFloat() / tMax * ancho
        fun y(p: Double) = arriba + (p / escala).toFloat() * (abajo - arriba)

        // Líneas guía: superficie, mitad y máxima
        listOf(0.0, pMax / 2, pMax).forEach { p ->
            drawLine(Divisor, Offset(izq, y(p)), Offset(izq + ancho, y(p)))
            texto(medidor, "${p.roundToInt()} m", 0f, y(p) - 7.dp.toPx(), centrado = false)
        }

        // Curva (ya viene suavizada) + relleno entre la superficie y el perfil
        // Solo se ve la parte de la curva hasta "progreso" → efecto de que se va dibujando
        clipRect(right = izq + ancho * progreso.coerceIn(0f, 1f)) {
            val curva = Path().apply {
                moveTo(x(puntos[0].segundo), y(puntos[0].profundidadM))
                for (k in 1 until puntos.size) lineTo(
                    x(puntos[k].segundo),
                    y(puntos[k].profundidadM)
                )
            }
            val relleno = Path().apply { addPath(curva); close() }   // empieza y termina en 0 m
            drawPath(relleno, color = Acento.copy(alpha = 0.25f))
            drawPath(curva, color = Acento, style = Stroke(width = 2.5.dp.toPx()))

            // Punto de máxima profundidad
            val pico = puntos.maxBy { it.profundidadM }

            val centro = Offset(x(pico.segundo), y(pico.profundidadM))

            drawCircle(ColorBajamar, radius = 5.dp.toPx(), center = centro)
            texto(
                medidor, "%.1f m".format(pico.profundidadM), centro.x, centro.y + 8.dp.toPx(),
                TextStyle(color = ColorBajamar, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
        }
        // Eje X en mm:ss
        for (k in 0..4) {
            val s = tMax * k / 4
            val xs = x(s)
            drawLine(Contorno, Offset(xs, abajo), Offset(xs, abajo + 4.dp.toPx()))
            texto(medidor, formatearDuracion(s), xs, abajo + 5.dp.toPx())
        }
    }
}