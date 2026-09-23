package com.berterogarcia.appnea.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    valor: String,
    onValorChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    mensajeError: String? = null
) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    val formato = remember { SimpleDateFormat("dd/MM/yy", Locale.US) }

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
                Icon(Icons.Default.DateRange, contentDescription = "Elegir fecha")
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
        val initialMillis = remember(valor) {
            valor.takeIf { it.isNotBlank() }
                ?.let { runCatching { formato.parse(it)?.time }.getOrNull() }
        }

        // "Hoy" expresado en UTC-medianoche, que es el formato que usa DatePickerState internamente
        val hoyUtcMillis = remember {
            LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }

        val state = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= hoyUtcMillis

                override fun isSelectableYear(year: Int): Boolean =
                    year <= LocalDate.now().year
            }
        )

        DatePickerDialog(
            onDismissRequest = { mostrarDialogo = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onValorChange(formato.format(Date(millis)))
                    }
                    mostrarDialogo = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}