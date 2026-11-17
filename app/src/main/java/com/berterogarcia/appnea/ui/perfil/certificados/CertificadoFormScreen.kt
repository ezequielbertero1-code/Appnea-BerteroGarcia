package com.berterogarcia.appnea.ui.perfil.certificados

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
import com.berterogarcia.appnea.dominio.modelo.TipoCertificado
import com.berterogarcia.appnea.ui.components.CampoFecha
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.SelectorOpciones
import com.berterogarcia.appnea.ui.components.nombre
import com.berterogarcia.appnea.ui.components.resolver
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
        titulo = stringResource(if (s.esEdicion) R.string.perfil_cert_editar else R.string.perfil_cert_nuevo),
        onVolver = onVolver,
        guardando = s.guardando,
        habilitado = s.puedeGuardar,
        onGuardar = viewModel::guardar,
        errorGeneral = s.errorGeneral?.resolver(),
        modifier = modifier
    ) {
        val tipos = TipoCertificado.entries.associateWith { it.nombre() }
        SelectorOpciones(
            titulo = stringResource(R.string.perfil_cert_tipo),
            opciones = TipoCertificado.entries,
            seleccionada = s.tipo,
            etiqueta = { tipos.getValue(it) },
            onSeleccion = viewModel::onTipoChange,
            mensajeError = s.errores[CampoCertificado.TIPO]?.resolver(),
            ayuda = stringResource(R.string.perfil_cert_tipo_ayuda),
            intentoGuardado = s.intentoGuardado,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        CampoTexto(
            valor = s.titulo,
            onValorChange = viewModel::onTituloChange,
            etiqueta = stringResource(R.string.perfil_cert_nivel),
            esError = CampoCertificado.TITULO in s.errores,
            mensajeError = s.errores[CampoCertificado.TITULO]?.resolver().orEmpty(),
            keyboardOptions = siguiente,
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.entidad,
            onValorChange = viewModel::onEntidadChange,
            etiqueta = stringResource(R.string.perfil_cert_entidad),
            esError = CampoCertificado.ENTIDAD in s.errores,
            mensajeError = s.errores[CampoCertificado.ENTIDAD]?.resolver().orEmpty(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.escuelaInstructor,
            onValorChange = viewModel::onEscuelaChange,
            etiqueta = stringResource(if (s.tipo == TipoCertificado.APTO_MEDICO) R.string.perfil_cert_medico else R.string.perfil_cert_escuela),
            esError = CampoCertificado.ESCUELA in s.errores,
            mensajeError = s.errores[CampoCertificado.ESCUELA]?.resolver().orEmpty(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.numero,
            onValorChange = viewModel::onNumeroChange,
            etiqueta = stringResource(R.string.perfil_cert_numero),
            esError = CampoCertificado.NUMERO in s.errores,
            mensajeError = s.errores[CampoCertificado.NUMERO]?.resolver().orEmpty(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        CampoFecha(
            etiqueta = stringResource(R.string.perfil_cert_emision),
            fecha = s.fechaEmision,
            onFechaChange = viewModel::onEmisionChange,
            maxima = LocalDate.now(),
            esError = CampoCertificado.EMISION in s.errores,
            mensajeError = s.errores[CampoCertificado.EMISION]?.resolver(),
            modifier = Modifier.fillMaxWidth()
        )
        CampoFecha(
            etiqueta = stringResource(if (s.vencimientoObligatorio) R.string.perfil_cert_vence_obligatorio else R.string.perfil_cert_vence_opcional),
            fecha = s.fechaVencimiento,
            onFechaChange = viewModel::onVencimientoChange,
            minima = s.fechaEmision,
            esError = CampoCertificado.VENCIMIENTO in s.errores,
            mensajeError = s.errores[CampoCertificado.VENCIMIENTO]?.resolver(),
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.notas,
            onValorChange = viewModel::onNotasChange,
            etiqueta = stringResource(R.string.perfil_notas),
            esError = CampoCertificado.NOTAS in s.errores,
            mensajeError = s.errores[CampoCertificado.NOTAS]?.resolver().orEmpty(),
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
