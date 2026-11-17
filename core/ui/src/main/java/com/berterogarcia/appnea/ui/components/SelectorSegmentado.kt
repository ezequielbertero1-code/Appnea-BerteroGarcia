package com.berterogarcia.appnea.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.ui.theme.ContenedorSecundario
import com.berterogarcia.appnea.ui.theme.ContornoFuerte
import com.berterogarcia.appnea.ui.theme.TextoDeshabilitado
import com.berterogarcia.appnea.ui.theme.TextoSobreContenedorSecundario
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante

/**
 * Selector de UNA sola opción con botones segmentados de Material 3: Buceo | Apnea, Tablas | Libre...
 * La opción elegida lleva relleno tonal y tilde. Para TalkBack se anuncia como grupo de radio buttons.
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
            Row(modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)) {
                Text(titulo, color = TextoSuperficie, style = MaterialTheme.typography.titleSmall)
                if (ayuda != null) {
                    Text(" · $ayuda", color = TextoSuperficieVariante, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            opciones.forEachIndexed { indice, opcion ->
                val elegida = opcion == seleccionada
                SegmentedButton(
                    selected = elegida,
                    onClick = {
                        when {
                            !elegida -> onSeleccion(opcion)
                            permitirNinguna -> onSeleccion(null)
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = indice, count = opciones.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = ContenedorSecundario,
                        activeContentColor = TextoSobreContenedorSecundario,
                        activeBorderColor = ContornoFuerte,
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = TextoSuperficie,
                        inactiveBorderColor = ContornoFuerte,
                        disabledInactiveContentColor = TextoDeshabilitado
                    )
                ) {
                    Text(etiqueta(opcion), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
