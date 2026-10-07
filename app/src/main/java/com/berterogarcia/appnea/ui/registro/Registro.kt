package com.berterogarcia.appnea.ui.registro

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.BotonSecundario
import com.berterogarcia.appnea.ui.components.CampoFecha
import com.berterogarcia.appnea.ui.components.CampoPassword
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.formatoLargo
import com.berterogarcia.appnea.ui.condiciones.Acento
import com.berterogarcia.appnea.ui.condiciones.FondoTarjeta
import com.berterogarcia.appnea.ui.condiciones.TextoSecundario
import java.time.LocalDate

private val RojoError = Color(0xFFFF6B6B)
private val Naranja = Color(0xFFFFB74D)
private val Verde = Color(0xFF81C784)

/**
 * Registro por pasos (mismo esquema que Nueva inmersión):
 * indicador arriba · contenido con scroll · botones fijos abajo (siempre arriba del teclado).
 */
@Composable
fun RegistroScreen(
    viewModel: RegistroViewModel,
    onCancelar: () -> Unit,
    onRegistroExitoso: () -> Unit
) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(s.registroOk) {
        if (s.registroOk) onRegistroExitoso()
    }

    // Atrás del sistema: paso anterior. En el primer paso actúa la navegación normal (vuelve al login).
    BackHandler(enabled = !s.esPrimerPaso) { viewModel.anterior() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 16.dp)
    ) {
        Text("Crear cuenta", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        IndicadorPasos(actual = s.paso, conErrores = s.pasosConError, onPasoClick = viewModel::irA)

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
                    .padding(top = 16.dp)
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
            Text(it, color = RojoError, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        }

        BotonesNavegacion(
            s = s,
            onAnterior = { if (!viewModel.anterior()) onCancelar() },
            onSiguiente = viewModel::siguiente,
            onCrear = viewModel::crearCuenta
        )
    }
}

// ===================================== Pasos =====================================

