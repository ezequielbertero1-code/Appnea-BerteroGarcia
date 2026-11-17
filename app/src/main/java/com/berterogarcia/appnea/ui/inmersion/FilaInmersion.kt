package com.berterogarcia.appnea.ui.inmersion

import com.berterogarcia.appnea.ui.components.FechaEnCuadro
import com.berterogarcia.appnea.ui.components.FilaLista
import com.berterogarcia.appnea.ui.components.nombre
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.dominio.modelo.Inmersion
import com.berterogarcia.appnea.dominio.modelo.TipoInmersion
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import java.util.Locale

private val LOCALE_ES: Locale = Locale.forLanguageTag("es-AR")

/**
 * Fila de una inmersión dentro de un [GrupoLista]: fecha a la izquierda (tono distinto para apnea),
 * lugar + resumen al medio y profundidad máxima a la derecha. Toda la fila abre el detalle.
 * [conTemperaturaAgua]: agrega "14 °C" al resumen (en Inmersiones; en Inicio va más corto).
 */
@Composable
fun FilaInmersion(
    inmersion: Inmersion,
    indice: Int,
    total: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    conTemperaturaAgua: Boolean = false
) {
    FilaLista(
        titulo = inmersion.lugar.ifBlank { stringResource(R.string.inicio_sin_lugar) },
        subtitulo = subtituloInmersion(inmersion, conTemperaturaAgua),
        indice = indice,
        total = total,
        modifier = modifier,
        onClick = onClick,
        inicio = { FechaEnCuadro(inmersion.fecha, esApnea = inmersion.tipo == TipoInmersion.APNEA) },
        fin = {
            Text(
                stringResource(R.string.formato_metros, inmersion.profundidadMetros),
                style = MaterialTheme.typography.titleMedium,
                color = TextoSuperficie
            )
        }
    )
}

/**
 * "Buceo · Recreativo · 48 min" / "Apnea · 2:45" (+ " · 14 °C").
 * Si la actividad empieza con el tipo ("Buceo recreativo") se saca la repetición.
 */
@Composable
private fun subtituloInmersion(i: Inmersion, conTemperaturaAgua: Boolean): String {
    val tipo = i.tipo.nombre()
    val actividad = i.actividad.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotEmpty() }
        ?.let { a ->
            if (a.startsWith(tipo, ignoreCase = true)) {
                a.drop(tipo.length).trim().replaceFirstChar { it.titlecase(LOCALE_ES) }
            } else a
        }
        ?.takeIf { it.isNotEmpty() }
    val tiempo = if (i.tipo == TipoInmersion.APNEA) {
        // Sin el detalle de bajadas no hay tiempo total (0): se muestra la cantidad de bajadas
        if (i.tiempoFondo.inWholeSeconds > 0) {
            i.tiempoFondo.toComponents { minutos, segundos, _ -> "%d:%02d".format(minutos, segundos) }
        } else {
            i.apnea?.let { pluralStringResource(R.plurals.apnea_bajadas, it.cantidadBajadas, it.cantidadBajadas) }
        }
    } else {
        "${i.tiempoFondo.inWholeMinutes} min"
    }
    // Sin temperatura cargada (0 o NaN en registros viejos) no se muestra
    val agua = i.temperaturaAguaC
        .takeIf { conTemperaturaAgua && it.isFinite() && it != 0f }
        ?.let { t -> if (t % 1f == 0f) "${t.toInt()} °C" else "%.1f °C".format(LOCALE_ES, t) }
    return listOfNotNull(tipo, actividad, tiempo, agua).joinToString(" · ")
}
