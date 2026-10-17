package com.berterogarcia.appnea.ui.perfil.datos

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.components.CampoFecha
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.resolver
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
        titulo = stringResource(R.string.cuenta_datos_personales),
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
            etiqueta = stringResource(R.string.registro_nombre),
            esError = CampoDatos.NOMBRE in s.errores,
            mensajeError = s.errores[CampoDatos.NOMBRE]?.resolver().orEmpty(),
            keyboardOptions = textoNombre,
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.apellido,
            onValorChange = viewModel::onApellidoChange,
            etiqueta = stringResource(R.string.perfil_apellido),
            esError = CampoDatos.APELLIDO in s.errores,
            mensajeError = s.errores[CampoDatos.APELLIDO]?.resolver().orEmpty(),
            keyboardOptions = textoNombre,
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.email,
            onValorChange = viewModel::onEmailChange,
            etiqueta = stringResource(R.string.perfil_email),
            esError = CampoDatos.EMAIL in s.errores,
            mensajeError = s.errores[CampoDatos.EMAIL]?.resolver().orEmpty(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.telefono,
            onValorChange = viewModel::onTelefonoChange,
            etiqueta = stringResource(R.string.perfil_telefono_campo),
            esError = CampoDatos.TELEFONO in s.errores,
            mensajeError = s.errores[CampoDatos.TELEFONO]?.resolver().orEmpty(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        CampoFecha(
            etiqueta = stringResource(R.string.perfil_nacimiento),
            fecha = s.fechaNacimiento,
            onFechaChange = viewModel::onFechaNacimientoChange,
            maxima = LocalDate.now(),
            esError = CampoDatos.FECHA_NACIMIENTO in s.errores,
            mensajeError = s.errores[CampoDatos.FECHA_NACIMIENTO]?.resolver(),
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.ciudad,
            onValorChange = viewModel::onCiudadChange,
            etiqueta = stringResource(R.string.registro_ciudad),
            esError = CampoDatos.CIUDAD in s.errores,
            mensajeError = s.errores[CampoDatos.CIUDAD]?.resolver().orEmpty(),
            keyboardOptions = textoNombre,
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            valor = s.companeroHabitual,
            onValorChange = viewModel::onCompaneroChange,
            etiqueta = stringResource(R.string.perfil_companero),
            esError = CampoDatos.COMPANERO in s.errores,
            mensajeError = s.errores[CampoDatos.COMPANERO]?.resolver().orEmpty(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
