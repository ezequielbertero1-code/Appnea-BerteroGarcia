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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.CampoPassword
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.alPresionarEnter
import com.berterogarcia.appnea.ui.condiciones.Acento
import com.berterogarcia.appnea.ui.condiciones.FondoTarjeta
import com.berterogarcia.appnea.ui.condiciones.TextoSecundario

private val RojoError = Color(0xFFFF6B6B)
private val Naranja = Color(0xFFFFB74D)

@Composable
fun LoginScreen(viewModel: LoginViewModel, onLoginExitoso: () -> Unit, onRegistroClick: () -> Unit) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()

    val focoPassword = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val teclado = LocalSoftwareKeyboardController.current

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
        Spacer(Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = FondoTarjeta, contentColor = Color.White)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = if (s.usuarioRecordado) "Hola de nuevo, ${s.usuario}" else stringResource(R.string.login_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Usuario: Enter (teclado físico) o "Siguiente" → pasa a contraseña.
                CampoTexto(
                    valor = s.usuario,
                    onValorChange = viewModel::onUsuarioChange,
                    etiqueta = stringResource(R.string.login_username),
                    esError = s.errorUsuario != null,
                    mensajeError = s.errorUsuario.orEmpty(),
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
                    mensajeError = s.errorPassword.orEmpty(),
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
                        s.cargando -> "Ingresando…"
                        s.bloqueado -> "Esperá ${s.bloqueadoSegundos} s"
                        else -> stringResource(R.string.login_button)
                    },
                    onClick = intentarLogin,
                    enabled = s.habilitado,
                    cargando = s.cargando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )

                if (s.usuarioRecordado) {
                    TextButton(
                        onClick = viewModel::usarOtraCuenta,
                        enabled = s.habilitado,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("¿No sos ${s.usuario}? Usar otra cuenta", color = TextoSecundario)
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("¿No tenés cuenta?", color = TextoSecundario)
            TextButton(onClick = onRegistroClick, enabled = !s.cargando) {
                Text("Creá una", color = Acento, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Logo + nombre de la app + frase. */
@Composable
private fun Encabezado() {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(colorResource(R.color.ic_launcher_background))
            .border(1.dp, Acento.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // El foreground de un ícono adaptativo trae margen (el logo ocupa el centro del lienzo):
        // lo agrandamos un poco para que llene el círculo.
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = "Logo de AppneaMe",
            modifier = Modifier
                .fillMaxSize()
                .scale(1.4f)
        )
    }
    Spacer(Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.app_name).trim(),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
    Text(
        text = "Tu bitácora de apnea y buceo",
        style = MaterialTheme.typography.bodyMedium,
        color = TextoSecundario,
        textAlign = TextAlign.Center
    )
}

/** Error general, aviso de intentos restantes o bloqueo temporal. */
@Composable
private fun MensajesEstado(s: LoginUiState) {
    val errorGeneral = s.errorGeneral
    val aviso = s.aviso
    val (texto, color) = when {
        s.bloqueado -> "Demasiados intentos fallidos. Podés volver a intentar en ${formatearEspera(s.bloqueadoSegundos)}." to Naranja
        errorGeneral != null -> errorGeneral to RojoError
        else -> return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(10.dp)
    ) {
        Text(texto, color = color, style = MaterialTheme.typography.bodyMedium)
        if (!s.bloqueado && aviso != null) {
            Text(aviso, color = Naranja, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

private fun formatearEspera(segundos: Int): String =
    if (segundos < 60) "$segundos s" else "%d:%02d min".format(segundos / 60, segundos % 60)