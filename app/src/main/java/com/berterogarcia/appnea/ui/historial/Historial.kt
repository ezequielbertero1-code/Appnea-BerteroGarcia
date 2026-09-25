package com.berterogarcia.appnea.ui.historial

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.data.local.InmersionEntity

@Composable
fun HistorialScreen(
    modifier: Modifier = Modifier,
    viewModel: HistorialViewModel,
    onAgregarClick: () -> Unit
) {
    val inmersiones by viewModel.inmersiones.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = onAgregarClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Agregar nueva inmersión")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        if (inmersiones.isEmpty()) {
            Text(
                text = "Todavía no cargaste ninguna inmersión de buceo o apnea.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        LazyColumn {
            items(inmersiones) { inmersion ->
                InmersionCard(inmersion)
            }
        }
    }
}

@Composable
private fun InmersionCard(inmersion: InmersionEntity) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${inmersion.fecha} — ${inmersion.lugar}", fontWeight = FontWeight.Bold)
            if (inmersion.actividad.isNotBlank()) {
                Text(inmersion.actividad, style = MaterialTheme.typography.bodySmall)
            }
            Text("Profundidad: ${inmersion.profundidadMetros} m")
            Text("Fondo: ${inmersion.tiempoFondo}")
        }
    }
}