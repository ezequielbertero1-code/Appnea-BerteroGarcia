package com.berterogarcia.appnea.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.auth.GoogleAuth
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.CampoPassword
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.alPresionarEnter
import com.berterogarcia.appnea.ui.components.resolver
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorAdvertencia
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante

/**
 * Ingreso (estilo "Appnea circular"): logo en contenedor redondeado, campos directamente sobre el
 * degradado, botón cian de ancho completo y "Creá una" abajo.
 */
@Composable
fun LoginScreen(viewModel: LoginViewModel, onLoginExitoso: () -> Unit, onRegistroClick: () -> Unit) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()

    val focoPassword = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val teclado = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    val irAPassword: () -> Unit = { focoPassword.requestFocus() }
    val intentarLogin: () -> Unit = {
        teclado?.hide()
        focusManager.clearFocus()
        viewModel.login()
    }

    LaunchedEffect(s.exitoso) {
        if (s.exitoso) onLoginExitoso()
    }
    // Si el usuario ya vino completo, el foco va directo a la contraseña.
    LaunchedEffect(Unit) {
        if (s.usuarioRecordado) runCatching { focoPassword.requestFocus() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Encabezado()
        Spacer(Modifier.height(36.dp))

        Column(Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.login_button),
                style = MaterialTheme.typography.titleMedium,
                color = TextoSuperficie,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            // Usuario: Enter (teclado físico) o "Siguiente" → pasa a contraseña.
            CampoTexto(
                valor = s.usuario,
                onValorChange = viewModel::onUsuarioChange,
                etiqueta = stringResource(R.string.login_username),
                esError = s.errorUsuario != null,
                mensajeError = s.errorUsuario?.resolver().orEmpty(),
                enabled = s.habilitado,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { irAPassword() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .alPresionarEnter(irAPassword)
            )

            // Contraseña: Enter o "Listo" → intenta iniciar sesión. El ojo muestra/oculta.
            CampoPassword(
                valor = s.password,
                onValorChange = viewModel::onPasswordChange,
                etiqueta = stringResource(R.string.login_password),
                esError = s.errorPassword != null,
                mensajeError = s.errorPassword?.resolver().orEmpty(),
                enabled = s.habilitado,
                imeAction = ImeAction.Done,
                onImeAction = intentarLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focoPassword)
                    .alPresionarEnter(intentarLogin)
            )

            MensajesEstado(s)

            BotonPrincipal(
                texto = when {
                    s.cargando -> stringResource(R.string.login_ingresando)
                    s.bloqueado -> stringResource(R.string.login_espera, s.bloqueadoSegundos)
                    else -> stringResource(R.string.login_button)
                },
                onClick = intentarLogin,
                enabled = s.habilitado,
                cargando = s.cargando,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            SeparadorO()

            OutlinedButton(
                onClick = {
                    teclado?.hide()
                    focusManager.clearFocus()
                    viewModel.loginConGoogle { GoogleAuth.pedirCuenta(context) }
                },
                enabled = s.googleHabilitado,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (s.cargandoGoogle) stringResource(R.string.login_google_cargando)
                    else stringResource(R.string.login_google),
                    color = if (s.googleHabilitado) TextoSuperficie else TextoSuperficieVariante
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text(stringResource(R.string.login_sin_cuenta), color = TextoSuperficieVariante)
            TextButton(onClick = onRegistroClick, enabled = !s.cargando) {
                Text(stringResource(R.string.login_crear_cuenta), color = Acento)
            }
        }
    }
}

/** Línea con "o" en el medio, entre el botón de contraseña y el de Google. */
@Composable
private fun SeparadorO() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        HorizontalDivider(Modifier.weight(1f), color = TextoSuperficieVariante.copy(alpha = 0.4f))
        Text(
            stringResource(R.string.login_o),
            color = TextoSuperficieVariante,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        HorizontalDivider(Modifier.weight(1f), color = TextoSuperficieVariante.copy(alpha = 0.4f))
    }
}

/** Logo en contenedor redondeado + nombre de la app + frase. */
@Composable
private fun Encabezado() {
    val forma = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(forma)
            .background(colorResource(R.color.ic_launcher_background))
            .border(1.dp, Acento.copy(alpha = 0.35f), forma),
        contentAlignment = Alignment.Center
    ) {
        // El foreground de un ícono adaptativo trae margen (el logo ocupa el centro del lienzo):
        // lo agrandamos un poco para que llene el contenedor.
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = stringResource(R.string.login_logo),
            modifier = Modifier
                .fillMaxSize()
                .scale(1.4f)
        )
    }
    Spacer(Modifier.height(20.dp))
    Text(
        text = stringResource(R.string.app_name).trim(),
        fontSize = 32.sp,
        lineHeight = 38.sp,
        color = TextoSuperficie
    )
    Text(
        text = stringResource(R.string.login_lema),
        style = MaterialTheme.typography.bodyMedium,
        color = TextoSuperficieVariante,
        textAlign = TextAlign.Center
    )
}

/** Error general, aviso de intentos restantes o bloqueo temporal. */
@Composable
private fun MensajesEstado(s: LoginUiState) {
    val errorGeneral = s.errorGeneral?.resolver()
    val aviso = s.aviso?.resolver()
    val (texto, color) = when {
        s.bloqueado -> stringResource(R.string.login_bloqueado, formatearEspera(s.bloqueadoSegundos)) to ColorAdvertencia
        errorGeneral != null -> errorGeneral to ColorError
        else -> return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(12.dp)
    ) {
        Text(texto, color = color, style = MaterialTheme.typography.bodyMedium)
        if (!s.bloqueado && aviso != null) {
            Text(aviso, color = ColorAdvertencia, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

private fun formatearEspera(segundos: Int): String =
    if (segundos < 60) "$segundos s" else "%d:%02d min".format(segundos / 60, segundos % 60)
