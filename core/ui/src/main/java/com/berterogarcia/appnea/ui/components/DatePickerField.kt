package com.berterogarcia.appnea.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.berterogarcia.appnea.core.ui.R
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
                Icon(painterResource(R.drawable.ic_ms_calendar_month), contentDescription = stringResource(R.string.campo_elegir_fecha))
            }
        },
        modifier = modifier,
        colors = coloresCampoTexto()
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
                }) { Text(stringResource(R.string.accion_aceptar)) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text(stringResource(R.string.accion_cancelar)) }
            }
        ) {
            DatePicker(state = state)
        }
    }
}
