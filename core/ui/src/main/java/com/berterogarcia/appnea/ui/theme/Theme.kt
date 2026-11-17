package com.berterogarcia.appnea.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Degradado de fondo de todas las pantallas. */
val fondoApp = Brush.verticalGradient(colors = listOf(FondoArriba, FondoAbajo))

/**
 * Esquema de Material armado con la paleta de la app. Lo usan los componentes que no tienen
 * color propio (TextField, pestañas, checkbox, diálogos, date pickers, progreso...).
 * Sin colores dinámicos: así la app se ve igual en todos los celulares, no según el fondo de
 * pantalla de cada uno (Android 12+).
 */
private val EsquemaAppnea = darkColorScheme(
    primary = Acento,
    onPrimary = TextoSobreAcentoFuerte,
    primaryContainer = FondoTarjetaResaltada,
    onPrimaryContainer = TextoSobreContenedorPrimario,
    secondary = FondoTarjetaResaltada,
    onSecondary = Color.White,
    // Chips elegidos, botones segmentados y tonales
    secondaryContainer = ContenedorSecundario,
    onSecondaryContainer = TextoSobreContenedorSecundario,
    tertiary = ColorRegular,
    onTertiary = FondoTarjeta,
    background = FondoAbajo,
    onBackground = Color.White,
    surface = FondoTarjeta,
    onSurface = TextoSuperficie,
    surfaceVariant = FondoArriba,
    onSurfaceVariant = TextoSuperficieVariante,
    // Diálogos (High), menús (Container) y hojas inferiores (Low) van sólidos en FondoIntermedio;
    // las tarjetas y filas usan velos (SuperficieTonal) con color propio, no estos.
    surfaceContainerLowest = FondoAbajo,
    surfaceContainerLow = FondoIntermedio,
    surfaceContainer = FondoIntermedio,
    surfaceContainerHigh = FondoIntermedio,
    surfaceContainerHighest = FondoTarjetaResaltada,
    error = ColorMalo,
    onError = FondoTarjeta,
    outline = ContornoFuerte,
    outlineVariant = Divisor
)

/** La app es siempre oscura (fondo marino), así que hay un solo esquema. */
@Composable
fun AppneaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaAppnea,
        typography = Typography,
        content = content
    )
}
