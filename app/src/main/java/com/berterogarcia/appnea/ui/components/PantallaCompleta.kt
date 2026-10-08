package com.berterogarcia.appnea.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

/**
 * Estado compartido para que una pantalla pueda ocultar la barra de navegación.
 * Lo provee MyApplicationApp (MainActivity). Fuera de ella el valor por defecto no hace nada.
 */
val LocalOcultarBarra = staticCompositionLocalOf<MutableState<Boolean>> { mutableStateOf(false) }

/**
 * Mientras este composable esté en pantalla, la barra de navegación queda oculta
 * (se usa en el entrenamiento en curso para no salir de la tabla sin querer).
 * Al salir de pantalla la vuelve a mostrar sola.
 */
@Composable
fun OcultarBarraNavegacion() {
    val ocultar = LocalOcultarBarra.current
    DisposableEffect(ocultar) {
        ocultar.value = true
        onDispose { ocultar.value = false }
    }
}

/** Mientras este composable esté en pantalla, el teléfono no apaga la pantalla. */
@Composable
fun MantenerPantallaEncendida() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
