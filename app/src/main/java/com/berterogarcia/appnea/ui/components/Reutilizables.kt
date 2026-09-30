package com.berterogarcia.appnea.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation


@Composable
fun CampoTexto(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    esPassword: Boolean = false,
    esError: Boolean = false,
    mensajeError: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    minLines: Int = 1,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        label = { Text(etiqueta) },
        trailingIcon = trailingIcon,
        visualTransformation = if (esPassword) PasswordVisualTransformation() else visualTransformation,
        isError = esError,
        supportingText = { if (esError) Text(mensajeError) },
        keyboardOptions = keyboardOptions,
        singleLine = minLines == 1,
        minLines = minLines,
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.10f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
            focusedBorderColor = Color(0xFF16F7FB),
            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
            focusedLabelColor = Color(0xFF16F7FB),
            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color(0xFF16F7FB),
            errorContainerColor = Color.White.copy(alpha = 0.10f),
            errorBorderColor = Color(0xFFFF6B6B),
            errorLabelColor = Color(0xFFFF6B6B),
            errorTextColor = Color.White,
            errorSupportingTextColor = Color(0xFFFF6B6B),
            errorCursorColor = Color(0xFFFF6B6B)
        )
    )
}