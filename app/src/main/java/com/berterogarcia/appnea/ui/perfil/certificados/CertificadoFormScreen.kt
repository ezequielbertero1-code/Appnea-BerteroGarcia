package com.berterogarcia.appnea.ui.perfil.certificados

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.data.local.TipoCertificado
import com.berterogarcia.appnea.ui.components.CampoFecha
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.SelectorOpciones
import com.berterogarcia.appnea.ui.perfil.comun.CargandoPerfil
import com.berterogarcia.appnea.ui.perfil.comun.FormularioPerfil
import java.time.LocalDate

@Composable
fun CertificadoFormScreen(
    viewModel: CertificadoFormViewModel,
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
        titulo = if (s.esEdicion) "Editar certificación" else "Nueva certificación",
        onVolver = onVolver,
        guardando = s.guardando,
        habilitado = s.puedeGuardar,
        onGuardar = viewModel::guardar,
        errorGeneral = s.errorGeneral,
        modifier = modifier
    ) {
        SelectorOpciones(
            titulo = "Tipo de certificación *",
            opciones = TipoCertificado.entries,
            seleccionada = s.tipo,
            etiqueta = { it.etiqueta },
            onSeleccion = viewModel::onTipoChange,
            mensajeError = s.errores[CampoCertificado.TIPO],
            ayuda = "Elegí qué tipo de certificación es.",
            intentoGuardado = s.intentoGuardado,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        CampoTexto(
            valor = s.titulo,
            onValorChange = viewModel::onTituloChange,
            etiqueta = "Nivel / certificado * (ej: Buceador 2 Estrellas, AIDA 2)",
            esError = CampoCertificado.TITULO in s.errores,
            mensajeError = s.errores[CampoCertificado.TITULO].orEmpty(),
            keyboardOptions = siguiente,
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.entidad,
            onValorChange = viewModel::onEntidadChange,
            etiqueta = "Entidad (CMAS, PADI, SSI, AIDA...)",
            esError = CampoCertificado.ENTIDAD in s.errores,
            mensajeError = s.errores[CampoCertificado.ENTIDAD].orEmpty(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.escuelaInstructor,
            onValorChange = viewModel::onEscuelaChange,
            etiqueta = if (s.tipo == TipoCertificado.APTO_MEDICO) "Médico / institución" else "Escuela / instructor",
            esError = CampoCertificado.ESCUELA in s.errores,
            mensajeError = s.errores[CampoCertificado.ESCUELA].orEmpty(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.numero,
            onValorChange = viewModel::onNumeroChange,
            etiqueta = "Número de certificado",
            esError = CampoCertificado.NUMERO in s.errores,
            mensajeError = s.errores[CampoCertificado.NUMERO].orEmpty(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        CampoFecha(
            etiqueta = "Fecha de emisión",
            fecha = s.fechaEmision,
            onFechaChange = viewModel::onEmisionChange,
            maxima = LocalDate.now(),
            esError = CampoCertificado.EMISION in s.errores,
            mensajeError = s.errores[CampoCertificado.EMISION],
            modifier = Modifier.fillMaxWidth()
        )
        CampoFecha(
            etiqueta = if (s.vencimientoObligatorio) "Vencimiento *" else "Vencimiento (si tiene)",
            fecha = s.fechaVencimiento,
            onFechaChange = viewModel::onVencimientoChange,
            minima = s.fechaEmision,
            esError = CampoCertificado.VENCIMIENTO in s.errores,
            mensajeError = s.errores[CampoCertificado.VENCIMIENTO],
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.notas,
            onValorChange = viewModel::onNotasChange,
            etiqueta = "Notas",
            esError = CampoCertificado.NOTAS in s.errores,
            mensajeError = s.errores[CampoCertificado.NOTAS].orEmpty(),
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
