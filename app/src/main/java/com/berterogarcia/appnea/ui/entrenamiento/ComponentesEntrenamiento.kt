package com.berterogarcia.appnea.ui.entrenamiento

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.Fase
import com.berterogarcia.appnea.data.local.TablaApnea
import com.berterogarcia.appnea.data.local.TipoFase
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorBueno
import com.berterogarcia.appnea.ui.theme.Contorno
import com.berterogarcia.appnea.ui.theme.FondoTarjeta
import com.berterogarcia.appnea.ui.theme.FondoTarjetaResaltada
import com.berterogarcia.appnea.ui.theme.TextoSecundario
import com.berterogarcia.appnea.ui.theme.TextoSobreResaltada
import com.berterogarcia.appnea.ui.theme.TextoTerciario
import com.berterogarcia.appnea.ui.theme.VeloFuerte
import com.berterogarcia.appnea.ui.theme.VeloMedio

// ============================ Textos y colores de las fases ============================

/** Color de cada tipo de fase. Todos salen de la paleta (Color.kt). */
fun colorFase(tipo: TipoFase): Color = when (tipo) {
    TipoFase.PREPARACION -> Contorno   // blanco translúcido
    TipoFase.APNEA -> Acento           // cian
    TipoFase.DESCANSO -> ColorBueno    // verde
}

fun etiquetaFase(tipo: TipoFase): String = when (tipo) {
    TipoFase.PREPARACION -> "Preparación"
    TipoFase.APNEA -> "Apnea"
    TipoFase.DESCANSO -> "Descanso"
}

/** 90 → "1:30" (más corto que formatearDuracion, que da "01:30"). */
fun minSeg(segundos: Int): String {
    val s = segundos.coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

/**
 * Resumen corto y GENÉRICO de una tabla, sirve para predefinidas y personalizadas:
 * - todas las apneas iguales → "4 × 1:30 apnea / 1:00 descanso"
 * - apneas distintas        → "5 apneas · 0:30 a 1:30"
 */
fun resumenTabla(tabla: TablaApnea): String {
    val apneas = tabla.fases.filter { it.tipo == TipoFase.APNEA }.map { it.duracionSegundos }
    val descansos = tabla.fases.filter { it.tipo == TipoFase.DESCANSO }.map { it.duracionSegundos }
    if (apneas.isEmpty()) return "${tabla.fases.size} fases"
    return if (apneas.distinct().size == 1) {
        val base = "${apneas.size} × ${minSeg(apneas.first())} apnea"
        if (descansos.isNotEmpty() && descansos.distinct().size == 1) {
            "$base / ${minSeg(descansos.first())} descanso"
        } else base
    } else {
        "${apneas.size} apneas · ${minSeg(apneas.min())} a ${minSeg(apneas.max())}"
    }
}

// ================================ Tira de fases ================================

/** Barra horizontal con un tramo por fase, proporcional a su duración. */
@Composable
fun TiraFases(fases: List<Fase>, modifier: Modifier = Modifier, alto: Dp = 7.dp) {
    if (fases.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(alto)
            .clip(RoundedCornerShape(alto / 2)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        fases.forEach { fase ->
            Box(
                Modifier
                    .weight(fase.duracionSegundos.coerceAtLeast(1).toFloat())
                    .fillMaxHeight()
                    .background(colorFase(fase.tipo))
            )
        }
    }
}

/** "■ Preparación ■ Apnea ■ Descanso" */
@Composable
fun LeyendaFases(modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TipoFase.entries.forEach { tipo ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colorFase(tipo))
                )
                Spacer(Modifier.width(4.dp))
                Text(etiquetaFase(tipo), style = MaterialTheme.typography.labelSmall, color = TextoSecundario)
            }
        }
    }
}

/**
 * Tira de fases con un marcador ▼ en el punto de la tabla donde vas,
 * y "Transcurrido / Total" abajo.
 */
