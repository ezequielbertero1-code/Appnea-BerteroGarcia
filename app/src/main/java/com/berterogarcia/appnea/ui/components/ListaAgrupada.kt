package com.berterogarcia.appnea.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorMalo
import com.berterogarcia.appnea.ui.theme.ColorRegular
import com.berterogarcia.appnea.ui.theme.ContenedorApnea
import com.berterogarcia.appnea.ui.theme.ContenedorSecundario
import com.berterogarcia.appnea.ui.theme.ContornoFuerte
import com.berterogarcia.appnea.ui.theme.SuperficieTonal
import com.berterogarcia.appnea.ui.theme.SuperficieTonalMaxima
import com.berterogarcia.appnea.ui.theme.TextoSobreAcentoFuerte
import com.berterogarcia.appnea.ui.theme.TextoSobreContenedorApnea
import com.berterogarcia.appnea.ui.theme.TextoSobreContenedorSecundario
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ============================ Listas agrupadas (estilo Android 16) ============================
// Filas separadas por 2 dp, con esquinas grandes solo arriba de la primera y abajo de la última.
// Se usan en Inicio y quedan listas para Inmersiones, Cuenta, Equipamiento, etc.

private val RADIO_EXTERNO = 20.dp
private val RADIO_INTERNO = 4.dp

private val LOCALE_ES: Locale = Locale.forLanguageTag("es-AR")
private val FORMATO_MES_CORTO: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", LOCALE_ES)

/** Forma de la fila [indice] dentro de un grupo de [total] filas. */
fun formaEnGrupo(indice: Int, total: Int): RoundedCornerShape {
    val arriba = if (indice == 0) RADIO_EXTERNO else RADIO_INTERNO
    val abajo = if (indice >= total - 1) RADIO_EXTERNO else RADIO_INTERNO
    return RoundedCornerShape(topStart = arriba, topEnd = arriba, bottomStart = abajo, bottomEnd = abajo)
}

/** Contenedor de un grupo de [FilaLista]. */
@Composable
fun GrupoLista(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = contenido
    )
}

/**
 * Una fila de lista de Material 3 (ListItem) con la forma que le toca en el grupo.
 * Si [onClick] es null la fila no es clickeable. [colorTitulo] permite filas destructivas en rojo.
 */
