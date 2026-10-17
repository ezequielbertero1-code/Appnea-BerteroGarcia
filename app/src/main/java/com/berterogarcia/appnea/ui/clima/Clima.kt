package com.berterogarcia.appnea.ui.clima

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.UBICACION_COMODORO_RIVADAVIA
import com.berterogarcia.appnea.ui.components.ChipFiltro
import com.berterogarcia.appnea.ui.components.resolver
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.TextoSuperficie

/**
 * Lugar de las condiciones (arriba de la pantalla de Condiciones): nombre del lugar y chips para
 * usar el GPS o Comodoro Rivadavia. El chip del lugar en uso queda marcado.
 */
@Composable
fun SelectorUbicacion(viewModel: ClimaViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val enComodoro = uiState.ubicacion == UBICACION_COMODORO_RIVADAVIA

    val lanzadorPermiso = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        if (resultado.values.any { it }) viewModel.usarMiUbicacion() else viewModel.permisoDenegado()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = Acento, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (uiState.buscandoUbicacion) stringResource(R.string.clima_buscando_ubicacion)
                else uiState.ubicacion.nombre
                    ?: stringResource(if (enComodoro) R.string.clima_ubicacion_seleccionada else R.string.lugar_mi_ubicacion),
                style = MaterialTheme.typography.titleSmall,
                color = TextoSuperficie,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChipFiltro(
                texto = stringResource(R.string.lugar_mi_ubicacion),
                elegido = !enComodoro,
                onClick = {
                    if (!uiState.cargando) lanzadorPermiso.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )
            ChipFiltro(
                texto = stringResource(R.string.clima_comodoro),
                elegido = enComodoro,
                onClick = { if (!uiState.cargando) viewModel.usarComodoroRivadavia() }
            )
        }

        // Avisos cuando YA hay datos en pantalla (ej.: falló el GPS y se volvió a Comodoro).
        // Sin datos, el error ya lo muestra la pantalla de Condiciones.
        if (uiState.condiciones != null) {
            uiState.error?.let {
                Text(
                    it.resolver(),
                    color = ColorError,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (uiState.cargando) {
                LinearProgressIndicator(color = Acento, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }
        }
    }
}
