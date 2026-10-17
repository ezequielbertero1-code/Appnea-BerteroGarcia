package com.berterogarcia.appnea.ui.perfil.contactos

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.FilaConSwitch
import com.berterogarcia.appnea.ui.components.resolver
import com.berterogarcia.appnea.ui.perfil.comun.CargandoPerfil
import com.berterogarcia.appnea.ui.perfil.comun.FormularioPerfil

@Composable
fun ContactoFormScreen(
    viewModel: ContactoFormViewModel,
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

    val telefono = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)

    FormularioPerfil(
        titulo = stringResource(if (s.esEdicion) R.string.perfil_contacto_editar else R.string.perfil_contacto_nuevo),
        onVolver = onVolver,
        guardando = s.guardando,
        habilitado = s.puedeGuardar,
        onGuardar = viewModel::guardar,
        errorGeneral = s.errorGeneral?.resolver(),
        modifier = modifier
    ) {
        CampoTexto(
            valor = s.nombre,
            onValorChange = viewModel::onNombreChange,
            etiqueta = stringResource(R.string.registro_contacto_nombre),
            esError = CampoContacto.NOMBRE in s.errores,
            mensajeError = s.errores[CampoContacto.NOMBRE]?.resolver().orEmpty(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.relacion,
            onValorChange = viewModel::onRelacionChange,
            etiqueta = stringResource(R.string.perfil_contacto_relacion),
            esError = CampoContacto.RELACION in s.errores,
            mensajeError = s.errores[CampoContacto.RELACION]?.resolver().orEmpty(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.telefono,
            onValorChange = viewModel::onTelefonoChange,
            etiqueta = stringResource(R.string.registro_telefono),
            esError = CampoContacto.TELEFONO in s.errores,
            mensajeError = s.errores[CampoContacto.TELEFONO]?.resolver().orEmpty(),
            keyboardOptions = telefono,
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.telefonoAlternativo,
            onValorChange = viewModel::onTelefonoAltChange,
            etiqueta = stringResource(R.string.perfil_contacto_alternativo),
            esError = CampoContacto.TELEFONO_ALT in s.errores,
            mensajeError = s.errores[CampoContacto.TELEFONO_ALT]?.resolver().orEmpty(),
            keyboardOptions = telefono,
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.notas,
            onValorChange = viewModel::onNotasChange,
            etiqueta = stringResource(R.string.perfil_contacto_notas),
            esError = CampoContacto.NOTAS in s.errores,
            mensajeError = s.errores[CampoContacto.NOTAS]?.resolver().orEmpty(),
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        FilaConSwitch(
            titulo = stringResource(R.string.perfil_contacto_principal),
            subtitulo = stringResource(
                when {
                    s.esPrimerContacto -> R.string.perfil_contacto_primero
                    s.eraPrincipal -> R.string.perfil_contacto_era_principal
                    else -> R.string.perfil_contacto_principal_ayuda
                }
            ),
            activado = s.esPrincipal,
            onCambio = viewModel::onPrincipalChange,
            enabled = !s.principalBloqueado,
            indice = 0,
            total = 1,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
