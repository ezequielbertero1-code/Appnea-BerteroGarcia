package com.berterogarcia.appnea.ui.nuevainmersion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.dominio.modelo.Visibilidad
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.CheckboxGroup
import com.berterogarcia.appnea.ui.components.ChipFiltro
import com.berterogarcia.appnea.ui.components.SelectorSegmentado
import com.berterogarcia.appnea.ui.components.nombre
import com.berterogarcia.appnea.ui.components.resolver
import com.berterogarcia.appnea.ui.theme.TextoSuperficie

// Paso 3: agua, temperaturas y visibilidad.

@Composable
internal fun EtapaCondiciones(uiState: NuevaInmersionUiState, viewModel: NuevaInmersionViewModel) {
    val una = stringResource(R.string.nueva_elegi_una)
    SelectorSegmentado(
        titulo = stringResource(R.string.nueva_tipo_agua), ayuda = una,
        opciones = AGUA_TIPO,
        seleccionada = AGUA_TIPO.firstOrNull { it in uiState.caracteristicasAgua },
        etiqueta = { it },
        onSeleccion = { viewModel.onAguaUnica(AGUA_TIPO, it) },
        permitirNinguna = true
    )
    SelectorSegmentado(
        titulo = stringResource(R.string.nueva_entrada_agua), ayuda = una,
        opciones = AGUA_ENTRADA,
        seleccionada = AGUA_ENTRADA.firstOrNull { it in uiState.caracteristicasAgua },
        etiqueta = { it },
        onSeleccion = { viewModel.onAguaUnica(AGUA_ENTRADA, it) },
        permitirNinguna = true,
        modifier = Modifier.padding(top = 16.dp)
    )
    CheckboxGroup(
        titulo = stringResource(R.string.nueva_estado_mar), opciones = AGUA_MAR,
        seleccionadas = uiState.caracteristicasAgua, onCambio = viewModel::onAguaToggle
    )
    CampoTexto(
        valor = uiState.temperaturaAireC, onValorChange = viewModel::onTempAireChange,
        etiqueta = stringResource(R.string.nueva_temp_aire),
        esError = uiState.errores.containsKey(Campo.TEMP_AIRE), mensajeError = uiState.errores[Campo.TEMP_AIRE]?.resolver() ?: "",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        trailingIcon = { BotonSigno(onClick = viewModel::onAlternarSignoAire) },
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    )
    CampoTexto(
        valor = uiState.temperaturaAguaC, onValorChange = viewModel::onTempAguaChange,
        etiqueta = stringResource(R.string.nueva_temp_agua),
        esError = uiState.errores.containsKey(Campo.TEMP_AGUA), mensajeError = uiState.errores[Campo.TEMP_AGUA]?.resolver() ?: "",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        trailingIcon = { BotonSigno(onClick = viewModel::onAlternarSignoAgua) },
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    )
    Text(
        stringResource(R.string.nueva_visibilidad_escala),
        color = TextoSuperficie, style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp)
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Visibilidad.entries.forEach { v ->
            val elegida = uiState.visibilidad == v
            // Tocar la elegida la desmarca: es opcional
            ChipFiltro(texto = v.nombre(), elegido = elegida, onClick = { viewModel.onVisibilidadElegida(if (elegida) null else v) })
        }
    }
    CampoTexto(
        valor = uiState.visibilidadMetros, onValorChange = viewModel::onVisibilidadChange,
        etiqueta = stringResource(R.string.nueva_visibilidad),
        esError = uiState.errores.containsKey(Campo.VISIBILIDAD), mensajeError = uiState.errores[Campo.VISIBILIDAD]?.resolver() ?: "",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
}
