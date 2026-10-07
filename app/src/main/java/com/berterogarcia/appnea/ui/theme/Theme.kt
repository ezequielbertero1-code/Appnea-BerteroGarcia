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
    onPrimary = FondoTarjeta,
    primaryContainer = FondoTarjetaResaltada,
    onPrimaryContainer = Color.White,
    secondary = FondoTarjetaResaltada,
    onSecondary = Color.White,
    tertiary = ColorRegular,
    onTertiary = FondoTarjeta,
    background = FondoAbajo,
    onBackground = Color.White,
    surface = FondoTarjeta,
    onSurface = Color.White,
    surfaceVariant = FondoArriba,
    onSurfaceVariant = TextoSecundario,
    // Diálogos, menús y date pickers usan estos: azules de la app en vez del gris violáceo por defecto
    surfaceContainerLowest = FondoAbajo,
    surfaceContainerLow = FondoTarjeta,
    surfaceContainer = FondoIntermedio,
    surfaceContainerHigh = FondoArriba,
    surfaceContainerHighest = FondoTarjetaResaltada,
    error = ColorMalo,
    onError = FondoTarjeta,
    outline = Contorno,
    outlineVariant = Divisor
)

/** La app es siempre oscura (fondo marino), así que hay un solo esquema. */
@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaAppnea,
        typography = Typography,
        content = content
    )
}