package com.berterogarcia.appnea.ui.entrenamiento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.data.local.Fase
import com.berterogarcia.appnea.data.local.TablaApnea
import com.berterogarcia.appnea.data.local.TipoFase
import com.berterogarcia.appnea.data.local.formatearDuracion
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.BotonSecundario
import com.berterogarcia.appnea.ui.components.CampoTexto

@Composable
fun CreadorDeTablas(
    modifier: Modifier = Modifier,
    onTablaCreada: (TablaApnea) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    var nombre by remember { mutableStateOf("") }
    val fases = remember { mutableStateListOf<Fase>() }
    var tipoSeleccionado by remember { mutableStateOf(TipoFase.APNEA) }
    var minutosTexto by remember { mutableStateOf("1") }
    var segundosTexto by remember { mutableStateOf("0") }

    Column(modifier = modifier.fillMaxWidth()) {
        TextButton(onClick = { expandido = !expandido }) {
            Text(if (expandido) "Ocultar creador de tablas ▲" else "Crear mi propia tabla ▼")
        }

        if (expandido) {
            Column(modifier = Modifier.padding(8.dp)) {
                CampoTexto(
                    valor = nombre,
                    onValorChange = { nombre = it },
                    etiqueta = "Nombre de la tabla",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                Text("Agregar fase", style = MaterialTheme.typography.titleSmall)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = tipoSeleccionado == TipoFase.PREPARACION,
                        onClick = { tipoSeleccionado = TipoFase.PREPARACION },
                        label = { Text("Preparación") }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = tipoSeleccionado == TipoFase.APNEA,
                        onClick = { tipoSeleccionado = TipoFase.APNEA },
                        label = { Text("Apnea") }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = tipoSeleccionado == TipoFase.DESCANSO,
                        onClick = { tipoSeleccionado = TipoFase.DESCANSO },
                        label = { Text("Descanso") }
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CampoTexto(
                        valor = minutosTexto,
                        onValorChange = { minutosTexto = it.filter { c -> c.isDigit() } },
                        etiqueta = "Min",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    CampoTexto(
                        valor = segundosTexto,
                        onValorChange = { segundosTexto = it.filter { c -> c.isDigit() } },
                        etiqueta = "Seg",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    BotonSecundario(
                        texto = "Agregar",
                        onClick = {
                            val min = minutosTexto.toIntOrNull() ?: 0
                            val seg = segundosTexto.toIntOrNull() ?: 0
                            val totalSeg = min * 60 + seg
                            if (totalSeg > 0) fases.add(Fase(tipoSeleccionado, totalSeg))
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))
                if (fases.isNotEmpty()) {
                    Text("Fases agregadas:", style = MaterialTheme.typography.titleSmall)
                    fases.forEachIndexed { i, fase ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${i + 1}. ${fase.tipo} - ${formatearDuracion(fase.duracionSegundos)}")
                            TextButton(onClick = { fases.removeAt(i) }) { Text("Quitar") }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                BotonPrincipal(
                    texto = "Guardar tabla",
                    onClick = {
                        if (nombre.isNotBlank() && fases.isNotEmpty()) {
                            onTablaCreada(TablaApnea(nombre, fases.toList()))
                            nombre = ""
                            fases.clear()
                        }
                    },
                    enabled = nombre.isNotBlank() && fases.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}