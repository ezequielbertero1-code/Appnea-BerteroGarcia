package com.berterogarcia.appnea.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val RojoError = Color(0xFFFF6B6B)
private val Celeste = Color(0xFF16F7FB)

/**
 * Selector de UNA opción con chips (categoría de equipo, tipo de certificado...).
 * Se eligió chips en vez de un dropdown porque la API de ExposedDropdownMenu cambió
 * varias veces entre versiones de Material3 y FilterChip es estable.
 *
 * Para que quede claro cuando falta elegir:
 *  - Sin elegir: debajo del título aparece [ayuda] en gris.
 *  - Con error: título en rojo, ícono de advertencia + mensaje, y todos los chips con borde rojo.
 *  - Cada vez que cambia [intentoGuardado] y hay error, la pantalla scrollea hasta el selector
 *    (el botón Guardar está fijo abajo, así que el selector podía quedar fuera de la vista).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun <T> SelectorOpciones(
    titulo: String,
    opciones: List<T>,
    seleccionada: T?,
    etiqueta: (T) -> String,
    onSeleccion: (T) -> Unit,
    modifier: Modifier = Modifier,
    mensajeError: String? = null,
    ayuda: String? = "Tocá una opción para elegirla.",
    intentoGuardado: Int = 0
) {
    val hayError = mensajeError != null
    val traerAVista = remember { BringIntoViewRequester() }

    LaunchedEffect(intentoGuardado) {
        if (intentoGuardado > 0 && mensajeError != null) traerAVista.bringIntoView()
    }

    Column(modifier = modifier.bringIntoViewRequester(traerAVista)) {
        Text(
            text = titulo,
            color = if (hayError) RojoError else Color.White,
            fontWeight = FontWeight.SemiBold
        )

        when {
            mensajeError != null -> Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = RojoError,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(mensajeError, color = RojoError, style = MaterialTheme.typography.bodySmall)
            }
            seleccionada == null && ayuda != null -> Text(
                text = ayuda,
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        FlowRow(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            opciones.forEach { opcion ->
                val elegida = opcion == seleccionada
                FilterChip(
                    selected = elegida,
                    onClick = { onSeleccion(opcion) },
                    label = { Text(etiqueta(opcion)) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White.copy(alpha = 0.06f),
                        labelColor = Color.White,
                        selectedContainerColor = Celeste.copy(alpha = 0.20f),
                        selectedLabelColor = Celeste
                    ),
                    border = if (hayError) {
                        BorderStroke(1.dp, RojoError)
                    } else {
                        FilterChipDefaults.filterChipBorder(enabled = true, selected = elegida)
                    }
                )
            }
        }
    }
}
