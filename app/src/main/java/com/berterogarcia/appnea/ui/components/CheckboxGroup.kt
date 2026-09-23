package com.berterogarcia.appnea.ui.components

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CheckboxGroup(
    titulo: String,
    opciones: List<String>,
    seleccionadas: Set<String>,
    onCambio: (String, Boolean) -> Unit
) {
    Text(
        text = titulo,
        color = Color.White,
        fontWeight = FontWeight.SemiBold
    )
    FlowRow(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
        opciones.forEach { opcion ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = opcion in seleccionadas,
                    onCheckedChange = { checked -> onCambio(opcion, checked) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF16F7FB),
                        uncheckedColor = Color.White.copy(alpha = 0.5f),
                        checkmarkColor = Color(0xFF0A3049)
                    )
                )
                Text(text = opcion, color = Color.White)
            }
        }
    }
}