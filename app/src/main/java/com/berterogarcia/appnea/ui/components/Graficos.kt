package com.berterogarcia.appnea.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorBajamar
import com.berterogarcia.appnea.ui.theme.Contorno
import com.berterogarcia.appnea.ui.theme.Divisor
import com.berterogarcia.appnea.ui.theme.FondoTarjeta
import com.berterogarcia.appnea.ui.theme.TextoChip
import com.berterogarcia.appnea.ui.theme.TextoSecundario
import com.berterogarcia.appnea.ui.theme.TextoTerciario
import com.berterogarcia.appnea.ui.theme.VeloMedio
import kotlin.math.abs

// Gráficos dibujados a mano con Canvas, con el mismo estilo que GraficoPerfil (Detalle de inmersión):
// líneas guía con Divisor, datos en Acento con relleno suave, el récord en ColorBajamar.

private val estiloEje = TextStyle(color = TextoTerciario, fontSize = 11.sp)
private val estiloValor = TextStyle(color = TextoChip, fontSize = 11.sp)
private val estiloRecord = TextStyle(color = ColorBajamar, fontSize = 12.sp, fontWeight = FontWeight.Bold)

/** Escribe [t] con el centro (o el borde izquierdo) en x, sin salirse del Canvas. */
private fun DrawScope.texto(
    medidor: TextMeasurer, t: String, x: Float, y: Float,
    estilo: TextStyle = estiloEje, centrado: Boolean = true
) {
    val medida = medidor.measure(t, estilo)
    val ancho = medida.size.width.toFloat()
    val xFinal = (if (centrado) x - ancho / 2 else x).coerceIn(0f, (size.width - ancho).coerceAtLeast(0f))
    val yFinal = y.coerceIn(0f, (size.height - medida.size.height).coerceAtLeast(0f))
    drawText(medida, topLeft = Offset(xFinal, yFinal))
}

/**
 * Progreso 0 → 1 para que el gráfico "se dibuje" de izquierda a derecha (como en Detalle).
 * Se reinicia cada vez que cambia [clave] (por ejemplo, al cambiar un filtro).
 */
@Composable
fun rememberProgresoDibujo(clave: Any?): Float {
    val progreso = remember { Animatable(0f) }
    LaunchedEffect(clave) {
        progreso.snapTo(0f)
        progreso.animateTo(1f, tween(durationMillis = 1000, easing = FastOutSlowInEasing))
    }
    return progreso.value
}

// ================================ Línea ================================

/**
 * Gráfico de línea con un punto por valor (en orden). El valor más alto se marca como récord.
 * @param etiquetasX una por valor; se muestran la primera, la del medio y la última.
 * @param formato cómo se escribe un valor ("24 m", "3:00"...).
 * @param onPuntoClick opcional: índice del punto tocado.
 */
@Composable
fun GraficoLinea(
    valores: List<Float>,
    etiquetasX: List<String>,
    formato: (Float) -> String,
    modifier: Modifier = Modifier,
    progreso: Float = 1f,
    alto: Dp = 180.dp,
    onPuntoClick: ((Int) -> Unit)? = null
) {
    val medidor = rememberTextMeasurer()
    val alTocar by rememberUpdatedState(onPuntoClick)

    // Misma geometría para dibujar y para saber qué punto se tocó
    fun geometria(ancho: Float, altoPx: Float, densidad: Float): Geometria {
        val izq = 42f * densidad
        val der = 10f * densidad
        val arriba = 16f * densidad
        val abajo = altoPx - 22f * densidad
        return Geometria(izq, ancho - izq - der, arriba, abajo)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(alto)
            .pointerInput(valores) {
                detectTapGestures { toque ->
                    val accion = alTocar ?: return@detectTapGestures
                    if (valores.isEmpty()) return@detectTapGestures
                    val g = geometria(size.width.toFloat(), size.height.toFloat(), density)
                    val indice = valores.indices.minByOrNull { abs(g.x(it, valores.size) - toque.x) }
                    if (indice != null && abs(g.x(indice, valores.size) - toque.x) < 24f * density) accion(indice)
                }
            }
    ) {
        if (valores.isEmpty()) return@Canvas
        val g = geometria(size.width, size.height, density)
        val maximo = valores.max().coerceAtLeast(0.0001f)
        val escala = maximo * 1.15f   // deja lugar arriba del récord para la etiqueta

        fun y(v: Float) = g.abajo - (v / escala) * (g.abajo - g.arriba)

        // Líneas guía: 0, mitad y máximo
        listOf(0f, maximo / 2, maximo).forEach { v ->
            drawLine(Divisor, Offset(g.izq, y(v)), Offset(g.izq + g.ancho, y(v)))
            texto(medidor, formato(v), 0f, y(v) - 7.dp.toPx(), centrado = false)
        }

        clipRect(right = g.izq + g.ancho * progreso.coerceIn(0f, 1f) + 8.dp.toPx()) {
            val n = valores.size
            if (n >= 2) {
                val curva = Path().apply {
                    moveTo(g.x(0, n), y(valores[0]))
                    for (k in 1 until n) lineTo(g.x(k, n), y(valores[k]))
                }
                val relleno = Path().apply {
                    addPath(curva)
                    lineTo(g.x(n - 1, n), g.abajo)
                    lineTo(g.x(0, n), g.abajo)
                    close()
                }
                drawPath(relleno, color = Acento.copy(alpha = 0.15f))
                drawPath(curva, color = Acento, style = Stroke(width = 2.5.dp.toPx()))
            }
            // Puntos (si son muchos, solo la línea para que no se empaste)
            if (n <= 40) {
                valores.forEachIndexed { k, v ->
                    val c = Offset(g.x(k, n), y(v))
                    drawCircle(FondoTarjeta, radius = 5.dp.toPx(), center = c)
                    drawCircle(Acento, radius = 3.dp.toPx(), center = c)
                }
            }
            // Récord
            val iMax = valores.indexOf(maximo)
            val centro = Offset(g.x(iMax, n), y(maximo))
            drawCircle(ColorBajamar, radius = 5.dp.toPx(), center = centro)
            texto(medidor, formato(maximo), centro.x, centro.y - 18.dp.toPx(), estiloRecord)
        }

        // Eje X: primera, del medio y última etiqueta
        val n = valores.size
        listOf(0, (n - 1) / 2, n - 1).distinct().forEach { k ->
            val xs = g.x(k, n)
            drawLine(Contorno, Offset(xs, g.abajo), Offset(xs, g.abajo + 4.dp.toPx()))
            texto(medidor, etiquetasX.getOrElse(k) { "" }, xs, g.abajo + 5.dp.toPx())
        }
    }
}

