package com.berterogarcia.appnea.ui.inmersion

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.berterogarcia.appnea.dominio.modelo.Inmersion
import com.berterogarcia.appnea.dominio.modelo.TipoInmersion
import com.berterogarcia.appnea.ui.components.VistaPrevia
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration.Companion.minutes

private fun ejemplo(id: Long, lugar: String, tipo: TipoInmersion, profundidad: Float) = Inmersion(
    id = id, fecha = LocalDate.of(2026, 10, 24), lugar = lugar, profundidadMetros = profundidad,
    paradaSeguridadMin = 3f, tiempoFondo = 42.minutes, horaEntrada = LocalTime.of(10, 30),
    horaSalida = LocalTime.of(11, 15), caracteristicasAgua = setOf("Salada"), lastreKg = 6f, proteccionMm = 7f,
    equipamiento = emptySet(), temperaturaAireC = 18f, temperaturaAguaC = 12f, actividad = "", comentarios = "", tipo = tipo
)

@Preview(name = "Filas del historial", widthDp = 360)
@Composable
private fun VistaFilasInmersion() = VistaPrevia {
    val lista = listOf(
        ejemplo(1, "Punta Marqués", TipoInmersion.BUCEO, 18f),
        ejemplo(2, "Rada Tilly", TipoInmersion.APNEA, 12.5f)
    )
    Column {
        lista.forEachIndexed { i, inmersion ->
            FilaInmersion(inmersion, indice = i, total = lista.size, onClick = {}, conTemperaturaAgua = true)
        }
    }
}
