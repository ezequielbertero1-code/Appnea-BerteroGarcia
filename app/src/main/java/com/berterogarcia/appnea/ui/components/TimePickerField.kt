package com.berterogarcia.appnea.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.berterogarcia.appnea.R
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.accion_cancelar)) }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.accion_aceptar)) }
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
                Icon(painterResource(R.drawable.ic_ms_schedule), contentDescription = stringResource(R.string.campo_elegir_hora))
            }
        },
        modifier = modifier,
        colors = coloresCampoTexto()
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