/** Zona de dibujo: margen izquierdo, ancho útil y alturas de arriba/abajo. */
private data class Geometria(val izq: Float, val ancho: Float, val arriba: Float, val abajo: Float) {
    fun x(indice: Int, total: Int): Float =
        if (total <= 1) izq + ancho / 2 else izq + indice.toFloat() / (total - 1) * ancho
}

// ================================ Barras ================================

/**
 * Barras verticales, una por valor, con el número arriba y la etiqueta abajo.
 * Las barras crecen desde abajo según [progreso].
 */
@Composable
fun GraficoBarras(
    valores: List<Float>,
    etiquetas: List<String>,
    formato: (Float) -> String,
    modifier: Modifier = Modifier,
    progreso: Float = 1f,
    alto: Dp = 160.dp
) {
    val medidor = rememberTextMeasurer()
    Canvas(modifier = modifier.fillMaxWidth().height(alto)) {
        if (valores.isEmpty()) return@Canvas
        val arriba = 18.dp.toPx()
        val abajo = size.height - 20.dp.toPx()
        val maximo = valores.max().coerceAtLeast(0.0001f)
        val hueco = size.width / valores.size
        val anchoBarra = (hueco * 0.6f).coerceAtMost(36.dp.toPx())
        val radio = CornerRadius(4.dp.toPx(), 4.dp.toPx())

        drawLine(Contorno, Offset(0f, abajo), Offset(size.width, abajo))
        valores.forEachIndexed { k, v ->
            val centroX = hueco * k + hueco / 2
            val altoBarra = (v / maximo) * (abajo - arriba) * progreso.coerceIn(0f, 1f)
            if (v > 0f) {
                drawRoundRect(
                    color = Acento,
                    topLeft = Offset(centroX - anchoBarra / 2, abajo - altoBarra),
                    size = Size(anchoBarra, altoBarra),
                    cornerRadius = radio
                )
            }
            texto(
                medidor, formato(v), centroX, abajo - altoBarra - 16.dp.toPx(),
                if (v > 0f) estiloValor else estiloEje
            )
            texto(medidor, etiquetas.getOrElse(k) { "" }, centroX, abajo + 4.dp.toPx())
        }
    }
}

// ================================ Barra horizontal ================================

/**
 * Renglón "etiqueta ...... valor" con una barra debajo (lugares, factores...).
 * @param fraccion largo de la barra, de 0 a 1.
 * @param marca opcional: rayita blanca de referencia (por ejemplo, el promedio), de 0 a 1.
 */
@Composable
fun BarraHorizontal(
    etiqueta: String,
    valor: String,
    fraccion: Float,
    modifier: Modifier = Modifier,
    marca: Float? = null,
    color: Color = Acento
) {
    Column(modifier = modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row {
            Text(
                etiqueta,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(valor, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(14.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .offset(y = 3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(VeloMedio)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraccion.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
            }
            if (marca != null) {
                // Rayita de referencia: se ubica con fillMaxWidth(marca) y se dibuja en su borde derecho
                Box(Modifier.fillMaxWidth(marca.coerceIn(0.01f, 1f)).height(14.dp)) {
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.85f))
                    )
                }
            }
        }
    }
}
