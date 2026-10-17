package com.berterogarcia.appnea.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.ContenedorSecundario
import com.berterogarcia.appnea.ui.theme.ContornoFuerte
import com.berterogarcia.appnea.ui.theme.FondoTarjetaResaltada
import com.berterogarcia.appnea.ui.theme.TextoDeshabilitado
import com.berterogarcia.appnea.ui.theme.TextoSobreAcentoFuerte
import com.berterogarcia.appnea.ui.theme.TextoSobreContenedorPrimario
import com.berterogarcia.appnea.ui.theme.TextoSobreContenedorSecundario
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante
import com.berterogarcia.appnea.ui.theme.VeloSuave

// Botones en pastilla de Material 3 (estilo "Appnea circular"). Las firmas no cambiaron:
// las pantallas que ya los usaban toman el estilo nuevo sin tocarlas.
val FormaBoton = CircleShape
private val AltoMinimoBoton = 48.dp

/** Acción principal de la pantalla (Siguiente, Guardar, Iniciar…): cian relleno. */
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
    fondo = Acento,
    contenido = TextoSobreAcentoFuerte,
    borde = null
)

/** Acción secundaria que igual tiene peso (Reiniciar, Llamar, Repetir última…): tonal. */
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
    fondo = ContenedorSecundario,
    contenido = TextoSobreContenedorSecundario,
    borde = null
)

/** Acción de menor jerarquía (Anterior, Cancelar, Descartar…): solo contorno. */
@Composable
fun BotonContorno(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icono: Painter? = null
) = BotonBase(
    texto, onClick, modifier, enabled, cargando = false, icono = icono,
    fondo = Color.Transparent,
    contenido = TextoSuperficieVariante,
    borde = ContornoFuerte
)

/** Acción destructiva o de corte (Abortar): contorno rojo; la confirmación va en un diálogo. */
@Composable
fun BotonPeligro(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icono: Painter? = null
) = BotonBase(
    texto, onClick, modifier, enabled, cargando = false, icono = icono,
    fondo = Color.Transparent,
    contenido = ColorError,
    borde = ColorError
)

/**
 * Botón flotante extendido (ícono + texto) para la acción de alta de una lista: "Nueva inmersión",
 * "Agregar equipo"... Va abajo a la derecha; [expandido] = false lo achica a solo ícono (al scrollear).
 */
@Composable
fun BotonFlotante(
    texto: String,
    icono: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expandido: Boolean = true
) {
    ExtendedFloatingActionButton(
        text = { Text(texto, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        icon = { Icon(icono, contentDescription = null) },
        onClick = onClick,
        expanded = expandido,
        modifier = modifier,
        containerColor = FondoTarjetaResaltada,
        contentColor = TextoSobreContenedorPrimario
    )
}

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
    contenido: Color,
    borde: Color?
) {
    val activo = enabled && !cargando
    // Sin relleno propio (contorno) el deshabilitado queda transparente, no con velo
    val fondoDeshabilitado = if (fondo == Color.Transparent) Color.Transparent else VeloSuave
    Button(
        onClick = onClick,
        enabled = activo,
        modifier = modifier.heightIn(min = AltoMinimoBoton),
        shape = FormaBoton,
        colors = ButtonDefaults.buttonColors(
            containerColor = fondo,
            contentColor = contenido,
            disabledContainerColor = if (cargando) fondo else fondoDeshabilitado,
            disabledContentColor = if (cargando) contenido else TextoDeshabilitado
        ),
        border = borde?.let { BorderStroke(1.dp, if (enabled) it else TextoDeshabilitado) },
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
    ) {
        when {
            cargando -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current
                )
                Spacer(Modifier.width(8.dp))
            }
            icono != null -> {
                // Sin tint propio: toma el color de contenido del botón (incluido el deshabilitado)
                Icon(painter = icono, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
        }
        Text(texto, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
