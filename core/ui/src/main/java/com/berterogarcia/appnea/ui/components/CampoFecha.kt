package com.berterogarcia.appnea.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.berterogarcia.appnea.core.ui.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Formato con año de 4 dígitos: con "yy" una fecha de nacimiento de 1995 se leería como 2095. */
val FORMATO_FECHA_LARGA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun LocalDate?.formatoLargo(vacio: String = "-"): String = this?.format(FORMATO_FECHA_LARGA) ?: vacio

/**
 * Campo de fecha que trabaja directamente con [LocalDate] (sin parsear Strings).
 *
 * Diferencias con DatePickerField (que se sigue usando en inmersiones):
 *  - Permite fechas futuras (vencimientos, próximo service) usando [maxima] = null.
 *  - Permite restringir con [minima] / [maxima].
 *  - Se puede borrar la fecha (campos opcionales).
 *  - Tocar en cualquier parte del campo abre el calendario, no solo el ícono.
 *
 * Nota: el DatePicker de Material trabaja en milisegundos UTC a medianoche, por eso las
 * conversiones usan ZoneOffset.UTC (si se usa la zona local, en Argentina (UTC-3) la
 * fecha elegida aparecería un día antes).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(
    etiqueta: String,
    fecha: LocalDate?,
    onFechaChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    minima: LocalDate? = null,
    maxima: LocalDate? = null,
    permitirBorrar: Boolean = true,
    esError: Boolean = false,
    mensajeError: String? = null
) {
    var mostrarDialogo by rememberSaveable { mutableStateOf(false) }

    // Un campo readOnly no dispara onClick: escuchamos el "soltar" del toque.
    val interaccion = remember { MutableInteractionSource() }
    LaunchedEffect(interaccion) {
        interaccion.interactions.collect { if (it is PressInteraction.Release) mostrarDialogo = true }
    }

    OutlinedTextField(
        value = fecha.formatoLargo(vacio = ""),
        onValueChange = {},
        readOnly = true,
        singleLine = true,
        label = { Text(etiqueta) },
        placeholder = { Text(stringResource(R.string.campo_formato_fecha)) },
        isError = esError,
        supportingText = { if (esError && mensajeError != null) Text(mensajeError) },
        interactionSource = interaccion,
        trailingIcon = {
            Row {
                if (permitirBorrar && fecha != null) {
                    IconButton(onClick = { onFechaChange(null) }) {
                        Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.campo_borrar_fecha))
                    }
                }
                IconButton(onClick = { mostrarDialogo = true }) {
                    Icon(painterResource(R.drawable.ic_ms_calendar_month), contentDescription = stringResource(R.string.campo_elegir_fecha))
                }
            }
        },
        modifier = modifier,
        colors = coloresCampoTexto()
    )

    if (mostrarDialogo) {
        val fechasValidas = remember(minima, maxima) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val dia = utcTimeMillis.aLocalDateUtc()
                    return (minima == null || !dia.isBefore(minima)) &&
                        (maxima == null || !dia.isAfter(maxima))
                }

                override fun isSelectableYear(year: Int): Boolean =
                    (minima == null || year >= minima.year) && (maxima == null || year <= maxima.year)
            }
        }
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fecha?.aMillisUtc(),
            selectableDates = fechasValidas
        )

        DatePickerDialog(
            onDismissRequest = { mostrarDialogo = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        estado.selectedDateMillis?.let { onFechaChange(it.aLocalDateUtc()) }
                        mostrarDialogo = false
                    },
                    enabled = estado.selectedDateMillis != null
                ) { Text(stringResource(R.string.accion_aceptar)) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text(stringResource(R.string.accion_cancelar)) }
            }
        ) {
            DatePicker(state = estado)
        }
    }
}

private fun LocalDate.aMillisUtc(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.aLocalDateUtc(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
