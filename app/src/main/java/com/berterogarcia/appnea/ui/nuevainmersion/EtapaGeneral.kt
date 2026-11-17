package com.berterogarcia.appnea.ui.nuevainmersion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.dominio.modelo.TipoInmersion
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.DatePickerField
import com.berterogarcia.appnea.ui.components.SelectorSegmentado
import com.berterogarcia.appnea.ui.components.TimePickerField
import com.berterogarcia.appnea.ui.components.nombre
import com.berterogarcia.appnea.ui.components.resolver

// Paso 1 del formulario de inmersión: tipo, fecha y lugar.

@Composable
internal fun EtapaGeneral(
    uiState: NuevaInmersionUiState,
    viewModel: NuevaInmersionViewModel,
    onAbrirMapa: () -> Unit
) {
    val tipos = TipoInmersion.entries.associateWith { it.nombre() }
    SelectorSegmentado(
        titulo = stringResource(R.string.nueva_que_tipo),
        opciones = TipoInmersion.entries,
        seleccionada = uiState.tipo,
        etiqueta = { tipos.getValue(it) },
        onSeleccion = { tipo -> tipo?.let(viewModel::onTipoChange) },
        modifier = Modifier.padding(bottom = 12.dp)
    )
    DatePickerField(
        label = stringResource(R.string.nueva_fecha), valor = uiState.fecha, onValorChange = viewModel::onFechaChange,
        isError = uiState.errores.containsKey(Campo.FECHA), mensajeError = uiState.errores[Campo.FECHA]?.resolver(),
        modifier = Modifier.fillMaxWidth()
    )
    CampoTexto(
        valor = uiState.lugar, onValorChange = viewModel::onLugarChange, etiqueta = stringResource(R.string.nueva_lugar),
        esError = uiState.errores.containsKey(Campo.LUGAR), mensajeError = uiState.errores[Campo.LUGAR]?.resolver() ?: "",
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
    TarjetaLugar(
        latitud = uiState.latitud,
        longitud = uiState.longitud,
        buscando = uiState.buscandoUbicacion,
        aviso = uiState.avisoUbicacion?.resolver(),
        onAbrirMapa = onAbrirMapa,
        onMiUbicacion = viewModel::usarMiUbicacion,
        onPermisoDenegado = viewModel::permisoUbicacionDenegado,
        onQuitar = viewModel::quitarUbicacion
    )
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TimePickerField(
            label = stringResource(R.string.nueva_entrada), valor = uiState.horaEntrada,
            onValorChange = viewModel::onHoraEntradaChange,
            isError = uiState.errores.containsKey(Campo.HORA_ENTRADA), mensajeError = uiState.errores[Campo.HORA_ENTRADA]?.resolver(),
            modifier = Modifier.weight(1f)
        )
        TimePickerField(
            label = stringResource(R.string.nueva_salida), valor = uiState.horaSalida,
            onValorChange = viewModel::onHoraSalidaChange,
            isError = uiState.errores.containsKey(Campo.HORA_SALIDA), mensajeError = uiState.errores[Campo.HORA_SALIDA]?.resolver(),
            modifier = Modifier.weight(1f)
        )
    }
}
