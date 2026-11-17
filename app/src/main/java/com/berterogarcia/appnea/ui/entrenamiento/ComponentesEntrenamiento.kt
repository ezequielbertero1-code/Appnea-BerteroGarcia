package com.berterogarcia.appnea.ui.entrenamiento

import androidx.compose.ui.res.pluralStringResource
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.dominio.modelo.Fase
import com.berterogarcia.appnea.dominio.modelo.TablaApnea
import com.berterogarcia.appnea.dominio.modelo.TipoFase
import com.berterogarcia.appnea.ui.components.formaEnGrupo
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorEntrenaApnea
import com.berterogarcia.appnea.ui.theme.ColorEntrenaDescanso
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.Contorno
import com.berterogarcia.appnea.ui.theme.SuperficieTonal
import com.berterogarcia.appnea.ui.theme.SuperficieTonalAlta
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante

// ============================ Textos, íconos y colores de las fases ============================

/** Color de cada tipo de fase. Todos salen de la paleta (Color.kt). */
fun colorFase(tipo: TipoFase): Color = when (tipo) {
    TipoFase.PREPARACION -> Contorno   // blanco translúcido
    TipoFase.APNEA -> ColorEntrenaApnea       // celeste
    TipoFase.DESCANSO -> ColorEntrenaDescanso // violeta
}

@Composable
fun etiquetaFase(tipo: TipoFase): String = stringResource(
    when (tipo) {
        TipoFase.PREPARACION -> R.string.fase_preparacion
        TipoFase.APNEA -> R.string.fase_apnea
        TipoFase.DESCANSO -> R.string.fase_descanso
    }
)

/** Ícono de cada fase (Material Symbols): relajación, aire, acostado. */
@DrawableRes
fun iconoFase(tipo: TipoFase): Int = when (tipo) {
    TipoFase.PREPARACION -> R.drawable.ic_ms_self_improvement
    TipoFase.APNEA -> R.drawable.ic_ms_air
    TipoFase.DESCANSO -> R.drawable.ic_ms_airline_seat_flat
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
@Composable
fun resumenTabla(tabla: TablaApnea): String {
    val apneas = tabla.fases.filter { it.tipo == TipoFase.APNEA }.map { it.duracionSegundos }
    val descansos = tabla.fases.filter { it.tipo == TipoFase.DESCANSO }.map { it.duracionSegundos }
    if (apneas.isEmpty()) return pluralStringResource(R.plurals.tabla_resumen_fases, tabla.fases.size, tabla.fases.size)
    return if (apneas.distinct().size == 1) {
        if (descansos.isNotEmpty() && descansos.distinct().size == 1) {
            stringResource(R.string.tabla_resumen_fijo_descanso, apneas.size, minSeg(apneas.first()), minSeg(descansos.first()))
        } else {
            stringResource(R.string.tabla_resumen_fijo, apneas.size, minSeg(apneas.first()))
        }
    } else {
        pluralStringResource(R.plurals.tabla_resumen_variable, apneas.size, apneas.size, minSeg(apneas.min()), minSeg(apneas.max()))
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
                drawPath(triangulo, TextoSuperficie)
            }
        }
        TiraFases(fases, alto = 10.dp, modifier = Modifier.padding(top = 2.dp))
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(
                stringResource(R.string.tabla_transcurrido, minSeg(transcurridos)),
                style = MaterialTheme.typography.labelSmall,
                color = TextoSuperficieVariante,
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(R.string.tabla_total, minSeg(total)),
                style = MaterialTheme.typography.labelSmall,
                color = TextoSuperficieVariante
            )
        }
    }
}

// ================================ Fila de tabla ================================

/**
 * Fila de una tabla de apnea dentro de un grupo (predefinidas y "Mis tablas"): radio a la izquierda,
 * nombre y resumen al medio, duración total a la derecha. [onEliminar] opcional: agrega un menú ⋮.
 */
@Composable
fun FilaTabla(
    tabla: TablaApnea,
    seleccionada: Boolean,
    onClick: () -> Unit,
    indice: Int,
    total: Int,
    modifier: Modifier = Modifier,
    onEliminar: (() -> Unit)? = null
) {
    val duracion = minSeg(tabla.fases.sumOf { it.duracionSegundos })
    ListItem(
        headlineContent = { Text(tabla.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                stringResource(R.string.tabla_fila_subtitulo, resumenTabla(tabla), duracion),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            RadioButton(
                selected = seleccionada,
                onClick = null, // lo maneja la fila entera (selectable)
                colors = RadioButtonDefaults.colors(selectedColor = Acento, unselectedColor = TextoSuperficieVariante)
            )
        },
        trailingContent = onEliminar?.let { eliminar -> { MenuTabla(nombre = tabla.nombre, onEliminar = eliminar) } },
        colors = ListItemDefaults.colors(
            containerColor = SuperficieTonal,
            headlineColor = TextoSuperficie,
            supportingColor = TextoSuperficieVariante,
            trailingIconColor = TextoSuperficieVariante
        ),
        modifier = modifier
            .clip(formaEnGrupo(indice, total))
            .selectable(selected = seleccionada, role = Role.RadioButton, onClick = onClick)
    )
}

/** Menú ⋮ de una tabla propia: por ahora solo "Eliminar" (en rojo, con confirmación aparte). */
@Composable
private fun MenuTabla(nombre: String, onEliminar: () -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.tabla_opciones, nombre))
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.tabla_eliminar)) },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                onClick = {
                    abierto = false
                    onEliminar()
                },
                colors = MenuDefaults.itemColors(textColor = ColorError, leadingIconColor = ColorError)
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
            drawArc(SuperficieTonalAlta, 0f, 360f, useCenter = false, topLeft = esquina, size = tam, style = Stroke(trazo))
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
        fontWeight = FontWeight.Medium,
        color = if (fase.tipo == TipoFase.PREPARACION) TextoSuperficie else color,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
