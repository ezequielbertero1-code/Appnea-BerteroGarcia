package com.berterogarcia.appnea.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorAdvertencia
import com.berterogarcia.appnea.ui.theme.ColorBueno
import com.berterogarcia.appnea.ui.theme.ColorMalo
import com.berterogarcia.appnea.ui.theme.SuperficieTonal
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante

// ============================ Tarjetas e insignias (estilo "Appnea circular") ============================

private val FormaTarjeta = RoundedCornerShape(16.dp)

/** Tarjeta de superficie tonal (velo blanco 7 % sobre el degradado), esquinas de 16 dp. */
@Composable
fun TarjetaTonal(
    modifier: Modifier = Modifier,
    relleno: PaddingValues = PaddingValues(16.dp),
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = FormaTarjeta,
        colors = CardDefaults.cardColors(containerColor = SuperficieTonal, contentColor = TextoSuperficie)
    ) {
        Column(modifier = Modifier.padding(relleno), content = contenido)
    }
}

/**
 * Tarjeta de un número clave: etiqueta chica arriba y valor grande abajo, con la [unidad]
 * más chica y en gris ("22,3 m"). Van de a dos por fila en Cuenta y Estadísticas.
 */
@Composable
fun TarjetaKpi(etiqueta: String, valor: String, modifier: Modifier = Modifier, unidad: String? = null) {
    TarjetaTonal(modifier = modifier, relleno = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.bodySmall, color = TextoSuperficieVariante)
        Text(
            buildAnnotatedString {
                append(valor)
                if (unidad != null) {
                    withStyle(SpanStyle(fontSize = 14.sp, color = TextoSuperficieVariante)) { append(" $unidad") }
                }
            },
            fontSize = 24.sp,
            lineHeight = 30.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/** Semáforo de las insignias: vigente / por vencer / vencido / informativa. */
enum class TipoInsignia(val color: Color, val alfaFondo: Float) {
    BUENA(ColorBueno, 0.16f),
    ADVERTENCIA(ColorAdvertencia, 0.16f),
    MALA(ColorMalo, 0.16f),
    INFO(Acento, 0.12f)
}

/** Insignia chica con fondo translúcido del color de estado ("Vigente", "Service pronto", "Buceo"). */
@Composable
fun Insignia(texto: String, tipo: TipoInsignia, modifier: Modifier = Modifier, icono: Painter? = null) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tipo.color.copy(alpha = tipo.alfaFondo))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, tint = tipo.color, modifier = Modifier.size(14.dp))
        }
        Text(texto, color = tipo.color, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** Fila de dos [TarjetaKpi] (o cualquier par de tarjetas) del mismo ancho. */
@Composable
fun FilaDeTarjetas(modifier: Modifier = Modifier, contenido: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = contenido
    )
}