@Composable
fun FilaLista(
    titulo: String,
    indice: Int,
    total: Int,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    onClick: (() -> Unit)? = null,
    inicio: (@Composable () -> Unit)? = null,
    fin: (@Composable () -> Unit)? = null,
    colorTitulo: Color = TextoSuperficie
) {
    ListItem(
        headlineContent = { Text(titulo, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = subtitulo?.let { texto ->
            { Text(texto, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        leadingContent = inicio,
        trailingContent = fin,
        colors = coloresFila(colorTitulo),
        modifier = modifier
            .clip(formaEnGrupo(indice, total))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    )
}

@Composable
private fun coloresFila(colorTitulo: Color = TextoSuperficie): ListItemColors = ListItemDefaults.colors(
    containerColor = SuperficieTonal,
    headlineColor = colorTitulo,
    supportingColor = TextoSuperficieVariante,
    leadingIconColor = TextoSuperficieVariante,
    trailingIconColor = TextoSuperficieVariante
)

/**
 * Fila de un grupo que se despliega (secciones de Aprender): al tocarla muestra [detalle] debajo,
 * dentro de la misma superficie, y la flecha cambia de sentido. TalkBack anuncia "expandir/contraer".
 */
@Composable
fun FilaListaExpandible(
    titulo: String,
    indice: Int,
    total: Int,
    expandida: Boolean,
    onCambio: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    inicio: (@Composable () -> Unit)? = null,
    detalle: @Composable ColumnScope.() -> Unit
) {
    val accion = stringResource(if (expandida) R.string.accion_contraer else R.string.accion_expandir)
    Column(
        modifier = modifier
            .clip(formaEnGrupo(indice, total))
            .background(SuperficieTonal)
    ) {
        ListItem(
            headlineContent = { Text(titulo, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = subtitulo?.let { texto ->
                { Text(texto, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            },
            leadingContent = inicio,
            trailingContent = {
                Icon(
                    if (expandida) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = accion
                )
            },
            // La superficie la pone la columna, así fila y detalle se ven como una sola pieza
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = TextoSuperficie,
                supportingColor = TextoSuperficieVariante,
                leadingIconColor = TextoSuperficieVariante,
                trailingIconColor = TextoSuperficieVariante
            ),
            modifier = Modifier.clickable(onClickLabel = accion) { onCambio(!expandida) }
        )
        AnimatedVisibility(visible = expandida, enter = expandVertically(), exit = shrinkVertically()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                content = detalle
            )
        }
    }
}

/**
 * Fila con un Switch a la derecha ("Estoy acompañado", "En uso"). Toda la fila es tocable y
 * TalkBack la anuncia como un interruptor, no como dos elementos separados.
 */
@Composable
fun FilaConSwitch(
    titulo: String,
    activado: Boolean,
    onCambio: (Boolean) -> Unit,
    indice: Int,
    total: Int,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    inicio: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    ListItem(
        headlineContent = { Text(titulo, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        // El subtítulo puede ocupar dos líneas: suele ser una aclaración ("Nunca entrenes apnea solo")
        supportingContent = subtitulo?.let { texto -> { Text(texto, maxLines = 2, overflow = TextOverflow.Ellipsis) } },
        leadingContent = inicio,
        trailingContent = {
            Switch(
                checked = activado,
                onCheckedChange = null, // lo maneja la fila entera (toggleable)
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextoSobreAcentoFuerte,
                    checkedTrackColor = Acento,
                    checkedBorderColor = Acento,
                    uncheckedThumbColor = ContornoFuerte,
                    uncheckedTrackColor = SuperficieTonalMaxima,
                    uncheckedBorderColor = ContornoFuerte
                )
            )
        },
        colors = coloresFila(),
        modifier = modifier
            .clip(formaEnGrupo(indice, total))
            .toggleable(value = activado, enabled = enabled, role = Role.Switch, onValueChange = onCambio)
    )
}

/** Encabezado de un grupo: título chico en cian y, opcional, un botón de texto a la derecha. */
@Composable
fun EncabezadoGrupo(
    titulo: String,
    modifier: Modifier = Modifier,
    accion: String? = null,
    onAccion: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = 4.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            titulo,
            style = MaterialTheme.typography.titleSmall,
            color = Acento,
            modifier = Modifier.weight(1f)
        )
        if (accion != null) {
            TextButton(onClick = onAccion) { Text(accion, color = Acento) }
        }
    }
}

/** Fecha en un cuadrado redondeado (día + mes). Apnea usa otro tono para distinguirse de buceo. */
@Composable
fun FechaEnCuadro(fecha: LocalDate, esApnea: Boolean, modifier: Modifier = Modifier) {
    val fondo = if (esApnea) ContenedorApnea else ContenedorSecundario
    val texto = if (esApnea) TextoSobreContenedorApnea else TextoSobreContenedorSecundario
    Column(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(fondo),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "%02d".format(fecha.dayOfMonth),
            color = texto,
            fontSize = 15.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            // "oct." → "OCT" (según la versión de Android el mes abreviado puede venir con punto)
            fecha.format(FORMATO_MES_CORTO).removeSuffix(".").uppercase(LOCALE_ES),
            color = texto,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            letterSpacing = 0.6.sp
        )
    }
}

/** Tono del círculo de [IconoCirculo]: normal, apnea (verde agua), error (rojo) o advertencia (amarillo). */
enum class TonoIcono { NORMAL, APNEA, ERROR, ADVERTENCIA }

/** Ícono dentro de un círculo de 40 dp, para el inicio de una [FilaLista] (Cuenta, Aprender, Perfil). */
@Composable
fun IconoCirculo(icono: Painter, modifier: Modifier = Modifier, tono: TonoIcono = TonoIcono.NORMAL) {
    val (fondo, color) = when (tono) {
        TonoIcono.NORMAL -> ContenedorSecundario to TextoSobreContenedorSecundario
        TonoIcono.APNEA -> ContenedorApnea to TextoSobreContenedorApnea
        TonoIcono.ERROR -> ColorMalo.copy(alpha = 0.18f) to ColorMalo
        TonoIcono.ADVERTENCIA -> ColorRegular.copy(alpha = 0.16f) to ColorRegular
    }
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(fondo),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
    }
}
