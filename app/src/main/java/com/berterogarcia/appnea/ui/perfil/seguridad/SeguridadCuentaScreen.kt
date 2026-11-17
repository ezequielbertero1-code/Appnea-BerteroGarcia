package com.berterogarcia.appnea.ui.perfil.seguridad

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.components.BotonPeligro
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.CampoPassword
import com.berterogarcia.appnea.ui.components.EncabezadoGrupo
import com.berterogarcia.appnea.ui.components.TarjetaTonal
import com.berterogarcia.appnea.ui.components.resolver
import com.berterogarcia.appnea.ui.perfil.comun.CargandoPerfil
import com.berterogarcia.appnea.ui.perfil.comun.EncabezadoPerfil
import com.berterogarcia.appnea.ui.perfil.comun.MensajeError
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante

@Composable
fun SeguridadCuentaScreen(
    viewModel: SeguridadCuentaViewModel,
    onVolver: () -> Unit,
    /** La cuenta ya no existe: vuelve al login descartando todo lo de la sesión. */
    onCuentaBorrada: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val textoCambiada = stringResource(R.string.seguridad_cambiada)

    LaunchedEffect(s.passwordCambiada) {
        if (s.passwordCambiada) {
            Toast.makeText(context, textoCambiada, Toast.LENGTH_SHORT).show()
            viewModel.avisoMostrado()
        }
    }
    LaunchedEffect(s.cuentaBorrada) { if (s.cuentaBorrada) onCuentaBorrada() }

    if (s.cargando) {
        CargandoPerfil(modifier)
        return
    }

    Column(modifier.fillMaxSize().imePadding()) {
        EncabezadoPerfil(titulo = stringResource(R.string.seguridad_titulo), onVolver = onVolver)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MensajeError(s.errorGeneral?.resolver())

            EncabezadoGrupo(titulo = stringResource(R.string.seguridad_cambiar_titulo))
            if (s.esLocal) {
                CampoPassword(
                    valor = s.actual,
                    onValorChange = viewModel::onActualChange,
                    etiqueta = stringResource(R.string.seguridad_actual),
                    esError = CampoSeguridad.ACTUAL in s.errores,
                    mensajeError = s.errores[CampoSeguridad.ACTUAL]?.resolver().orEmpty(),
                    enabled = !s.ocupado,
                    modifier = Modifier.fillMaxWidth()
                )
                CampoPassword(
                    valor = s.nueva,
                    onValorChange = viewModel::onNuevaChange,
                    etiqueta = stringResource(R.string.seguridad_nueva),
                    esError = CampoSeguridad.NUEVA in s.errores,
                    mensajeError = s.errores[CampoSeguridad.NUEVA]?.resolver().orEmpty(),
                    enabled = !s.ocupado,
                    modifier = Modifier.fillMaxWidth()
                )
                CampoPassword(
                    valor = s.confirmacion,
                    onValorChange = viewModel::onConfirmacionChange,
                    etiqueta = stringResource(R.string.seguridad_confirmar),
                    esError = CampoSeguridad.CONFIRMACION in s.errores,
                    mensajeError = s.errores[CampoSeguridad.CONFIRMACION]?.resolver().orEmpty(),
                    imeAction = ImeAction.Done,
                    onImeAction = viewModel::cambiarPassword,
                    enabled = !s.ocupado,
                    modifier = Modifier.fillMaxWidth()
                )
                BotonPrincipal(
                    texto = stringResource(R.string.seguridad_cambiar),
                    onClick = viewModel::cambiarPassword,
                    cargando = s.cambiando,
                    enabled = !s.ocupado,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            } else {
                TarjetaTonal { Text(stringResource(R.string.seguridad_google), color = TextoSuperficieVariante) }
            }

            EncabezadoGrupo(titulo = stringResource(R.string.seguridad_borrar_titulo), modifier = Modifier.padding(top = 16.dp))
            Text(
                stringResource(R.string.seguridad_borrar_texto),
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSuperficieVariante
            )
            BotonPeligro(
                texto = stringResource(R.string.seguridad_borrar),
                onClick = viewModel::pedirBorrado,
                enabled = !s.ocupado && s.tipo != null,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }

    if (s.confirmandoBorrado) DialogoBorrarCuenta(s, viewModel)
}

@Composable
private fun DialogoBorrarCuenta(s: SeguridadUiState, viewModel: SeguridadCuentaViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::cancelarBorrado,
        icon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = ColorError) },
        title = { Text(stringResource(R.string.seguridad_borrar_confirmar_titulo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.seguridad_borrar_confirmar_texto))
                // Las cuentas locales confirman con la contraseña: que nadie la borre con el teléfono prestado
                if (s.esLocal) {
                    CampoPassword(
                        valor = s.passwordBorrado,
                        onValorChange = viewModel::onPasswordBorradoChange,
                        etiqueta = stringResource(R.string.seguridad_actual),
                        esError = CampoSeguridad.PASSWORD_BORRAR in s.errores,
                        mensajeError = s.errores[CampoSeguridad.PASSWORD_BORRAR]?.resolver().orEmpty(),
                        imeAction = ImeAction.Done,
                        onImeAction = viewModel::borrarCuenta,
                        enabled = !s.borrando
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = viewModel::borrarCuenta,
                enabled = !s.borrando,
                colors = ButtonDefaults.textButtonColors(contentColor = ColorError)
            ) { Text(stringResource(R.string.seguridad_borrar_confirmar)) }
        },
        dismissButton = {
            TextButton(onClick = viewModel::cancelarBorrado, enabled = !s.borrando) { Text(stringResource(R.string.accion_cancelar)) }
        }
    )
}
