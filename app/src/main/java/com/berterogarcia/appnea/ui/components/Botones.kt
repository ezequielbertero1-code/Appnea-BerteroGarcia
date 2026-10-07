package com.berterogarcia.appnea.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.FondoPeligro
import com.berterogarcia.appnea.ui.theme.FondoTarjetaResaltada
import com.berterogarcia.appnea.ui.theme.TextoDeshabilitado
import com.berterogarcia.appnea.ui.theme.VeloMedio
import com.berterogarcia.appnea.ui.theme.VeloSuave

// Estilo común de los botones de la app (mismo que "Registrar inmersión" de Inicio).
val FormaBoton = RoundedCornerShape(16.dp)
private val AltoMinimoBoton = 52.dp

/** Acción principal de la pantalla (Siguiente, Guardar, Iniciar…). */
@Composable
fun BotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cargando: Boolean = false,
    icono: Painter? = null
) = BotonBase(
    texto, onClick, modifier, enabled, cargando, icono,
    fondo = FondoTarjetaResaltada,
    borde = Acento.copy(alpha = 0.45f),
    colorIcono = Color.White
)

/** Acción secundaria (Anterior, Parar, Cerrar sesión…): no le compite a la principal. */
@Composable
fun BotonSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cargando: Boolean = false,
    icono: Painter? = null
) = BotonBase(
    texto, onClick, modifier, enabled, cargando, icono,
    fondo = VeloMedio,
    borde = null,
    colorIcono = Acento
)

/** Acción destructiva o de corte (Abortar). */
@Composable
fun BotonPeligro(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icono: Painter? = null
) = BotonBase(
    texto, onClick, modifier, enabled, cargando = false, icono = icono,
    fondo = FondoPeligro,
    borde = ColorError.copy(alpha = 0.5f),
    colorIcono = Color.White
)

/**
 * [cargando]: el botón queda bloqueado (no se puede tocar dos veces) pero conserva su color
 * y muestra una ruedita junto al texto, en vez de ponerse gris.
 */
@Composable
private fun BotonBase(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    cargando: Boolean,
    icono: Painter?,
    fondo: Color,
    borde: Color?,
    colorIcono: Color
) {
    val activo = enabled && !cargando
    Button(
        onClick = onClick,
        enabled = activo,
        modifier = modifier.heightIn(min = AltoMinimoBoton),
        shape = FormaBoton,
        colors = ButtonDefaults.buttonColors(
            containerColor = fondo,
            contentColor = Color.White,
            disabledContainerColor = if (cargando) fondo else VeloSuave,
            disabledContentColor = if (cargando) Color.White else TextoDeshabilitado
        ),
        border = borde?.let { BorderStroke(1.dp, if (enabled) it else it.copy(alpha = 0.15f)) },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        when {
            cargando -> {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(Modifier.width(8.dp))
            }
            icono != null -> {
                Icon(
                    painter = icono,
                    contentDescription = null, // el texto del botón ya lo describe
                    tint = if (activo) colorIcono else TextoDeshabilitado,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
        }
        Text(texto, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}