package com.berterogarcia.appnea.ui.registro

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.components.BarraAccionesInferior
import com.berterogarcia.appnea.ui.components.BarraSuperior
import com.berterogarcia.appnea.ui.components.BotonContorno
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.CampoFecha
import com.berterogarcia.appnea.ui.components.CampoPassword
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.FilaLista
import com.berterogarcia.appnea.ui.components.GrupoLista
import com.berterogarcia.appnea.ui.components.NavegacionBarra
import com.berterogarcia.appnea.ui.components.ProgresoPasosNavegable
import com.berterogarcia.appnea.ui.components.TarjetaAviso
import com.berterogarcia.appnea.ui.components.formatoLargo
import com.berterogarcia.appnea.ui.components.nombre
import com.berterogarcia.appnea.ui.components.resolver
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorAdvertencia
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.ColorOk
import com.berterogarcia.appnea.ui.theme.SuperficieTonalMaxima
import com.berterogarcia.appnea.ui.theme.TextoSobreAcentoFuerte
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante
import java.time.LocalDate

/**
 * Registro por pasos (mismo esquema que Nueva inmersión, estilo "Appnea circular"):
 * barra con flecha · progreso por pasos · contenido con scroll · acciones fijas abajo (arriba del teclado).
 */
@Composable
fun RegistroScreen(
    viewModel: RegistroViewModel,
    onCancelar: () -> Unit,
    onRegistroExitoso: () -> Unit
) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()
    val pasos = PasoRegistro.entries
    val onAnterior: () -> Unit = { if (!viewModel.anterior()) onCancelar() }

    LaunchedEffect(s.registroOk) {
        if (s.registroOk) onRegistroExitoso()
    }

    // Atrás del sistema: paso anterior. En el primer paso actúa la navegación normal (vuelve al login).
    BackHandler(enabled = !s.esPrimerPaso) { viewModel.anterior() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        BarraSuperior(
            titulo = stringResource(R.string.registro_titulo),
            navegacion = NavegacionBarra.VOLVER,
            onNavegacion = { if (!s.ocupado) onAnterior() }
        )
        Column(Modifier.padding(horizontal = 16.dp)) {
            ProgresoPasosNavegable(
                total = pasos.size,
                actual = s.paso.ordinal,
                conError = s.pasosConError.map { it.ordinal }.toSet(),
                onIrA = { viewModel.irA(pasos[it]) },
                descripcion = { stringResource(R.string.nueva_ir_a_paso, it + 1, pasos[it].nombre()) }
            )
            Text(
                stringResource(R.string.nueva_paso_de, s.paso.ordinal + 1, pasos.size, s.paso.nombre()),
                style = MaterialTheme.typography.bodySmall,
                color = TextoSuperficieVariante,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }

        AnimatedContent(
            targetState = s.paso,
            transitionSpec = {
                val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally { it * dir } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it * dir } + fadeOut())
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            label = "pasoRegistro"
        ) { paso ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
            ) {
                when (paso) {
                    PasoRegistro.CUENTA -> PasoCuenta(s, viewModel)
                    PasoRegistro.PERSONALES -> PasoPersonales(s, viewModel)
                    PasoRegistro.EMERGENCIA -> PasoEmergencia(s, viewModel)
                    PasoRegistro.CONFIRMAR -> PasoConfirmar(s, viewModel)
                }
            }
        }

        s.errorGeneral?.let {
            Text(
                it.resolver(),
                color = ColorError,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        BarraAccionesInferior {
            BotonContorno(
                texto = stringResource(if (s.esPrimerPaso) R.string.accion_volver else R.string.nueva_anterior),
                onClick = onAnterior,
                enabled = !s.ocupado,
                modifier = Modifier.weight(1f)
            )
            BotonPrincipal(
                texto = stringResource(if (s.esUltimoPaso) R.string.registro_button else R.string.nueva_siguiente),
                onClick = if (s.esUltimoPaso) viewModel::crearCuenta else viewModel::siguiente,
                cargando = s.ocupado,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ===================================== Pasos =====================================

@Composable
private fun PasoCuenta(s: RegistroUiState, vm: RegistroViewModel) {
    Texto(stringResource(R.string.registro_cuenta_ayuda))
    CampoTexto(
        valor = s.usuario,
        onValorChange = vm::onUsuarioChange,
        etiqueta = stringResource(R.string.registro_username),
        esError = CampoRegistro.USUARIO in s.errores,
        mensajeError = s.errores[CampoRegistro.USUARIO]?.resolver().orEmpty(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
    CampoPassword(
        valor = s.password,
        onValorChange = vm::onPasswordChange,
        etiqueta = stringResource(R.string.registro_password),
        esError = CampoRegistro.PASSWORD in s.errores,
        mensajeError = s.errores[CampoRegistro.PASSWORD]?.resolver().orEmpty(),
        modifier = Modifier.fillMaxWidth()
    )
    BarraFuerza(s.fuerza)
    CampoPassword(
        valor = s.confirmacion,
        onValorChange = vm::onConfirmacionChange,
        etiqueta = stringResource(R.string.registro_confirmar_password),
        esError = CampoRegistro.CONFIRMACION in s.errores,
        mensajeError = s.errores[CampoRegistro.CONFIRMACION]?.resolver().orEmpty(),
        imeAction = ImeAction.Done,
        onImeAction = vm::siguiente,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    )
}

@Composable
private fun PasoPersonales(s: RegistroUiState, vm: RegistroViewModel) {
    val palabras = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
    // Nombre y apellido lado a lado, como en el mockup de Datos personales
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CampoTexto(
            valor = s.nombre, onValorChange = vm::onNombreChange, etiqueta = stringResource(R.string.registro_nombre),
            esError = CampoRegistro.NOMBRE in s.errores, mensajeError = s.errores[CampoRegistro.NOMBRE]?.resolver().orEmpty(),
            keyboardOptions = palabras, modifier = Modifier.weight(1f)
        )
        CampoTexto(
            valor = s.apellido, onValorChange = vm::onApellidoChange, etiqueta = stringResource(R.string.registro_apellido),
            esError = CampoRegistro.APELLIDO in s.errores, mensajeError = s.errores[CampoRegistro.APELLIDO]?.resolver().orEmpty(),
            keyboardOptions = palabras, modifier = Modifier.weight(1f)
        )
    }
    CampoTexto(
        valor = s.email, onValorChange = vm::onEmailChange, etiqueta = stringResource(R.string.registro_email),
        esError = CampoRegistro.EMAIL in s.errores, mensajeError = s.errores[CampoRegistro.EMAIL]?.resolver().orEmpty(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
    CampoTexto(
        valor = s.telefono, onValorChange = vm::onTelefonoChange, etiqueta = stringResource(R.string.registro_telefono),
        esError = CampoRegistro.TELEFONO in s.errores, mensajeError = s.errores[CampoRegistro.TELEFONO]?.resolver().orEmpty(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
    CampoFecha(
        etiqueta = stringResource(R.string.registro_nacimiento),
        fecha = s.fechaNacimiento,
        onFechaChange = vm::onNacimientoChange,
        maxima = LocalDate.now(),
        esError = CampoRegistro.NACIMIENTO in s.errores,
        mensajeError = s.errores[CampoRegistro.NACIMIENTO]?.resolver(),
        modifier = Modifier.fillMaxWidth()
    )
    CampoTexto(
        valor = s.ciudad, onValorChange = vm::onCiudadChange, etiqueta = stringResource(R.string.registro_ciudad),
        esError = CampoRegistro.CIUDAD in s.errores, mensajeError = s.errores[CampoRegistro.CIUDAD]?.resolver().orEmpty(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { vm.siguiente() }),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PasoEmergencia(s: RegistroUiState, vm: RegistroViewModel) {
    Texto(stringResource(R.string.registro_emergencia_ayuda))
    CampoTexto(
        valor = s.contactoNombre, onValorChange = vm::onContactoNombreChange,
        etiqueta = stringResource(R.string.registro_contacto_nombre),
        esError = CampoRegistro.CONTACTO_NOMBRE in s.errores,
        mensajeError = s.errores[CampoRegistro.CONTACTO_NOMBRE]?.resolver().orEmpty(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
    CampoTexto(
        valor = s.contactoRelacion, onValorChange = vm::onContactoRelacionChange,
        etiqueta = stringResource(R.string.registro_contacto_relacion),
        esError = CampoRegistro.CONTACTO_RELACION in s.errores,
        mensajeError = s.errores[CampoRegistro.CONTACTO_RELACION]?.resolver().orEmpty(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
    CampoTexto(
        valor = s.contactoTelefono, onValorChange = vm::onContactoTelefonoChange,
        etiqueta = stringResource(R.string.registro_telefono),
        esError = CampoRegistro.CONTACTO_TELEFONO in s.errores,
        mensajeError = s.errores[CampoRegistro.CONTACTO_TELEFONO]?.resolver().orEmpty(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { vm.siguiente() }),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Resumen editable: cada sección es una fila con su ícono y "Editar"; después el aviso y el checkbox. */
@Composable
private fun PasoConfirmar(s: RegistroUiState, vm: RegistroViewModel) {
    val filas = listOf(
        Triple(R.drawable.ic_ms_person, stringResource(R.string.registro_resumen_usuario), s.usuario) to PasoRegistro.CUENTA,
        Triple(
            R.drawable.ic_ms_badge,
            stringResource(R.string.cuenta_datos_personales),
            listOf("${s.nombre} ${s.apellido}".trim(), s.email, s.telefono, s.fechaNacimiento.formatoLargo(), s.ciudad)
                .filter { it.isNotBlank() && it != "-" }
                .joinToString(" · ")
        ) to PasoRegistro.PERSONALES,
        Triple(
            R.drawable.ic_ms_emergency,
            stringResource(R.string.cuenta_contacto_emergencia),
            listOf(s.contactoNombre, s.contactoRelacion, s.contactoTelefono).filter { it.isNotBlank() }.joinToString(" · ")
        ) to PasoRegistro.EMERGENCIA
    )
    GrupoLista {
        filas.forEachIndexed { i, (datos, paso) ->
            val (icono, titulo, resumen) = datos
            FilaLista(
                titulo = titulo,
                subtitulo = resumen.ifBlank { "-" },
                indice = i,
                total = filas.size,
                inicio = { Icon(painterResource(icono), contentDescription = null) },
                fin = {
                    TextButton(onClick = { vm.irA(paso) }) { Text(stringResource(R.string.registro_editar), color = Acento) }
                }
            )
        }
    }
    TarjetaAviso(
        titulo = stringResource(R.string.registro_aviso_titulo),
        texto = stringResource(R.string.registro_aviso_texto),
        modifier = Modifier.padding(top = 16.dp)
    )
    AceptarAviso(
        aceptado = s.aceptaAviso,
        error = s.errores[CampoRegistro.ACEPTA_AVISO]?.resolver(),
        onCambio = vm::onAceptaAvisoChange
    )
}

// ================================== Componentes ==================================

@Composable
private fun Texto(texto: String) {
    Text(
        texto,
        color = TextoSuperficieVariante,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

/** Barra de fuerza de la contraseña (solo informativa). */
@Composable
private fun BarraFuerza(fuerza: FuerzaPassword) {
    if (fuerza == FuerzaPassword.VACIA) return
    val color = when (fuerza) {
        FuerzaPassword.DEBIL -> ColorError
        FuerzaPassword.MEDIA -> ColorAdvertencia
        else -> ColorOk
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        LinearProgressIndicator(
            progress = { fuerza.nivel / 3f },
            color = color,
            trackColor = SuperficieTonalMaxima,
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(12.dp))
        Text(stringResource(R.string.registro_seguridad, fuerza.nombre()), color = color, style = MaterialTheme.typography.labelMedium)
    }
    if (fuerza != FuerzaPassword.FUERTE) {
        Text(
            stringResource(R.string.registro_tip_password),
            color = TextoSuperficieVariante,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )
    }
}

/** Checkbox de aceptación: toda la fila se toca y TalkBack la anuncia como checkbox. */
@Composable
private fun AceptarAviso(aceptado: Boolean, error: String?, onCambio: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .toggleable(value = aceptado, role = Role.Checkbox, onValueChange = onCambio)
            .padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = aceptado,
            onCheckedChange = null, // lo maneja la fila
            colors = CheckboxDefaults.colors(
                checkedColor = Acento,
                uncheckedColor = if (error != null) ColorError else TextoSuperficieVariante,
                checkmarkColor = TextoSobreAcentoFuerte
            )
        )
        Text(stringResource(R.string.registro_acepto_aviso), style = MaterialTheme.typography.bodyMedium, color = TextoSuperficie)
    }
    if (error != null) {
        Text(error, color = ColorError, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 12.dp))
    }
}

/** Nombre de cada paso del registro. */
@Composable
private fun PasoRegistro.nombre(): String = stringResource(
    when (this) {
        PasoRegistro.CUENTA -> R.string.registro_paso_cuenta
        PasoRegistro.PERSONALES -> R.string.cuenta_datos_personales
        PasoRegistro.EMERGENCIA -> R.string.cuenta_contacto_emergencia
        PasoRegistro.CONFIRMAR -> R.string.registro_paso_confirmar
    }
)
