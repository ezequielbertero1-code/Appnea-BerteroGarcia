package com.berterogarcia.appnea.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.SuperficieTonalAlta
import com.berterogarcia.appnea.ui.theme.TextoSuperficie

/**
 * Selector de UNA opción con menú desplegable expuesto de Material 3 (categoría de equipo,
 * tipo de certificado...). El campo es un [CampoTexto] de solo lectura: mismo estilo, ayuda y
 * error que el resto del formulario.
 *
 * Para que quede claro cuando falta elegir:
 *  - Sin elegir: debajo del campo aparece [ayuda].
 *  - Con error: contorno y etiqueta en rojo con [mensajeError] debajo.
 *  - Cada vez que cambia [intentoGuardado] y hay error, la pantalla scrollea hasta el selector
 *    (el botón Guardar está fijo abajo, así que el selector podía quedar fuera de la vista).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun <T> SelectorOpciones(
    titulo: String,
    opciones: List<T>,
    seleccionada: T?,
    etiqueta: (T) -> String,
    onSeleccion: (T) -> Unit,
    modifier: Modifier = Modifier,
    mensajeError: String? = null,
    ayuda: String? = null,
    intentoGuardado: Int = 0
) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    val traerAVista = remember { BringIntoViewRequester() }

    LaunchedEffect(intentoGuardado) {
        if (intentoGuardado > 0 && mensajeError != null) traerAVista.bringIntoView()
    }

    ExposedDropdownMenuBox(
        expanded = abierto,
        onExpandedChange = { abierto = it },
        modifier = modifier.bringIntoViewRequester(traerAVista)
    ) {
        CampoTexto(
            valor = seleccionada?.let(etiqueta).orEmpty(),
            onValorChange = {},
            etiqueta = titulo,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            esError = mensajeError != null,
            mensajeError = mensajeError.orEmpty(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            ayuda = if (seleccionada == null) ayuda else null,
            soloLectura = true
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { opcion ->
                val elegida = opcion == seleccionada
                DropdownMenuItem(
                    text = { Text(etiqueta(opcion)) },
                    onClick = {
                        onSeleccion(opcion)
                        abierto = false
                    },
                    trailingIcon = if (elegida) {
                        { Icon(Icons.Rounded.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = TextoSuperficie,
                        trailingIconColor = Acento
                    ),
                    modifier = Modifier.background(if (elegida) SuperficieTonalAlta else Color.Transparent),
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
