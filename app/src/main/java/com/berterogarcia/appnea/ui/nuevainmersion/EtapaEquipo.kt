package com.berterogarcia.appnea.ui.nuevainmersion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.EquipoEntity
import com.berterogarcia.appnea.ui.components.BotonSecundario
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.GrupoLista
import com.berterogarcia.appnea.ui.components.SelectorSegmentado
import com.berterogarcia.appnea.ui.components.formaEnGrupo
import com.berterogarcia.appnea.ui.components.nombre
import com.berterogarcia.appnea.ui.components.resolver
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.SuperficieTonal
import com.berterogarcia.appnea.ui.theme.TextoSobreAcentoFuerte
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante

// Paso 4: equipo usado (del perfil), lastre y protección.

@Composable
internal fun EtapaEquipo(
    uiState: NuevaInmersionUiState,
    viewModel: NuevaInmersionViewModel,
    onCargarEquipo: () -> Unit
) {
    Row(Modifier.padding(start = 4.dp)) {
        Text(stringResource(R.string.nueva_que_equipo), color = TextoSuperficie, style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.nueva_de_tu_perfil), color = TextoSuperficieVariante, style = MaterialTheme.typography.bodyMedium)
    }
    when {
        !uiState.equipoCargado -> LinearProgressIndicator(
            color = Acento,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        )
        uiState.equipoVisible.isEmpty() -> SinEquipo(
            perfilVacio = uiState.equipoPerfil.isEmpty(),
            onCargarEquipo = onCargarEquipo
        )
        else -> {
            uiState.equipoVisible
                .groupBy { it.categoria }
                .toSortedMap()   // en el orden del enum: Máscara, Snorkel, Aletas, Traje...
                .forEach { (categoria, equipos) ->
                    Text(
                        categoria.nombre(),
                        color = Acento,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 14.dp, start = 4.dp, bottom = 6.dp)
                    )
                    GrupoLista {
                        equipos.forEachIndexed { i, equipo ->
                            val etiqueta = equipo.etiquetaInmersion
                            FilaEquipo(
                                equipo = equipo,
                                marcado = etiqueta in uiState.equipamiento,
                                onCambio = { viewModel.onEquipamientoToggle(etiqueta, it) },
                                indice = i,
                                total = equipos.size
                            )
                        }
                    }
                }
            TextButton(onClick = onCargarEquipo, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = Acento, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.nueva_agregar_equipo), color = Acento, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
    PreguntaExtra(
        pregunta = stringResource(R.string.nueva_usaste_lastre),
        usado = uiState.usoLastre, onUsadoChange = viewModel::onUsoLastreChange,
        valor = uiState.lastreKg, onValorChange = viewModel::onLastreChange,
        etiqueta = stringResource(R.string.nueva_lastre),
        error = uiState.errores[Campo.LASTRE]?.resolver()
    )
    PreguntaExtra(
        pregunta = stringResource(R.string.nueva_usaste_proteccion),
        usado = uiState.usoProteccion, onUsadoChange = viewModel::onUsoProteccionChange,
        valor = uiState.proteccionMm, onValorChange = viewModel::onProteccionMmChange,
        etiqueta = stringResource(R.string.nueva_proteccion),
        error = uiState.errores[Campo.PROTECCION]?.resolver()
    )
}

/** Pregunta Sí/No; si es Sí aparece el campo del valor (obligatorio). No = no se guarda nada. */
@Composable
internal fun PreguntaExtra(
    pregunta: String,
    usado: Boolean,
    onUsadoChange: (Boolean) -> Unit,
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    error: String?
) {
    val si = stringResource(R.string.nueva_si)
    val no = stringResource(R.string.nueva_no)
    SelectorSegmentado(
        titulo = pregunta,
        ayuda = stringResource(R.string.nueva_ademas_perfil),
        opciones = listOf(false, true),
        seleccionada = usado,
        etiqueta = { if (it) si else no },
        onSeleccion = { onUsadoChange(it ?: false) },
        modifier = Modifier.padding(top = 16.dp)
    )
    if (usado) {
        CampoTexto(
            valor = valor, onValorChange = onValorChange, etiqueta = etiqueta,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            esError = error != null, mensajeError = error ?: "",
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}

/** Toda la fila se toca (no solo el checkbox) y TalkBack la anuncia como checkbox. */
@Composable
internal fun FilaEquipo(equipo: EquipoEntity, marcado: Boolean, onCambio: (Boolean) -> Unit, indice: Int, total: Int) {
    val nombre = listOf(equipo.marca, equipo.modelo)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { equipo.categoria.nombre() }
    ListItem(
        headlineContent = { Text(nombre, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = equipo.talle.takeIf { it.isNotBlank() }?.let { talle -> { Text(talle) } },
        leadingContent = {
            Checkbox(
                checked = marcado,
                onCheckedChange = null, // lo maneja el toggleable de la fila
                colors = CheckboxDefaults.colors(
                    checkedColor = Acento,
                    uncheckedColor = TextoSuperficieVariante,
                    checkmarkColor = TextoSobreAcentoFuerte
                )
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = SuperficieTonal,
            headlineColor = TextoSuperficie,
            supportingColor = TextoSuperficieVariante
        ),
        modifier = Modifier
            .clip(formaEnGrupo(indice, total))
            .toggleable(value = marcado, role = Role.Checkbox, onValueChange = onCambio)
    )
}

@Composable
internal fun SinEquipo(perfilVacio: Boolean, onCargarEquipo: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(if (perfilVacio) R.string.nueva_sin_equipo_perfil else R.string.nueva_sin_equipo_tipo),
            color = TextoSuperficieVariante,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        BotonSecundario(
            texto = stringResource(R.string.nueva_cargar_equipo),
            onClick = onCargarEquipo,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
