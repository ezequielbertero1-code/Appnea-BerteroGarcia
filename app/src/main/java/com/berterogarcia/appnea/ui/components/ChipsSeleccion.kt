package com.berterogarcia.appnea.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.Divisor
import com.berterogarcia.appnea.ui.theme.FondoTarjetaResaltada
import com.berterogarcia.appnea.ui.theme.TextoChip

/**
 * Chips de selección MÚLTIPLE (mismo estilo que los filtros de Inmersiones).
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
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        opciones.forEach { opcion ->
            val elegida = opcion in seleccionadas
            FilterChip(
                selected = elegida,
                onClick = { onCambio(opcion, !elegida) },
                label = { Text(opcion, fontWeight = if (elegida) FontWeight.SemiBold else FontWeight.Normal) },
                leadingIcon = if (elegida) {
                    { Icon(Icons.Filled.Check, contentDescription = null, tint = Acento, modifier = Modifier.size(16.dp)) }
                } else {
                    null
                },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = TextoChip,
                    selectedContainerColor = FondoTarjetaResaltada,
                    selectedLabelColor = Color.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = elegida,
                    borderColor = Divisor,
                    selectedBorderColor = Acento.copy(alpha = 0.55f),
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.dp
                )
            )
        }
    }
}
