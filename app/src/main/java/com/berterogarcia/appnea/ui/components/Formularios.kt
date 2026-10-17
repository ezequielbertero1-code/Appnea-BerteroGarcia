package com.berterogarcia.appnea.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.Divisor
import com.berterogarcia.appnea.ui.theme.FondoAbajo
import com.berterogarcia.appnea.ui.theme.SuperficieTonalAlta
import com.berterogarcia.appnea.ui.theme.SuperficieTonalMaxima
import com.berterogarcia.appnea.ui.theme.TextoDeshabilitado
import com.berterogarcia.appnea.ui.theme.TextoSuperficie

// ============================ Piezas de formularios y asistentes (estilo "Appnea circular") ============================

/**
 * Progreso segmentado: un tramo por paso (registro, nueva inmersión) o por fase (tablas).
 * Los [completados] primeros tramos van en cian. TalkBack lo lee como "paso X de N".
 */
@Composable
fun ProgresoPorPasos(total: Int, completados: Int, modifier: Modifier = Modifier) {
    val totalSeguro = total.coerceAtLeast(1)
    val hechos = completados.coerceIn(0, totalSeguro)
    val descripcion = stringResource(R.string.progreso_paso_de, hechos, totalSeguro)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .semantics {
                contentDescription = descripcion
                progressBarRangeInfo = ProgressBarRangeInfo(hechos.toFloat(), 0f..totalSeguro.toFloat(), totalSeguro - 1)
            },
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(totalSeguro) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i < hechos) Acento else SuperficieTonalMaxima)
            )
        }
    }
}

/**
 * Progreso por pasos de un asistente (registro, nueva inmersión): un tramo por paso, en cian los
 * visitados y en rojo los que tienen errores. Los pasos anteriores al [actual] se pueden tocar para
 * volver; el área táctil es más alta que el tramo de 4 dp.
 */
@Composable
fun ProgresoPasosNavegable(
    total: Int,
    actual: Int,
    conError: Set<Int>,
    onIrA: (Int) -> Unit,
    descripcion: @Composable (Int) -> String,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { i ->
            val color = when {
                i in conError -> ColorError
                i <= actual -> Acento
                else -> SuperficieTonalMaxima
            }
            val texto = descripcion(i)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp)
                    .clickable(enabled = i < actual) { onIrA(i) }
                    .semantics { contentDescription = texto },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }
    }
}

/**
 * Barra fija abajo de los formularios con las acciones (Anterior | Siguiente, Descartar | Guardar).
 * Los botones se reparten el ancho con `Modifier.weight(1f)` desde el llamador.
 * [conFondo] = false la deja transparente y sin línea (sesión de tabla en curso).
 */
@Composable
fun BarraAccionesInferior(
    modifier: Modifier = Modifier,
    conFondo: Boolean = true,
    contenido: @Composable RowScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (conFondo) HorizontalDivider(color = Divisor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (conFondo) Modifier.background(FondoAbajo.copy(alpha = 0.6f)) else Modifier)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = contenido
        )
    }
}

/**
 * Stepper − valor + (duración de cada fase al crear una tabla). Los botones se deshabilitan en
 * los límites; [descripcionMenos]/[descripcionMas] son lo que lee TalkBack ("Restar 15 segundos").
 */
@Composable
fun Stepper(
    valor: String,
    onMenos: () -> Unit,
    onMas: () -> Unit,
    descripcionMenos: String,
    descripcionMas: String,
    modifier: Modifier = Modifier,
    menosHabilitado: Boolean = true,
    masHabilitado: Boolean = true
) {
    val colores = IconButtonDefaults.filledIconButtonColors(
        containerColor = SuperficieTonalAlta,
        contentColor = TextoSuperficie,
        disabledContainerColor = SuperficieTonalAlta.copy(alpha = 0.05f),
        disabledContentColor = TextoDeshabilitado
    )
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        FilledIconButton(onClick = onMenos, enabled = menosHabilitado, colors = colores, modifier = Modifier.size(32.dp)) {
            Icon(
                painterResource(R.drawable.ic_ms_remove),
                contentDescription = descripcionMenos,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            valor,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = TextoSuperficie,
            // Números tabulares: el ancho no baila al pasar de 1:45 a 2:00
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFeatureSettings = "tnum"
            ),
            modifier = Modifier
                .widthIn(min = 52.dp)
                .clearAndSetSemantics { contentDescription = valor }
        )
        FilledIconButton(onClick = onMas, enabled = masHabilitado, colors = colores, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Rounded.Add, contentDescription = descripcionMas, modifier = Modifier.size(18.dp))
        }
    }
}
