package com.berterogarcia.appnea.ui.nuevainmersion

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante

// Paso 5: actividad y comentarios.

@Composable
internal fun EtapaNotas(uiState: NuevaInmersionUiState, onEvento: (EventoFormulario) -> Unit) {
    CampoTexto(
        valor = uiState.actividad,
        onValorChange = { onEvento(EventoFormulario.Actividad(it)) },
        etiqueta = stringResource(R.string.nueva_actividad), minLines = 3,
        modifier = Modifier.fillMaxWidth()
    )
    CampoTexto(
        valor = uiState.comentarios,
        onValorChange = { onEvento(EventoFormulario.Comentarios(it)) },
        etiqueta = stringResource(R.string.nueva_comentarios),
        minLines = 3,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    )
    Text(
        stringResource(R.string.nueva_condiciones_automaticas),
        style = MaterialTheme.typography.bodySmall,
        color = TextoSuperficieVariante,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}
