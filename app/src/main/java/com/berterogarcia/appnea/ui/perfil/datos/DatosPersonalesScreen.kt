package com.berterogarcia.appnea.ui.perfil.datos

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.ui.components.CampoFecha
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.perfil.comun.CargandoPerfil
import com.berterogarcia.appnea.ui.perfil.comun.FormularioPerfil
import java.time.LocalDate

@Composable
fun DatosPersonalesScreen(
    viewModel: DatosPersonalesViewModel,
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

    // Opciones de teclado: "Siguiente" pasa al próximo campo solo (comportamiento por defecto de ImeAction.Next).
    val textoNombre = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)

    FormularioPerfil(
        titulo = "Datos personales",
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
            etiqueta = "Nombre *",
            esError = CampoDatos.NOMBRE in s.errores,
            mensajeError = s.errores[CampoDatos.NOMBRE].orEmpty(),
            keyboardOptions = textoNombre,
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.apellido,
            onValorChange = viewModel::onApellidoChange,
            etiqueta = "Apellido",
            esError = CampoDatos.APELLIDO in s.errores,
            mensajeError = s.errores[CampoDatos.APELLIDO].orEmpty(),
            keyboardOptions = textoNombre,
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.email,
            onValorChange = viewModel::onEmailChange,
            etiqueta = "Email",
            esError = CampoDatos.EMAIL in s.errores,
            mensajeError = s.errores[CampoDatos.EMAIL].orEmpty(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.telefono,
            onValorChange = viewModel::onTelefonoChange,
            etiqueta = "Teléfono",
            esError = CampoDatos.TELEFONO in s.errores,
            mensajeError = s.errores[CampoDatos.TELEFONO].orEmpty(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        CampoFecha(
            etiqueta = "Fecha de nacimiento",
            fecha = s.fechaNacimiento,
            onFechaChange = viewModel::onFechaNacimientoChange,
            maxima = LocalDate.now(),
            esError = CampoDatos.FECHA_NACIMIENTO in s.errores,
            mensajeError = s.errores[CampoDatos.FECHA_NACIMIENTO],
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.ciudad,
            onValorChange = viewModel::onCiudadChange,
            etiqueta = "Ciudad",
            esError = CampoDatos.CIUDAD in s.errores,
            mensajeError = s.errores[CampoDatos.CIUDAD].orEmpty(),
            keyboardOptions = textoNombre,
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.companeroHabitual,
            onValorChange = viewModel::onCompaneroChange,
            etiqueta = "Compañero/a habitual de buceo",
            esError = CampoDatos.COMPANERO in s.errores,
            mensajeError = s.errores[CampoDatos.COMPANERO].orEmpty(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