@Composable
fun LineaTiempoTabla(fases: List<Fase>, transcurridos: Int, total: Int, modifier: Modifier = Modifier) {
    val fraccion = if (total > 0) (transcurridos.toFloat() / total).coerceIn(0f, 1f) else 0f
    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(10.dp)) {
            Canvas(
                Modifier
                    .offset(x = maxWidth * fraccion - 6.dp)
                    .size(width = 12.dp, height = 8.dp)
            ) {
                val triangulo = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2, size.height)
                    close()
                }
                drawPath(triangulo, Color.White)
            }
        }
        TiraFases(fases, alto = 10.dp, modifier = Modifier.padding(top = 2.dp))
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(
                "Transcurrido ${minSeg(transcurridos)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextoSecundario,
                modifier = Modifier.weight(1f)
            )
            Text("Total ${minSeg(total)}", style = MaterialTheme.typography.labelSmall, color = TextoSecundario)
        }
    }
}

// ================================ Tarjeta de tabla ================================

/**
 * Tarjeta GENÉRICA de una tabla de apnea: la usan las predefinidas y "Mis tablas".
 * [onEliminar] es opcional: si viene, aparece el tachito.
 */
@Composable
fun TarjetaTabla(
    tabla: TablaApnea,
    seleccionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEliminar: (() -> Unit)? = null
) {
    val total = tabla.fases.sumOf { it.duracionSegundos }
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionada) FondoTarjetaResaltada else FondoTarjeta,
            contentColor = Color.White
        ),
        border = if (seleccionada) BorderStroke(1.dp, Acento.copy(alpha = 0.55f)) else null
    ) {
        Column(
            Modifier.padding(
                start = 12.dp,
                end = if (onEliminar != null) 4.dp else 12.dp,
                top = 10.dp,
                bottom = 11.dp
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        tabla.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        resumenTabla(tabla),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (seleccionada) TextoSobreResaltada else TextoSecundario,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    minSeg(total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    color = if (seleccionada) Color.White else Acento
                )
                if (onEliminar != null) {
                    IconButton(onClick = onEliminar) {
                        Icon(
                            painterResource(R.drawable.ic_borrar),
                            contentDescription = "Eliminar tabla ${tabla.nombre}",
                            tint = TextoTerciario
                        )
                    }
                }
            }
            TiraFases(
                tabla.fases,
                modifier = Modifier.padding(top = 8.dp, end = if (onEliminar != null) 8.dp else 0.dp)
            )
        }
    }
}

// ================================ Anillo de progreso ================================

/** Anillo con [progreso] de 0 a 1 (arranca arriba, sentido horario) y contenido en el centro. */
@Composable
fun AnilloProgreso(
    progreso: Float,
    color: Color,
    modifier: Modifier = Modifier,
    grosor: Dp = 12.dp,
    contenido: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val trazo = grosor.toPx()
            val esquina = Offset(trazo / 2, trazo / 2)
            val tam = Size(size.width - trazo, size.height - trazo)
            drawArc(VeloFuerte, 0f, 360f, useCenter = false, topLeft = esquina, size = tam, style = Stroke(trazo))
            val p = progreso.coerceIn(0f, 1f)
            if (p > 0f) {
                drawArc(
                    color, -90f, 360f * p, useCenter = false, topLeft = esquina, size = tam,
                    style = Stroke(trazo, cap = StrokeCap.Round)
                )
            }
        }
        contenido()
    }
}

/** Pastilla de color con el nombre y la duración de una fase: "Descanso 1:00". */
@Composable
fun PastillaFase(fase: Fase, modifier: Modifier = Modifier) {
    val color = colorFase(fase.tipo)
    Text(
        "${etiquetaFase(fase.tipo)} ${minSeg(fase.duracionSegundos)}",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Normal,
        color = if (fase.tipo == TipoFase.PREPARACION) Color.White else color,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

// ================================ Botón − / + ================================

/** "− 1:30 +": los botones suman o restan; el texto del medio es el valor. */
@Composable
fun SelectorPaso(
    valor: String,
    onMenos: () -> Unit,
    onMas: () -> Unit,
    modifier: Modifier = Modifier,
    descripcion: String = "duración"
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        BotonPaso("−", "Restar $descripcion", onMenos)
        Text(
            valor,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(52.dp)
        )
        BotonPaso("+", "Sumar $descripcion", onMas)
    }
}

@Composable
private fun BotonPaso(texto: String, descripcion: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(VeloMedio)
            .clickable(onClick = onClick)
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center
    ) {
        Text(texto, style = MaterialTheme.typography.titleMedium, color = Acento)
    }
}
