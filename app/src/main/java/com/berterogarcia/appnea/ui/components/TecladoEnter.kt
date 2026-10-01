package com.berterogarcia.appnea.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Ejecuta [accion] cuando se aprieta Enter en un TECLADO FÍSICO (el de la PC en el
 * emulador, un teclado bluetooth, etc.).
 *
 * El teclado en pantalla NO pasa por acá: ese usa el botón de acción del IME
 * (ImeAction.Next / ImeAction.Done + KeyboardActions). Por eso en los campos se usan
 * las dos cosas juntas.
 *
 * - Se consume el KeyDown y el KeyUp para que el campo no reciba el Enter (en campos
 *   de una línea no metería un salto, pero así nos aseguramos de que no haga nada raro).
 * - Se ignoran las repeticiones (tecla mantenida) para no disparar la acción varias veces.
 */
fun Modifier.alPresionarEnter(accion: () -> Unit): Modifier = onPreviewKeyEvent { evento ->
    val esEnter = evento.key == Key.Enter || evento.key == Key.NumPadEnter
    when {
        !esEnter -> false
        evento.type == KeyEventType.KeyDown -> {
            if (evento.nativeKeyEvent.repeatCount == 0) accion()
            true
        }
        else -> true
    }
}
