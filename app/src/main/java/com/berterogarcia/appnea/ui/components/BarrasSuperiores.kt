package com.berterogarcia.appnea.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.theme.FondoTarjetaResaltada
import com.berterogarcia.appnea.ui.theme.TextoSobreContenedorPrimario
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante

// ============================ Barras superiores (TopAppBar de Material 3) ============================
// Transparentes sobre el degradado y sin insets propios: el Scaffold de MainActivity ya deja el
// espacio de la barra de estado.

/** Ícono de la izquierda: flecha en detalles, "X" en formularios a pantalla completa. */
enum class NavegacionBarra { NINGUNA, VOLVER, CERRAR }

/**
 * Barra chica: título de una línea, navegación opcional y acciones a la derecha.
 * Sin scrollBehavior (es experimental y obligaría a cada pantalla a hacer opt-in).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperior(
    titulo: String,
    modifier: Modifier = Modifier,
    navegacion: NavegacionBarra = NavegacionBarra.NINGUNA,
    onNavegacion: () -> Unit = {},
    acciones: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(titulo, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        navigationIcon = { IconoNavegacion(navegacion, onNavegacion) },
        actions = acciones,
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = coloresBarra()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun coloresBarra(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    // Transparente también al scrollear: el contenido no pasa por debajo de la barra
    containerColor = Color.Transparent,
    scrolledContainerColor = Color.Transparent,
    navigationIconContentColor = TextoSuperficie,
    titleContentColor = TextoSuperficie,
    actionIconContentColor = TextoSuperficieVariante
)

@Composable
private fun IconoNavegacion(navegacion: NavegacionBarra, onNavegacion: () -> Unit) {
    when (navegacion) {
        NavegacionBarra.NINGUNA -> Unit
        NavegacionBarra.VOLVER -> IconButton(onClick = onNavegacion) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.accion_volver))
        }
        NavegacionBarra.CERRAR -> IconButton(onClick = onNavegacion) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.accion_cerrar))
        }
    }
}

/**
 * Círculo con la inicial del usuario (o un ícono de persona si no hay nombre).
 * 32 dp en la barra de Inicio, 72 dp en el encabezado de Cuenta, 44 dp en contactos.
 */
@Composable
fun Avatar(inicial: String?, modifier: Modifier = Modifier, tamanio: Dp = 32.dp) {
    Box(
        modifier = modifier
            .size(tamanio)
            .clip(CircleShape)
            .background(FondoTarjetaResaltada),
        contentAlignment = Alignment.Center
    ) {
        if (inicial != null) {
            Text(
                inicial,
                color = TextoSobreContenedorPrimario,
                // La letra crece con el círculo: 32 dp → 14 sp, 72 dp → ~29 sp
                fontSize = (tamanio.value * 0.42f).sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            Icon(
                Icons.Rounded.Person,
                contentDescription = null,
                tint = TextoSobreContenedorPrimario,
                modifier = Modifier.size(tamanio * 0.6f)
            )
        }
    }
}
