package com.berterogarcia.appnea.ui.nuevainmersion

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.berterogarcia.appnea.dominio.modelo.TipoInmersion
import com.berterogarcia.appnea.dominio.modelo.Visibilidad
import com.berterogarcia.appnea.ui.components.VistaPrevia

// Las etapas reciben el estado y un onEvento (no el ViewModel), así se pueden ver sin datos reales.
// La etapa General no tiene vista previa: pide permisos de ubicación, que el editor no simula.

@Preview(name = "Etapa Inmersión · apnea con detalle", widthDp = 360, heightDp = 900)
@Composable
private fun VistaEtapaApnea() = VistaPrevia {
    Column {
        EtapaInmersion(
            NuevaInmersionUiState(
                tipo = TipoInmersion.APNEA,
                cantidadBajadas = "2",
                cargarDetalleBajadas = true,
                bajadas = listOf(BajadaForm(profundidad = "12", tiempo = "0045"), BajadaForm(profundidad = "15", tiempo = ""))
            ),
            onEvento = {}
        )
    }
}

@Preview(name = "Etapa Condiciones", widthDp = 360, heightDp = 800)
@Composable
private fun VistaEtapaCondiciones() = VistaPrevia {
    Column {
        EtapaCondiciones(
            NuevaInmersionUiState(temperaturaAireC = "18", temperaturaAguaC = "-1.5", visibilidad = Visibilidad.entries.first()),
            onEvento = {}
        )
    }
}

@Preview(name = "Etapa Notas", widthDp = 360)
@Composable
private fun VistaEtapaNotas() = VistaPrevia {
    Column { EtapaNotas(NuevaInmersionUiState(actividad = "Pesca", comentarios = "Mucha corriente en la punta."), onEvento = {}) }
}
