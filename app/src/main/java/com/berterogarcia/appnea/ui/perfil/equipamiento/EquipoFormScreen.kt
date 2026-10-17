package com.berterogarcia.appnea.ui.perfil.equipamiento

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.CategoriaEquipo
import com.berterogarcia.appnea.ui.components.CampoFecha
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.FilaConSwitch
import com.berterogarcia.appnea.ui.components.SelectorOpciones
import com.berterogarcia.appnea.ui.components.nombre
import com.berterogarcia.appnea.ui.components.resolver
import com.berterogarcia.appnea.ui.perfil.comun.CargandoPerfil
import com.berterogarcia.appnea.ui.perfil.comun.FormularioPerfil
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
        titulo = stringResource(if (s.esEdicion) R.string.perfil_equipo_editar else R.string.perfil_equipo_nuevo),
        onVolver = onVolver,
        guardando = s.guardando,
        habilitado = s.puedeGuardar,
        onGuardar = viewModel::guardar,
        errorGeneral = s.errorGeneral?.resolver(),
        modifier = modifier
    ) {
        val categorias = CategoriaEquipo.entries.associateWith { it.nombre() }
        SelectorOpciones(
            titulo = stringResource(R.string.perfil_equipo_categoria),
            opciones = CategoriaEquipo.entries,
            seleccionada = s.categoria,
            etiqueta = { categorias.getValue(it) },
            onSeleccion = viewModel::onCategoriaChange,
            mensajeError = s.errores[CampoEquipo.CATEGORIA]?.resolver(),
            ayuda = stringResource(R.string.perfil_equipo_categoria_ayuda),
            intentoGuardado = s.intentoGuardado,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        CampoTexto(
            valor = s.marca,
            onValorChange = viewModel::onMarcaChange,
            etiqueta = stringResource(R.string.perfil_equipo_marca),
            esError = CampoEquipo.MARCA_MODELO in s.errores,
            mensajeError = "", // el mensaje se muestra una sola vez, debajo de "Modelo"
            keyboardOptions = siguiente,
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.modelo,
            onValorChange = viewModel::onModeloChange,
            etiqueta = stringResource(R.string.perfil_equipo_modelo),
            esError = CampoEquipo.MARCA_MODELO in s.errores,
            mensajeError = s.errores[CampoEquipo.MARCA_MODELO]?.resolver().orEmpty(),
            keyboardOptions = siguiente,
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.talle,
            onValorChange = viewModel::onTalleChange,
            etiqueta = stringResource(R.string.perfil_equipo_talle),
            esError = CampoEquipo.TALLE in s.errores,
            mensajeError = s.errores[CampoEquipo.TALLE]?.resolver().orEmpty(),
            keyboardOptions = siguiente,
            modifier = Modifier.fillMaxWidth()
        )
        CampoFecha(
            etiqueta = stringResource(R.string.perfil_equipo_compra),
            fecha = s.fechaCompra,
            onFechaChange = viewModel::onFechaCompraChange,
            maxima = LocalDate.now(),
            esError = CampoEquipo.FECHA_COMPRA in s.errores,
            mensajeError = s.errores[CampoEquipo.FECHA_COMPRA]?.resolver(),
            modifier = Modifier.fillMaxWidth()
        )
        CampoFecha(
            etiqueta = stringResource(R.string.perfil_equipo_service),
            fecha = s.proximoMantenimiento,
            onFechaChange = viewModel::onMantenimientoChange,
            minima = s.fechaCompra,
            esError = CampoEquipo.MANTENIMIENTO in s.errores,
            mensajeError = s.errores[CampoEquipo.MANTENIMIENTO]?.resolver(),
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.notas,
            onValorChange = viewModel::onNotasChange,
            etiqueta = stringResource(R.string.perfil_notas),
            esError = CampoEquipo.NOTAS in s.errores,
            mensajeError = s.errores[CampoEquipo.NOTAS]?.resolver().orEmpty(),
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        FilaConSwitch(
            titulo = stringResource(R.string.perfil_equipo_en_uso),
            subtitulo = stringResource(R.string.perfil_equipo_en_uso_ayuda),
            activado = s.enUso,
            onCambio = viewModel::onEnUsoChange,
            indice = 0,
            total = 1,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