@Composable
private fun PasoCuenta(s: RegistroUiState, vm: RegistroViewModel) {
    Text(
        "Con estos datos vas a iniciar sesión.",
        color = TextoSecundario,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    CampoTexto(
        valor = s.usuario,
        onValorChange = vm::onUsuarioChange,
        etiqueta = stringResource(R.string.registro_username),
        esError = CampoRegistro.USUARIO in s.errores,
        mensajeError = s.errores[CampoRegistro.USUARIO].orEmpty(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
    CampoPassword(
        valor = s.password,
        onValorChange = vm::onPasswordChange,
        etiqueta = stringResource(R.string.registro_password),
        esError = CampoRegistro.PASSWORD in s.errores,
        mensajeError = s.errores[CampoRegistro.PASSWORD].orEmpty(),
        modifier = Modifier.fillMaxWidth()
    )
    BarraFuerza(s.fuerza)
    CampoPassword(
        valor = s.confirmacion,
        onValorChange = vm::onConfirmacionChange,
        etiqueta = stringResource(R.string.registro_confirmar_password),
        esError = CampoRegistro.CONFIRMACION in s.errores,
        mensajeError = s.errores[CampoRegistro.CONFIRMACION].orEmpty(),
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
    CampoTexto(
        valor = s.nombre, onValorChange = vm::onNombreChange, etiqueta = "Nombre *",
        esError = CampoRegistro.NOMBRE in s.errores, mensajeError = s.errores[CampoRegistro.NOMBRE].orEmpty(),
        keyboardOptions = palabras, modifier = Modifier.fillMaxWidth()
    )
    CampoTexto(
        valor = s.apellido, onValorChange = vm::onApellidoChange, etiqueta = "Apellido *",
        esError = CampoRegistro.APELLIDO in s.errores, mensajeError = s.errores[CampoRegistro.APELLIDO].orEmpty(),
        keyboardOptions = palabras, modifier = Modifier.fillMaxWidth()
    )
    CampoTexto(
        valor = s.email, onValorChange = vm::onEmailChange, etiqueta = "Email *",
        esError = CampoRegistro.EMAIL in s.errores, mensajeError = s.errores[CampoRegistro.EMAIL].orEmpty(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
    CampoTexto(
        valor = s.telefono, onValorChange = vm::onTelefonoChange, etiqueta = "Teléfono *",
        esError = CampoRegistro.TELEFONO in s.errores, mensajeError = s.errores[CampoRegistro.TELEFONO].orEmpty(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
    CampoFecha(
        etiqueta = "Fecha de nacimiento *",
        fecha = s.fechaNacimiento,
        onFechaChange = vm::onNacimientoChange,
        maxima = LocalDate.now(),
        esError = CampoRegistro.NACIMIENTO in s.errores,
        mensajeError = s.errores[CampoRegistro.NACIMIENTO],
        modifier = Modifier.fillMaxWidth()
    )
    CampoTexto(
        valor = s.ciudad, onValorChange = vm::onCiudadChange, etiqueta = "Ciudad",
        esError = CampoRegistro.CIUDAD in s.errores, mensajeError = s.errores[CampoRegistro.CIUDAD].orEmpty(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { vm.siguiente() }),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PasoEmergencia(s: RegistroUiState, vm: RegistroViewModel) {
    Text(
        "Es la persona a la que hay que avisar si te pasa algo en el agua. " +
                "Después podés agregar más contactos desde Cuenta.",
        color = TextoSecundario,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    CampoTexto(
        valor = s.contactoNombre, onValorChange = vm::onContactoNombreChange, etiqueta = "Nombre y apellido *",
        esError = CampoRegistro.CONTACTO_NOMBRE in s.errores,
        mensajeError = s.errores[CampoRegistro.CONTACTO_NOMBRE].orEmpty(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
    CampoTexto(
        valor = s.contactoRelacion, onValorChange = vm::onContactoRelacionChange,
        etiqueta = "Relación (ej: hermana, pareja, amigo)",
        esError = CampoRegistro.CONTACTO_RELACION in s.errores,
        mensajeError = s.errores[CampoRegistro.CONTACTO_RELACION].orEmpty(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
    CampoTexto(
        valor = s.contactoTelefono, onValorChange = vm::onContactoTelefonoChange, etiqueta = "Teléfono *",
        esError = CampoRegistro.CONTACTO_TELEFONO in s.errores,
        mensajeError = s.errores[CampoRegistro.CONTACTO_TELEFONO].orEmpty(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { vm.siguiente() }),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PasoConfirmar(s: RegistroUiState, vm: RegistroViewModel) {
    TarjetaResumen(
        titulo = "Cuenta",
        filas = listOf("Usuario" to s.usuario),
        onEditar = { vm.irA(PasoRegistro.CUENTA) }
    )
    TarjetaResumen(
        titulo = "Datos personales",
        filas = listOf(
            "Nombre" to "${s.nombre} ${s.apellido}".trim(),
            "Email" to s.email,
            "Teléfono" to s.telefono,
            "Nacimiento" to s.fechaNacimiento.formatoLargo(),
            "Ciudad" to s.ciudad.ifBlank { "-" }
        ),
        onEditar = { vm.irA(PasoRegistro.PERSONALES) }
    )
    TarjetaResumen(
        titulo = "Contacto de emergencia",
        filas = listOf(
            "Nombre" to s.contactoNombre,
            "Relación" to s.contactoRelacion.ifBlank { "-" },
            "Teléfono" to s.contactoTelefono
        ),
        onEditar = { vm.irA(PasoRegistro.EMERGENCIA) }
    )
    AvisoSeguridad(
        aceptado = s.aceptaAviso,
        error = s.errores[CampoRegistro.ACEPTA_AVISO],
        onCambio = vm::onAceptaAvisoChange
    )
}

// ================================== Componentes ==================================

/** Círculos numerados + barra de progreso + título del paso (como en Nueva inmersión). */
@Composable
private fun IndicadorPasos(
    actual: PasoRegistro,
    conErrores: Set<PasoRegistro>,
    onPasoClick: (PasoRegistro) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        PasoRegistro.entries.forEach { paso ->
            val visitado = paso.ordinal <= actual.ordinal
            val fondo = when {
                paso in conErrores -> RojoError
                visitado -> Acento
                else -> Color.White.copy(alpha = 0.15f)
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(fondo)
                    .clickable(enabled = paso.ordinal < actual.ordinal) { onPasoClick(paso) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${paso.ordinal + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (visitado || paso in conErrores) FondoTarjeta else Color.White
                )
            }
        }
    }
    LinearProgressIndicator(
        progress = { (actual.ordinal + 1f) / PasoRegistro.entries.size },
        color = Acento,
        trackColor = Color.White.copy(alpha = 0.15f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    )
    Text(actual.titulo, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 20.dp))
    Text(
        "Paso ${actual.ordinal + 1} de ${PasoRegistro.entries.size}",
        style = MaterialTheme.typography.bodyMedium,
        color = TextoSecundario
    )
}

/** Barra de fuerza de la contraseña (solo informativa). */
@Composable
private fun BarraFuerza(fuerza: FuerzaPassword) {
    if (fuerza == FuerzaPassword.VACIA) return
    val color = when (fuerza) {
        FuerzaPassword.DEBIL -> RojoError
        FuerzaPassword.MEDIA -> Naranja
        else -> Verde
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { fuerza.nivel / 3f },
            color = color,
            trackColor = Color.White.copy(alpha = 0.15f),
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
        Spacer(Modifier.width(12.dp))
        Text("Seguridad: ${fuerza.etiqueta}", color = color, style = MaterialTheme.typography.labelMedium)
    }
    if (fuerza != FuerzaPassword.FUERTE) {
        Text(
            "Tip: usá 12 o más caracteres, mezclá mayúsculas, números y algún símbolo.",
            color = TextoSecundario,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun TarjetaResumen(titulo: String, filas: List<Pair<String, String>>, onEditar: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta, contentColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    "Editar",
                    color = Acento,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onEditar)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            filas.forEach { (etiqueta, valor) ->
                Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = TextoSecundario, modifier = Modifier.padding(top = 8.dp))
                Text(valor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun AvisoSeguridad(aceptado: Boolean, error: String?, onCambio: (Boolean) -> Unit) {
    val borde = if (error != null) RojoError else Naranja.copy(alpha = 0.6f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borde, RoundedCornerShape(12.dp))
            .background(Naranja.copy(alpha = 0.08f))
            .padding(12.dp)
    ) {
        Text("Aviso de seguridad", fontWeight = FontWeight.Bold, color = Naranja)
        Text(
            "AppneaMe es un registro personal de tus inmersiones y entrenamientos. " +
                    "No reemplaza la formación con un instructor certificado ni el criterio en el agua. " +
                    "Las condiciones del mar son orientativas. Nunca bucees ni hagas apnea solo/a: " +
                    "siempre con un compañero que te vigile desde la superficie.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = 4.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onCambio(!aceptado) }
        ) {
            Checkbox(
                checked = aceptado,
                onCheckedChange = onCambio,
                colors = CheckboxDefaults.colors(
                    checkedColor = Acento,
                    uncheckedColor = if (error != null) RojoError else Color.White.copy(alpha = 0.6f),
                    checkmarkColor = FondoTarjeta
                )
            )
            Text("Leí y acepto el aviso de seguridad", style = MaterialTheme.typography.bodyMedium)
        }
        if (error != null) {
            Text(error, color = RojoError, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Botones fijos abajo: Volver/Anterior y Siguiente/Crear cuenta (con indicador mientras trabaja). */
@Composable
private fun BotonesNavegacion(
    s: RegistroUiState,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit,
    onCrear: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BotonSecundario(
            texto = if (s.esPrimerPaso) "Volver" else "Anterior",
            onClick = onAnterior,
            enabled = !s.ocupado,
            modifier = Modifier.weight(1f)
        )
        BotonPrincipal(
            texto = if (s.esUltimoPaso) stringResource(R.string.registro_button) else "Siguiente",
            onClick = if (s.esUltimoPaso) onCrear else onSiguiente,
            cargando = s.ocupado,
            modifier = Modifier.weight(1f)
        )
        if (s.ocupado) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = FondoTarjeta)
        } else {
            Text(if (s.esUltimoPaso) stringResource(R.string.registro_button) else "Siguiente")
        }
    }
}