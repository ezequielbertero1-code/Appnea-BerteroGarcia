package com.berterogarcia.appnea.ui.perfil.contactos

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.ui.components.CampoTexto
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
        titulo = if (s.esEdicion) "Editar contacto" else "Nuevo contacto de emergencia",
        onVolver = onVolver,
        guardando = s.guardando,
        habilitado = s.puedeGuardar,
        onGuardar = viewModel::guardar,
        errorGeneral = s.errorGeneral,
        modifier = modifier
    ) {
        CampoTexto(
            valor = s.nombre,
            onValorChange = viewModel::onNombreChange,
            etiqueta = "Nombre y apellido *",
            esError = CampoContacto.NOMBRE in s.errores,
            mensajeError = s.errores[CampoContacto.NOMBRE].orEmpty(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.relacion,
            onValorChange = viewModel::onRelacionChange,
            etiqueta = "Relación (ej: hermana, pareja, compañero)",
            esError = CampoContacto.RELACION in s.errores,
            mensajeError = s.errores[CampoContacto.RELACION].orEmpty(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.telefono,
            onValorChange = viewModel::onTelefonoChange,
            etiqueta = "Teléfono *",
            esError = CampoContacto.TELEFONO in s.errores,
            mensajeError = s.errores[CampoContacto.TELEFONO].orEmpty(),
            keyboardOptions = telefono,
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.telefonoAlternativo,
            onValorChange = viewModel::onTelefonoAltChange,
            etiqueta = "Teléfono alternativo",
            esError = CampoContacto.TELEFONO_ALT in s.errores,
            mensajeError = s.errores[CampoContacto.TELEFONO_ALT].orEmpty(),
            keyboardOptions = telefono,
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.notas,
            onValorChange = viewModel::onNotasChange,
            etiqueta = "Notas (obra social, aclaraciones...)",
            esError = CampoContacto.NOTAS in s.errores,
            mensajeError = s.errores[CampoContacto.NOTAS].orEmpty(),
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
                Text("Contacto principal")
                Text(
                    text = when {
                        s.esPrimerContacto -> "Es tu primer contacto, queda como principal."
                        s.eraPrincipal -> "Para cambiarlo, marcá otro contacto como principal."
                        else -> "Es el que se muestra primero en tu cuenta."
                    },
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Switch(
                checked = s.esPrincipal,
                onCheckedChange = viewModel::onPrincipalChange,
                enabled = !s.principalBloqueado,
                colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF16F7FB))
            )
        }
    }
}
