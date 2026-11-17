package com.berterogarcia.appnea.ui.nuevainmersion

import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.dominio.modelo.TipoInmersion
import com.berterogarcia.appnea.ui.components.TextoUi
import com.berterogarcia.appnea.ui.components.textoUi
import java.time.LocalTime

/**
 * Reglas del formulario de inmersión. Función pura (sin Android ni ViewModel): recibe el estado
 * del formulario y devuelve los errores por campo. Así se puede testear sola.
 */
internal fun validarInmersion(s: NuevaInmersionUiState): Map<Campo, TextoUi> {
    val errores = mutableMapOf<Campo, TextoUi>()
    if (s.fecha.isBlank()) errores[Campo.FECHA] = textoUi(R.string.nueva_error_fecha)
    if (s.lugar.isBlank()) errores[Campo.LUGAR] = textoUi(R.string.nueva_error_lugar)
    val esApnea = s.tipo == TipoInmersion.APNEA
    // Apnea con detalle: la profundidad máxima sale de las bajadas
    if (!(esApnea && s.cargarDetalleBajadas) && s.profundidadMetros.toFloatOrNull() == null)
        errores[Campo.PROFUNDIDAD] = textoUi(R.string.nueva_error_profundidad)
    if (esApnea) {
        if ((s.cantidadBajadas.toIntOrNull() ?: 0) < 1)
            errores[Campo.CANTIDAD_BAJADAS] = textoUi(R.string.nueva_error_cantidad_bajadas)
        if (s.cargarDetalleBajadas && (s.bajadasActivas.isEmpty() || s.bajadasActivas.any { !it.esValida() }))
            errores[Campo.BAJADAS] = textoUi(R.string.nueva_error_bajadas)
    }

    // La parada de seguridad solo existe en buceo con tanque
    if (s.tipo == TipoInmersion.BUCEO) {
        val parada = s.paradaSeguridadMin.toFloatOrNull()
        if (parada == null || parada < 3f) errores[Campo.PARADA] = textoUi(R.string.nueva_error_parada)
    }

    // En apnea el tiempo sale de las bajadas (si se cargaron), no se pide aparte
    if (!esApnea && !tiempoValido(s.tiempoFondo)) errores[Campo.TIEMPO_FONDO] = textoUi(R.string.nueva_error_tiempo)

    val horaEntrada = s.horaEntrada.takeIf { it.isNotBlank() }
        ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
    val horaSalida = s.horaSalida.takeIf { it.isNotBlank() }
        ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

    if (horaEntrada == null) errores[Campo.HORA_ENTRADA] = textoUi(R.string.nueva_error_hora_entrada)
    if (horaSalida == null) errores[Campo.HORA_SALIDA] = textoUi(R.string.nueva_error_hora_salida)
    if (horaEntrada != null && horaSalida != null && horaEntrada.isAfter(horaSalida)) {
        errores[Campo.HORA_SALIDA] = textoUi(R.string.nueva_error_salida_anterior)
    }

    // Lastre y protección son extras opcionales: solo se validan si se eligió "Sí"
    if (s.usoLastre) {
        val lastre = s.lastreKg.toFloatOrNull()
        if (lastre == null || lastre < 0f) errores[Campo.LASTRE] = textoUi(R.string.nueva_error_lastre)
    }
    if (s.usoProteccion) {
        val proteccion = s.proteccionMm.toFloatOrNull()
        if (proteccion == null || proteccion < 0f) errores[Campo.PROTECCION] = textoUi(R.string.nueva_error_proteccion)
    }

    val temperaturaAire = s.temperaturaAireC.toFloatOrNull()
    if (temperaturaAire == null || temperaturaAire < LimitesInmersion.TEMP_AIRE_MIN) errores[Campo.TEMP_AIRE] = textoUi(R.string.nueva_error_temp_minima, LimitesInmersion.TEMP_AIRE_MIN)
    else if (temperaturaAire > LimitesInmersion.TEMP_AIRE_MAX) errores[Campo.TEMP_AIRE] = textoUi(R.string.nueva_error_temp_maxima, LimitesInmersion.TEMP_AIRE_MAX)

    val temperaturaAgua = s.temperaturaAguaC.toFloatOrNull()
    if (temperaturaAgua == null || temperaturaAgua < LimitesInmersion.TEMP_AGUA_MIN) errores[Campo.TEMP_AGUA] = textoUi(R.string.nueva_error_temp_minima, LimitesInmersion.TEMP_AGUA_MIN)
    else if (temperaturaAgua > LimitesInmersion.TEMP_AGUA_MAX) errores[Campo.TEMP_AGUA] = textoUi(R.string.nueva_error_temp_maxima, LimitesInmersion.TEMP_AGUA_MAX)

    // Visibilidad (escala y metros) es opcional; el campo de metros solo acepta dígitos
    return errores
}
