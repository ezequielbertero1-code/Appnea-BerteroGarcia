package com.berterogarcia.appnea.ui.nuevainmersion

import com.berterogarcia.appnea.ui.components.ChipsSeleccionMultiple
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.ui.theme.TextoSuperficie

/**
 * Selección MÚLTIPLE con título (estado del mar...). En el estilo "Appnea circular" la selección
 * múltiple se hace con FilterChips en vez de checkboxes sueltos: ver [ChipsSeleccionMultiple].
 */
@Composable
fun CheckboxGroup(
    titulo: String,
    opciones: List<String>,
    seleccionadas: Set<String>,
    onCambio: (String, Boolean) -> Unit
) {
    Text(
        text = titulo,
        color = TextoSuperficie,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
    )
    ChipsSeleccionMultiple(
        opciones = opciones,
        seleccionadas = seleccionadas,
        onCambio = onCambio,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
