package com.berterogarcia.appnea.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Aceptar") }
        },
        text = { content() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    label: String,
    valor: String,
    onValorChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    mensajeError: String? = null
) {
    var mostrarDialogo by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = valor,
        onValueChange = {},
        readOnly = true,
        isError = isError,
        supportingText = {
            if (isError && mensajeError != null) Text(mensajeError)
        },
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { mostrarDialogo = true }) {
                Icon(Icons.Default.Info, contentDescription = "Elegir hora")
            }
        },
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.10f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
            disabledContainerColor = Color.White.copy(alpha = 0.06f),
            focusedBorderColor = Color(0xFF16F7FB),
            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
            focusedLabelColor = Color(0xFF16F7FB),
            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            disabledTextColor = Color.White,
            cursorColor = Color(0xFF16F7FB),
            focusedTrailingIconColor = Color(0xFF16F7FB),
            unfocusedTrailingIconColor = Color.White.copy(alpha = 0.7f),
            errorContainerColor = Color.White.copy(alpha = 0.10f),
            errorBorderColor = Color(0xFFFF6B6B),
            errorLabelColor = Color(0xFFFF6B6B),
            errorTextColor = Color.White,
            errorSupportingTextColor = Color(0xFFFF6B6B),
            errorCursorColor = Color(0xFFFF6B6B),
            errorTrailingIconColor = Color(0xFFFF6B6B)
        )
    )

    if (mostrarDialogo) {
        val (horaInicial, minutoInicial) = remember(valor) {
            val partes = valor.split(":").mapNotNull { it.toIntOrNull() }
            if (partes.size == 2) {
                partes[0] to partes[1]
            } else {
                val ahora = Calendar.getInstance()
                ahora.get(Calendar.HOUR_OF_DAY) to ahora.get(Calendar.MINUTE)
            }
        }
        val state = rememberTimePickerState(
            initialHour = horaInicial,
            initialMinute = minutoInicial,
            is24Hour = true
        )
        TimePickerDialog(
            onDismiss = { mostrarDialogo = false },
            onConfirm = {
                val hh = state.hour.toString().padStart(2, '0')
                val mm = state.minute.toString().padStart(2, '0')
                onValorChange("$hh:$mm")
                mostrarDialogo = false
            }
        ) {
            TimePicker(state = state)
        }
    }
}