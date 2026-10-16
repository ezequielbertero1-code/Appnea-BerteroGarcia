package com.berterogarcia.appnea.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.FondoTarjetaResaltada
import com.berterogarcia.appnea.ui.theme.TextoChip
import com.berterogarcia.appnea.ui.theme.TextoTerciario
import com.berterogarcia.appnea.ui.theme.VeloMedio

private val FormaExterior = RoundedCornerShape(14.dp)
private val FormaOpcion = RoundedCornerShape(11.dp)

/**
 * Selector de UNA sola opción con botones pegados: Buceo | Apnea, Dulce | Salada...
 * Para TalkBack se anuncia como un grupo de radio buttons.
 *
 * [permitirNinguna]: si es true, tocar la opción elegida la desmarca (campos opcionales).
 */
@Composable
fun <T> SelectorSegmentado(
    opciones: List<T>,
    seleccionada: T?,
    etiqueta: (T) -> String,
    onSeleccion: (T?) -> Unit,
    modifier: Modifier = Modifier,
    titulo: String? = null,
    ayuda: String? = null,
    permitirNinguna: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (titulo != null) {
            Row {
                Text(titulo, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                if (ayuda != null) {
                    Text(" · $ayuda", color = TextoTerciario, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .background(VeloMedio, FormaExterior)
                .padding(4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            opciones.forEach { opcion ->
                val elegida = opcion == seleccionada
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)   // área táctil mínima recomendada
                        .clip(FormaOpcion)
                        .then(
                            if (elegida) Modifier
                                .background(FondoTarjetaResaltada)
                                .border(1.dp, Acento.copy(alpha = 0.55f), FormaOpcion)
                            else Modifier
                        )
                        .selectable(
                            selected = elegida,
                            role = Role.RadioButton,
                            onClick = {
                                when {
                                    !elegida -> onSeleccion(opcion)
                                    permitirNinguna -> onSeleccion(null)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = etiqueta(opcion),
                        color = if (elegida) Color.White else TextoChip,
                        fontWeight = FontWeight.Normal,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}