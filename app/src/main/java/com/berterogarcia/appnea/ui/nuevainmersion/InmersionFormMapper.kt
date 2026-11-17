package com.berterogarcia.appnea.ui.nuevainmersion

import com.berterogarcia.appnea.dominio.modelo.Inmersion
import com.berterogarcia.appnea.dominio.modelo.TipoInmersion
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration.Companion.seconds

/*
 * Conversión formulario <-> entidad de Room, separada del ViewModel: son funciones puras
 * (sin estado ni corrutinas), así se testean solas y el ViewModel queda con la lógica de pantalla.
 */

/** Formulario (ya validado con [validarInmersion]) -> entidad. Las condiciones las pone RegistrarInmersion. */
internal fun NuevaInmersionUiState.aInmersion(): Inmersion {
    val esApnea = tipo == TipoInmersion.APNEA
    val registro = if (esApnea) registroApnea else null
    return Inmersion(
        fecha = LocalDate.parse(fecha, FORMATO_FECHA),
        lugar = lugar,
        // Apnea: la máxima del registro; buceo: lo escrito (ya validado)
        profundidadMetros = registro?.profundidadMaximaMetros ?: profundidadMetros.toFloat(),
        paradaSeguridadMin =
            if (tipo == TipoInmersion.APNEA) 0f
            else paradaSeguridadMin.toFloatOrNull() ?: 0f,
        // Apnea: suma de las bajadas cargadas (0 si no se cargó el detalle)
        tiempoFondo = if (esApnea) (registro?.resumen()?.tiempoTotalSegundos ?: 0).seconds
        else parseTiempoFondo(tiempoFondo), // "mm:ss" -> Duration
        horaEntrada = LocalTime.parse(horaEntrada),     // "HH:mm" -> LocalTime
        horaSalida = LocalTime.parse(horaSalida),
        caracteristicasAgua = caracteristicasAgua,
        lastreKg = if (usoLastre) lastreKg.toFloatOrNull() else null,
        proteccionMm = if (usoProteccion) proteccionMm.toFloatOrNull() else null,
        equipamiento = equipamiento,
        temperaturaAireC = temperaturaAireC.toFloatOrNull() ?: 0f,
        temperaturaAguaC = temperaturaAguaC.toFloatOrNull() ?: 0f,
        visibilidad = visibilidad,
        visibilidadMetros = visibilidadMetros.toFloatOrNull(),
        actividad = actividad,
        comentarios = comentarios,
        tipo = tipo,
        latitud = latitud,   // solo lo marcado; null si no se marcó nada
        longitud = longitud,
        apnea = registro
    )
}

/** Entidad guardada -> formulario, para editarla. */
internal fun NuevaInmersionUiState.conInmersion(i: Inmersion): NuevaInmersionUiState = copy(
    cargandoEdicion = false,
    tipo = i.tipo,
    fecha = i.fecha.format(FORMATO_FECHA),
    lugar = i.lugar,
    latitud = i.latitud,
    longitud = i.longitud,
    profundidadMetros = numeroEditable(i.profundidadMetros),
    paradaSeguridadMin = if (i.tipo == TipoInmersion.APNEA) "" else numeroEditable(i.paradaSeguridadMin),
    tiempoFondo = tiempoFondoEditable(i.tiempoFondo),
    horaEntrada = i.horaEntrada.format(FORMATO_HORA),
    horaSalida = i.horaSalida.format(FORMATO_HORA),
    caracteristicasAgua = i.caracteristicasAgua,
    usoLastre = i.lastreKg != null,
    lastreKg = enteroEditable(i.lastreKg),
    usoProteccion = i.proteccionMm != null,
    proteccionMm = enteroEditable(i.proteccionMm),
    equipamiento = i.equipamiento,
    temperaturaAireC = numeroEditable(i.temperaturaAireC),
    temperaturaAguaC = numeroEditable(i.temperaturaAguaC),
    visibilidad = i.visibilidad,
    visibilidadMetros = enteroEditable(i.visibilidadMetros),
    // Apnea guardada antes de las bajadas (sin registro): cuenta como una sola bajada
    cantidadBajadas = i.apnea?.cantidadBajadas?.toString() ?: if (i.tipo == TipoInmersion.APNEA) "1" else "",
    cargarDetalleBajadas = i.apnea?.bajadas?.isNotEmpty() == true,
    bajadas = i.apnea?.bajadas.orEmpty().map { b -> b.aForm() },
    sincope = i.apnea?.sincope ?: false,
    actividad = i.actividad,
    comentarios = i.comentarios
)
