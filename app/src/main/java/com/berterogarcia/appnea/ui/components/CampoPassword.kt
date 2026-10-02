package com.berterogarcia.appnea.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.berterogarcia.appnea.R

/**
 * Campo de contraseña con el ojo para mostrar/ocultar. Usa CampoTexto por debajo (mismo estilo).
 * @param onImeAction qué hace el botón del teclado (Siguiente / Listo). Si es null, "Siguiente" pasa al próximo campo.
 */
@Composable
fun CampoPassword(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    esError: Boolean = false,
    mensajeError: String = "",
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null
) {
    var visible by rememberSaveable { mutableStateOf(false) }

    CampoTexto(
        valor = valor,
        onValorChange = onValorChange,
        etiqueta = etiqueta,
        modifier = modifier,
        esError = esError,
        mensajeError = mensajeError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = onImeAction?.let { accion ->
            KeyboardActions(onNext = { accion() }, onDone = { accion() })
        } ?: KeyboardActions.Default,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    painter = painterResource(if (visible) R.drawable.ic_ocultar else R.drawable.ic_ver),
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    )
}