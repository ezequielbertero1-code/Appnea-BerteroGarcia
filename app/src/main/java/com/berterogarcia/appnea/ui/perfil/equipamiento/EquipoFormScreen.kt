package com.berterogarcia.appnea.ui.perfil.equipamiento

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.data.local.CategoriaEquipo
import com.berterogarcia.appnea.ui.components.CampoFecha
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.SelectorOpciones
import com.berterogarcia.appnea.ui.perfil.comun.CargandoPerfil
import com.berterogarcia.appnea.ui.perfil.comun.FormularioPerfil
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.TextoTerciario
import java.time.LocalDate

@Composable
fun EquipoFormScreen(
    viewModel: EquipoFormViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(s.guardadoOk) {
        if (s.guardadoOk) onVolver()
    }

    if (s.cargando) {
        CargandoPerfil(modifier)
        return
    }

    val siguiente = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)

    FormularioPerfil(
        titulo = if (s.esEdicion) "Editar equipo" else "Nuevo equipo",
        onVolver = onVolver,
        guardando = s.guardando,
        habilitado = s.puedeGuardar,
        onGuardar = viewModel::guardar,
        errorGeneral = s.errorGeneral,
        modifier = modifier
    ) {
        SelectorOpciones(
            titulo = "Categoría *",
            opciones = CategoriaEquipo.entries,
            seleccionada = s.categoria,
            etiqueta = { it.etiqueta },
            onSeleccion = viewModel::onCategoriaChange,
            mensajeError = s.errores[CampoEquipo.CATEGORIA],
            ayuda = "Elegí a qué categoría pertenece.",
            intentoGuardado = s.intentoGuardado,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        CampoTexto(
            valor = s.marca,
            onValorChange = viewModel::onMarcaChange,
            etiqueta = "Marca",
            esError = CampoEquipo.MARCA_MODELO in s.errores,
            mensajeError = "", // el mensaje se muestra una sola vez, debajo de "Modelo"
            keyboardOptions = siguiente,
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.modelo,
            onValorChange = viewModel::onModeloChange,
            etiqueta = "Modelo",
            esError = CampoEquipo.MARCA_MODELO in s.errores,
            mensajeError = s.errores[CampoEquipo.MARCA_MODELO].orEmpty(),
            keyboardOptions = siguiente,
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.talle,
            onValorChange = viewModel::onTalleChange,
            etiqueta = "Talle / medida / espesor (ej: L, 42-43, 5 mm)",
            esError = CampoEquipo.TALLE in s.errores,
            mensajeError = s.errores[CampoEquipo.TALLE].orEmpty(),
            keyboardOptions = siguiente,
            modifier = Modifier.fillMaxWidth()
        )
        CampoFecha(
            etiqueta = "Fecha de compra",
            fecha = s.fechaCompra,
            onFechaChange = viewModel::onFechaCompraChange,
            maxima = LocalDate.now(),
            esError = CampoEquipo.FECHA_COMPRA in s.errores,
            mensajeError = s.errores[CampoEquipo.FECHA_COMPRA],
            modifier = Modifier.fillMaxWidth()
        )
        CampoFecha(
            etiqueta = "Próximo service / revisión",
            fecha = s.proximoMantenimiento,
            onFechaChange = viewModel::onMantenimientoChange,
            minima = s.fechaCompra,
            esError = CampoEquipo.MANTENIMIENTO in s.errores,
            mensajeError = s.errores[CampoEquipo.MANTENIMIENTO],
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.notas,
            onValorChange = viewModel::onNotasChange,
            etiqueta = "Notas",
            esError = CampoEquipo.NOTAS in s.errores,
            mensajeError = s.errores[CampoEquipo.NOTAS].orEmpty(),
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("En uso")
                Text(
                    "Desactivalo si lo vendiste o ya no lo usás (queda en el historial).",
                    color = TextoTerciario
                )
            }
            Switch(
                checked = s.enUso,
                onCheckedChange = viewModel::onEnUsoChange,
                colors = SwitchDefaults.colors(checkedTrackColor = Acento)
            )
        }
    }
}
