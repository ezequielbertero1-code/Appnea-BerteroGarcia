package com.berterogarcia.appnea.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.TextoSecundario

/**
 * Título de las pantallas principales (Inicio, Entrenar, Inmersiones, Cuenta).
 * Es el mismo componente en todas, así el título y el botón de la derecha quedan
 * siempre a la misma altura y en la misma posición.
 *
 * La fila se alinea ARRIBA: el botón mide 48 dp (área táctil mínima) y, centrado,
 * bajaría el título respecto de las pantallas que no tienen botón.
 */
@Composable
fun TituloPantalla(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    accion: (@Composable () -> Unit)? = null
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                titulo,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (subtitulo != null) {
                Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            }
        }
        accion?.invoke()
    }
}

/** Botón "📈 Estadísticas" de la derecha del título (Entrenar e Inmersiones). */
@Composable
fun BotonVerEstadisticas(onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(stringResource(R.string.ver_estadisticas), color = Acento) }
}
