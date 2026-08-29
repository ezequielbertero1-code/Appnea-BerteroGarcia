package com.berterogarcia.appnea.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun CampoTexto(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    esPassword: Boolean = false,
    esError: Boolean = false,
    mensajeError: String = ""
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        label = { Text(etiqueta) },
        visualTransformation = if (esPassword) PasswordVisualTransformation() else VisualTransformation.None,
        isError = esError,
        supportingText = { if (esError) Text(mensajeError) },
        modifier = Modifier.fillMaxWidth()
    )
}