package com.berterogarcia.appnea.ui.entrenamiento

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.core.ui.R as UiR
import com.berterogarcia.appnea.ui.theme.AvisoAcento
import com.berterogarcia.appnea.ui.theme.AvisoFondo
import com.berterogarcia.appnea.ui.theme.AvisoTexto

/**
 * Aviso de seguridad de apnea, desplegable como los temas de Aprender: cerrado muestra solo
 * el título con el ícono de advertencia; al tocarlo se abre el texto. Mantiene los colores
 * claros originales (AvisoFondo/Acento/Texto). Arranca cerrado y recuerda si se abrió.
 */
@Composable
fun AvisoSeguridadApnea(modifier: Modifier = Modifier, separacionInferior: Dp = 16.dp) {
    var expandido by rememberSaveable { mutableStateOf(false) }
    val accion = stringResource(if (expandido) UiR.string.accion_contraer else UiR.string.accion_expandir)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = separacionInferior)
            .clip(RoundedCornerShape(16.dp))
            .background(AvisoFondo)
            .clickable(onClickLabel = accion) { expandido = !expandido }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Warning,
                contentDescription = null, // el título ya lo dice
                tint = AvisoAcento,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.aviso_apnea_titulo),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = AvisoAcento,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (expandido) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = accion,
                tint = AvisoAcento
            )
        }
        AnimatedVisibility(visible = expandido, enter = expandVertically(), exit = shrinkVertically()) {
            Text(
                stringResource(R.string.aviso_apnea_texto),
                style = MaterialTheme.typography.bodySmall,
                color = AvisoTexto,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
