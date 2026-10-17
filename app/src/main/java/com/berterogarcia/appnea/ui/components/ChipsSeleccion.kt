package com.berterogarcia.appnea.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.ContenedorSecundario
import com.berterogarcia.appnea.ui.theme.ContornoFuerte
import com.berterogarcia.appnea.ui.theme.TextoSobreContenedorSecundario
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante

// FilterChips de Material 3 (estilo "Appnea circular"): esquinas de 8 dp, contorno sin elegir y
// relleno tonal con tilde al elegir. Mismo chip para selección única y múltiple.

/**
 * Chips de selección MÚLTIPLE (factores de la sesión, tipo de buceo...).
 * [onCambio] recibe la opción y si quedó marcada o no.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipsSeleccionMultiple(
    opciones: List<String>,
    seleccionadas: Set<String>,
    onCambio: (opcion: String, marcada: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        opciones.forEach { opcion ->
            val elegida = opcion in seleccionadas
            ChipFiltro(texto = opcion, elegido = elegida, onClick = { onCambio(opcion, !elegida) })
        }
    }
}

/**
 * Chips de selección ÚNICA en una fila con scroll horizontal (filtros de período, tipo...).
 * Lo usan Inmersiones y Estadísticas.
 */
@Composable
fun <T> ChipsSeleccionUnica(
    opciones: List<T>,
    seleccionada: T,
    etiqueta: (T) -> String,
    onSeleccion: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    // Scroll horizontal por si la pantalla es angosta o la letra está agrandada
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        opciones.forEach { opcion ->
            ChipFiltro(
                texto = etiqueta(opcion),
                elegido = opcion == seleccionada,
                onClick = { onSeleccion(opcion) }
            )
        }
    }
}

/** Un FilterChip con el estilo de la app. [conError] pinta el contorno de rojo. */
@Composable
fun ChipFiltro(
    texto: String,
    elegido: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    conError: Boolean = false
) {
    FilterChip(
        selected = elegido,
        onClick = onClick,
        label = { Text(texto) },
        modifier = modifier,
        leadingIcon = if (elegido) {
            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = TextoSuperficieVariante,
            selectedContainerColor = ContenedorSecundario,
            selectedLabelColor = TextoSobreContenedorSecundario,
            selectedLeadingIconColor = TextoSobreContenedorSecundario
        ),
        border = if (conError) {
            BorderStroke(1.dp, ColorError)
        } else {
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = elegido,
                borderColor = ContornoFuerte,
                selectedBorderColor = Color.Transparent,
                borderWidth = 1.dp,
                selectedBorderWidth = 0.dp
            )
        }
    )
}
