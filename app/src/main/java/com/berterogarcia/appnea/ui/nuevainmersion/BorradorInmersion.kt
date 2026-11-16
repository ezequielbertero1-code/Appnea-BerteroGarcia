package com.berterogarcia.appnea.ui.nuevainmersion

import com.berterogarcia.appnea.data.local.TipoInmersion
import com.berterogarcia.appnea.data.local.Visibilidad
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lo que el usuario escribió en el formulario, como JSON, para guardarlo en el SavedStateHandle.
 *
 * Si Android mata la app mientras está en segundo plano (ej. el usuario fue a Ajustes a dar el
 * permiso de ubicación), al volver el formulario de 5 pasos aparece como lo dejó en vez de vacío.
 * Solo se guardan los campos editables: lo derivado (equipo del perfil, errores) se recalcula.
 */
internal fun NuevaInmersionUiState.aBorradorJson(): String = JSONObject().apply {
    put("etapa", etapa.name)
    put("tipo", tipo.name)
    put("fecha", fecha)
    put("lugar", lugar)
    latitud?.let { put("latitud", it) }
    longitud?.let { put("longitud", it) }
    put("profundidad", profundidadMetros)
    put("parada", paradaSeguridadMin)
    put("tiempoFondo", tiempoFondo)
    put("horaEntrada", horaEntrada)
    put("horaSalida", horaSalida)
    put("agua", JSONArray(caracteristicasAgua.toList()))
    put("usoLastre", usoLastre)
    put("lastre", lastreKg)
    put("usoProteccion", usoProteccion)
    put("proteccion", proteccionMm)
    put("equipamiento", JSONArray(equipamiento.toList()))
    put("tempAire", temperaturaAireC)
    put("tempAgua", temperaturaAguaC)
    visibilidad?.let { put("visibilidad", it.name) }
    put("visibilidadMetros", visibilidadMetros)
    put("cantidadBajadas", cantidadBajadas)
    put("detalleBajadas", cargarDetalleBajadas)
    put("bajadas", JSONArray(bajadas.map { JSONObject().put("p", it.profundidad).put("t", it.tiempo) }))
    put("sincope", sincope)
    put("actividad", actividad)
    put("comentarios", comentarios)
}.toString()

/** Aplica un borrador guardado. Si el JSON está roto, deja el estado como estaba. */
internal fun NuevaInmersionUiState.conBorrador(json: String): NuevaInmersionUiState = runCatching {
    val j = JSONObject(json)
    fun JSONArray?.textos(): List<String> = if (this == null) emptyList() else List(length()) { getString(it) }
    copy(
        etapa = runCatching { Etapa.valueOf(j.getString("etapa")) }.getOrDefault(etapa),
        tipo = TipoInmersion.desdeNombre(j.optString("tipo")),
        fecha = j.optString("fecha"),
        lugar = j.optString("lugar"),
        latitud = if (j.has("latitud")) j.getDouble("latitud") else null,
        longitud = if (j.has("longitud")) j.getDouble("longitud") else null,
        profundidadMetros = j.optString("profundidad"),
        paradaSeguridadMin = j.optString("parada"),
        tiempoFondo = j.optString("tiempoFondo"),
        horaEntrada = j.optString("horaEntrada"),
        horaSalida = j.optString("horaSalida"),
        caracteristicasAgua = j.optJSONArray("agua").textos().toSet(),
        usoLastre = j.optBoolean("usoLastre"),
        lastreKg = j.optString("lastre"),
        usoProteccion = j.optBoolean("usoProteccion"),
        proteccionMm = j.optString("proteccion"),
        equipamiento = j.optJSONArray("equipamiento").textos().toSet(),
        temperaturaAireC = j.optString("tempAire"),
        temperaturaAguaC = j.optString("tempAgua"),
        visibilidad = Visibilidad.desdeNombre(j.optString("visibilidad").ifEmpty { null }),
        visibilidadMetros = j.optString("visibilidadMetros"),
        cantidadBajadas = j.optString("cantidadBajadas"),
        cargarDetalleBajadas = j.optBoolean("detalleBajadas"),
        bajadas = j.optJSONArray("bajadas")?.let { a ->
            List(a.length()) { a.getJSONObject(it).let { b -> BajadaForm(b.optString("p"), b.optString("t")) } }
        }.orEmpty(),
        sincope = j.optBoolean("sincope"),
        actividad = j.optString("actividad"),
        comentarios = j.optString("comentarios")
    )
}.getOrDefault(this)
