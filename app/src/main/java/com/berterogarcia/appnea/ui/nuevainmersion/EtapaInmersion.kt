package com.berterogarcia.appnea.ui.nuevainmersion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.dominio.modelo.TipoInmersion
import com.berterogarcia.appnea.dominio.modelo.formatearDuracion
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.FilaMosaicos
import com.berterogarcia.appnea.ui.components.Mosaico
import com.berterogarcia.appnea.ui.components.SelectorSegmentado
import com.berterogarcia.appnea.ui.components.resolver
import com.berterogarcia.appnea.ui.condiciones.TarjetaCondicion
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.TextoSobreAcentoFuerte
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante

// Paso 2: profundidad, tiempos y (apnea) registro de bajadas.

@Composable
internal fun EtapaInmersion(uiState: NuevaInmersionUiState, onEvento: (EventoFormulario) -> Unit) {
    if (uiState.tipo == TipoInmersion.APNEA) {
        BloqueApnea(uiState, onEvento)
        return
    }
    CampoTexto(
        valor = uiState.profundidadMetros, onValorChange = { onEvento(EventoFormulario.Profundidad(it)) },
        etiqueta = stringResource(R.string.nueva_profundidad),
        esError = uiState.errores.containsKey(Campo.PROFUNDIDAD), mensajeError = uiState.errores[Campo.PROFUNDIDAD]?.resolver() ?: "",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
    CampoTexto(
        valor = uiState.tiempoFondo, onValorChange = { onEvento(EventoFormulario.TiempoFondo(it)) },
        etiqueta = stringResource(R.string.nueva_tiempo_fondo),
        visualTransformation = tiempoFondoVisualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        esError = uiState.errores.containsKey(Campo.TIEMPO_FONDO), mensajeError = uiState.errores[Campo.TIEMPO_FONDO]?.resolver() ?: "",
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    )
    CampoTexto(
        valor = uiState.paradaSeguridadMin, onValorChange = { onEvento(EventoFormulario.Parada(it)) },
        etiqueta = stringResource(R.string.nueva_parada),
        esError = uiState.errores.containsKey(Campo.PARADA), mensajeError = uiState.errores[Campo.PARADA]?.resolver() ?: "",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    )
}

/**
 * Apnea: siempre se pide la cantidad de bajadas y la profundidad máxima; cargar cada bajada
 * (profundidad + tiempo) es opcional. Al activar el detalle se pre-generan tantas filas como
 * bajadas indicadas, y la profundidad máxima sale de ellas.
 */
@Composable
internal fun BloqueApnea(uiState: NuevaInmersionUiState, onEvento: (EventoFormulario) -> Unit) {
    CampoTexto(
        valor = uiState.cantidadBajadas, onValorChange = { onEvento(EventoFormulario.CantidadBajadas(it)) },
        etiqueta = stringResource(R.string.nueva_cantidad_bajadas),
        esError = uiState.errores.containsKey(Campo.CANTIDAD_BAJADAS),
        mensajeError = uiState.errores[Campo.CANTIDAD_BAJADAS]?.resolver() ?: "",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
    if (!uiState.cargarDetalleBajadas) {
        CampoTexto(
            valor = uiState.profundidadMetros, onValorChange = { onEvento(EventoFormulario.Profundidad(it)) },
            etiqueta = stringResource(R.string.nueva_profundidad),
            esError = uiState.errores.containsKey(Campo.PROFUNDIDAD), mensajeError = uiState.errores[Campo.PROFUNDIDAD]?.resolver() ?: "",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }

    // Interruptor del detalle (toda la fila se toca)
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp)
            .toggleable(
                value = uiState.cargarDetalleBajadas, role = Role.Switch,
                onValueChange = { onEvento(EventoFormulario.DetalleBajadas(it)) }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(stringResource(R.string.nueva_cargar_cada_bajada), color = TextoSuperficie, style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.nueva_cargar_cada_bajada_ayuda),
                color = TextoSuperficieVariante, style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(
            checked = uiState.cargarDetalleBajadas, onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = Acento, checkedThumbColor = TextoSobreAcentoFuerte)
        )
    }

    if (uiState.cargarDetalleBajadas) {
        val hayError = uiState.errores.containsKey(Campo.BAJADAS)
        val mejor = uiState.indiceMejorBajada
        val activas = uiState.bajadasActivas
        activas.forEachIndexed { indice, bajada ->
            val invalida = hayError && !bajada.esValida()
            val esMejor = indice == mejor
            val descMejor = stringResource(R.string.nueva_bajada_mejor)
            // Una sola línea: nº de bajada · profundidad · tiempo · tacho
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.width(30.dp).semantics { if (esMejor) contentDescription = descMejor },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (esMejor) Icon(Icons.Rounded.Star, contentDescription = null, tint = Acento, modifier = Modifier.size(14.dp))
                    Text(
                        "${indice + 1}",
                        color = if (esMejor) Acento else TextoSuperficieVariante,
                        fontWeight = if (esMejor) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                CampoTexto(
                    valor = bajada.profundidad,
                    onValorChange = { onEvento(EventoFormulario.BajadaProfundidad(indice, it)) },
                    etiqueta = stringResource(R.string.nueva_bajada_profundidad),
                    esError = invalida,
                    tamanoTexto = 14.sp, conTextoApoyo = false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                )
                CampoTexto(
                    valor = bajada.tiempo,
                    onValorChange = { onEvento(EventoFormulario.BajadaTiempo(indice, it)) },
                    etiqueta = stringResource(R.string.nueva_bajada_tiempo),
                    visualTransformation = tiempoFondoVisualTransformation,
                    esError = invalida,
                    tamanoTexto = 14.sp, conTextoApoyo = false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                )
                IconButton(
                    onClick = { onEvento(EventoFormulario.QuitarBajada(indice)) },
                    enabled = activas.size > 1
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.nueva_bajada_borrar, indice + 1),
                        tint = if (activas.size > 1) ColorError else TextoSuperficieVariante.copy(alpha = 0.4f)
                    )
                }
            }
        }
        uiState.errores[Campo.BAJADAS]?.let {
            Text(it.resolver(), color = ColorError, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        }
        TextButton(onClick = { onEvento(EventoFormulario.AgregarBajada) }, modifier = Modifier.padding(top = 4.dp)) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = Acento, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.nueva_agregar_bajada), color = Acento, modifier = Modifier.padding(start = 8.dp))
        }
    }

    val si = stringResource(R.string.nueva_si)
    val no = stringResource(R.string.nueva_no)
    SelectorSegmentado(
        titulo = stringResource(R.string.nueva_sincope),
        opciones = listOf(false, true),
        seleccionada = uiState.sincope,
        etiqueta = { if (it) si else no },
        onSeleccion = { onEvento(EventoFormulario.Sincope(it ?: false)) },
        modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
    )

    // Resumen en vivo (se calcula con lo que se va cargando)
    uiState.resumenApnea?.let { r ->
        TarjetaCondicion(stringResource(R.string.nueva_resumen_apnea), resaltada = true) {
            FilaMosaicos {
                Mosaico(stringResource(R.string.nueva_resumen_bajadas), r.cantidad.toString())
                Mosaico(stringResource(R.string.nueva_resumen_prof_max), "${numeroCorto(r.profundidadMaximaMetros)} m")
            }
            if (r.tiempoTotalSegundos != null) {
                FilaMosaicos {
                    // La mejor bajada es la más profunda; su número es el de la lista del formulario
                    val numeroMejor = (uiState.indiceMejorBajada ?: r.indiceMejorBajada ?: 0) + 1
                    Mosaico(
                        stringResource(R.string.nueva_resumen_mejor),
                        stringResource(R.string.nueva_resumen_mejor_numero, numeroMejor),
                        stringResource(
                            R.string.nueva_resumen_mejor_detalle,
                            numeroCorto(r.profundidadMaximaMetros),
                            formatearDuracion(r.tiempoMejorBajadaSegundos ?: 0)
                        )
                    )
                    Mosaico(stringResource(R.string.nueva_resumen_tiempo_total), formatearDuracion(r.tiempoTotalSegundos ?: 0))
                }
                FilaMosaicos {
                    Mosaico(stringResource(R.string.nueva_resumen_promedio), formatearDuracion(r.tiempoPromedioSegundos ?: 0))
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** 18.0 → "18", 18.5 → "18.5" */
internal fun numeroCorto(v: Float): String = if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(v)

// Botón para cambiar el signo: muchos teclados numéricos no tienen la tecla "-"
@Composable
internal fun BotonSigno(onClick: () -> Unit) {
    val descripcion = stringResource(R.string.nueva_cambiar_signo)
    TextButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = descripcion } // para TalkBack
    ) {
        Text("±", style = MaterialTheme.typography.titleLarge, color = Acento)
    }
}
