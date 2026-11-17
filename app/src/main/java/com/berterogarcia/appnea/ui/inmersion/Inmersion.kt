package com.berterogarcia.appnea.ui.inmersion

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.berterogarcia.appnea.ui.historial.HistorialScreen
import com.berterogarcia.appnea.ui.historial.HistorialViewModel

/**
 * Ambiente "Inmersión": bitácora de buceo y apnea (sin pestañas: pesca submarina se sacó).
 */
@Composable
fun InmersionScreen(
    historialViewModel: HistorialViewModel,
    onAgregarInmersion: () -> Unit,
    onVerDetalleInmersion: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onVerEstadisticas: () -> Unit = {}
) {
    HistorialScreen(
        modifier = modifier.fillMaxSize(),
        viewModel = historialViewModel,
        onAgregarClick = onAgregarInmersion,
        onVerDetalle = onVerDetalleInmersion,
        onVerEstadisticas = onVerEstadisticas
    )
}
