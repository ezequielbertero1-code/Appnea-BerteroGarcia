package com.berterogarcia.appnea.ui.nuevainmersion

import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.dominio.modelo.TipoInmersion
import com.berterogarcia.appnea.dominio.modelo.Visibilidad
import com.berterogarcia.appnea.ui.components.textoUi

/**
 * Todo lo que el usuario puede hacer en el formulario de inmersión. Las pantallas reciben un
 * único `onEvento` (no el ViewModel), así se pueden previsualizar y probar por separado.
 */
sealed interface EventoFormulario {
    // ---- Navegación entre etapas ----
    data object Siguiente : EventoFormulario
    data object Anterior : EventoFormulario
    /** Solo vuelve a etapas anteriores (las de adelante se alcanzan con "Siguiente"). */
    data class IrA(val etapa: Etapa) : EventoFormulario
    data object Guardar : EventoFormulario

    // ---- General ----
    data class Tipo(val tipo: TipoInmersion) : EventoFormulario
    data class Fecha(val valor: String) : EventoFormulario
    data class Lugar(val valor: String) : EventoFormulario
    data class HoraEntrada(val valor: String) : EventoFormulario
    data class HoraSalida(val valor: String) : EventoFormulario
    /** Viene del mapa. Si el nombre quedó vacío, se mantiene el que ya estaba escrito. */
    data class UbicacionElegida(val latitud: Double, val longitud: Double, val nombre: String) : EventoFormulario
    data object QuitarUbicacion : EventoFormulario
    /** Botón "Mi ubicación" de la tarjeta (la pantalla ya pidió el permiso). */
    data object UsarMiUbicacion : EventoFormulario
    data object PermisoUbicacionDenegado : EventoFormulario

    // ---- Inmersión ----
    data class Profundidad(val valor: String) : EventoFormulario
    data class TiempoFondo(val valor: String) : EventoFormulario
    data class Parada(val valor: String) : EventoFormulario
    data class CantidadBajadas(val valor: String) : EventoFormulario
    data class DetalleBajadas(val activo: Boolean) : EventoFormulario
    data object AgregarBajada : EventoFormulario
    data class QuitarBajada(val indice: Int) : EventoFormulario
    data class BajadaProfundidad(val indice: Int, val valor: String) : EventoFormulario
    data class BajadaTiempo(val indice: Int, val valor: String) : EventoFormulario
    data class Sincope(val hubo: Boolean) : EventoFormulario

    // ---- Condiciones ----
    /** Grupos de una sola opción (Dulce/Salada, Orilla/Bote). [opcion] null = ninguna. */
    data class AguaUnica(val grupo: List<String>, val opcion: String?) : EventoFormulario
    data class AguaMarcada(val opcion: String, val marcado: Boolean) : EventoFormulario
    data class TemperaturaAire(val valor: String) : EventoFormulario
    data class TemperaturaAgua(val valor: String) : EventoFormulario
    data object AlternarSignoAire : EventoFormulario
    data object AlternarSignoAgua : EventoFormulario
    data class VisibilidadElegida(val visibilidad: Visibilidad?) : EventoFormulario
    data class VisibilidadMetros(val valor: String) : EventoFormulario

    // ---- Equipo ----
    data class EquipoMarcado(val etiqueta: String, val marcado: Boolean) : EventoFormulario
    data class UsoLastre(val usado: Boolean) : EventoFormulario
    data class Lastre(val valor: String) : EventoFormulario
    data class UsoProteccion(val usado: Boolean) : EventoFormulario
    data class ProteccionMm(val valor: String) : EventoFormulario

    // ---- Notas ----
    data class Actividad(val valor: String) : EventoFormulario
    data class Comentarios(val valor: String) : EventoFormulario
}

/**
 * Cómo cambia el formulario con cada evento. Es una función pura (sin corrutinas ni
 * repositorios): los eventos con efectos (guardar, buscar la ubicación) los resuelve el
 * ViewModel y acá no cambian nada.
 */
internal fun reducir(s: NuevaInmersionUiState, e: EventoFormulario): NuevaInmersionUiState = when (e) {
    EventoFormulario.Siguiente -> {
        val erroresEtapa = validarInmersion(s).filterKeys { it in s.etapa.campos }
        val sig = Etapa.entries.getOrNull(s.etapa.ordinal + 1)
        when {
            erroresEtapa.isNotEmpty() -> s.copy(errores = s.errores - s.etapa.campos + erroresEtapa)
            sig == null -> s
            else -> s.copy(etapa = sig, errores = s.errores - s.etapa.campos)
        }
    }
    EventoFormulario.Anterior -> Etapa.entries.getOrNull(s.etapa.ordinal - 1)?.let { s.copy(etapa = it) } ?: s
    is EventoFormulario.IrA -> if (e.etapa.ordinal < s.etapa.ordinal) s.copy(etapa = e.etapa) else s

    // Cambiar a Apnea borra el error de parada (en apnea no se pide)
    is EventoFormulario.Tipo -> s.copy(tipo = e.tipo, errores = s.errores - Campo.PARADA)
    is EventoFormulario.Fecha -> s.copy(fecha = e.valor)
    is EventoFormulario.Lugar -> s.copy(lugar = e.valor)
    is EventoFormulario.HoraEntrada -> s.copy(horaEntrada = e.valor)
    is EventoFormulario.HoraSalida -> s.copy(horaSalida = e.valor)
    is EventoFormulario.UbicacionElegida -> {
        val nuevoLugar = e.nombre.trim().ifBlank { s.lugar }
        s.copy(
            latitud = e.latitud, longitud = e.longitud, lugar = nuevoLugar, avisoUbicacion = null,
            errores = if (nuevoLugar.isBlank()) s.errores else s.errores - Campo.LUGAR
        )
    }
    EventoFormulario.QuitarUbicacion -> s.copy(latitud = null, longitud = null)
    EventoFormulario.PermisoUbicacionDenegado -> s.copy(avisoUbicacion = textoUi(R.string.nueva_aviso_sin_permiso))

    is EventoFormulario.Profundidad -> s.copy(profundidadMetros = e.valor)
    is EventoFormulario.TiempoFondo -> s.copy(tiempoFondo = e.valor.filter { it.isDigit() }.take(4))
    is EventoFormulario.Parada -> s.copy(paradaSeguridadMin = e.valor.filter { it.isDigit() || it == '.' })
    is EventoFormulario.CantidadBajadas -> {
        val cantidad = e.valor.filter { it.isDigit() }.take(2)
        val n = cantidad.toIntOrNull()
        s.copy(
            cantidadBajadas = cantidad,
            // Con el detalle activo la lista acompaña a la cantidad (si está vacía no se toca)
            bajadas = if (s.cargarDetalleBajadas && n != null && n >= 1) ajustarBajadas(s.bajadas, n) else s.bajadas,
            errores = s.errores - Campo.CANTIDAD_BAJADAS
        )
    }
    // Activar el detalle pre-genera una fila por cada bajada de la cantidad ya ingresada.
    // Al desactivarlo, la profundidad máxima queda con la de la bajada más profunda cargada.
    is EventoFormulario.DetalleBajadas -> {
        val cantidad = s.cantidadBajadas.toIntOrNull()?.coerceAtLeast(1) ?: s.bajadas.size.coerceAtLeast(1)
        val maxima = s.bajadasActivas.mapNotNull { it.profundidad.toFloatOrNull() }.maxOrNull()
        s.copy(
            cargarDetalleBajadas = e.activo,
            cantidadBajadas = if (e.activo) cantidad.toString() else s.cantidadBajadas,
            bajadas = if (e.activo) ajustarBajadas(s.bajadas, cantidad) else s.bajadas,
            profundidadMetros = if (!e.activo && maxima != null && maxima > 0f) numeroEditable(maxima) else s.profundidadMetros,
            errores = s.errores - Campo.BAJADAS - Campo.CANTIDAD_BAJADAS - Campo.PROFUNDIDAD
        )
    }
    EventoFormulario.AgregarBajada -> {
        val nuevas = s.bajadasActivas + BajadaForm()
        s.copy(bajadas = nuevas, cantidadBajadas = nuevas.size.toString(), errores = s.errores - Campo.CANTIDAD_BAJADAS)
    }
    is EventoFormulario.QuitarBajada -> s.copy(
        bajadas = s.bajadas.filterIndexed { i, _ -> i != e.indice },
        cantidadBajadas = (s.bajadasActivas.size - 1).toString()
    )
    is EventoFormulario.BajadaProfundidad -> s.conBajada(e.indice) {
        it.copy(profundidad = e.valor.replace(',', '.').filter { c -> c.isDigit() || c == '.' }.take(5))
    }
    is EventoFormulario.BajadaTiempo -> s.conBajada(e.indice) { it.copy(tiempo = e.valor.filter { c -> c.isDigit() }.take(4)) }
    is EventoFormulario.Sincope -> s.copy(sincope = e.hubo)

    is EventoFormulario.AguaUnica -> s.copy(caracteristicasAgua = s.caracteristicasAgua - e.grupo.toSet() + listOfNotNull(e.opcion))
    is EventoFormulario.AguaMarcada -> s.copy(
        caracteristicasAgua = if (e.marcado) s.caracteristicasAgua + e.opcion else s.caracteristicasAgua - e.opcion
    )
    is EventoFormulario.TemperaturaAire -> s.copy(temperaturaAireC = filtrarTemperatura(e.valor))
    is EventoFormulario.TemperaturaAgua -> s.copy(temperaturaAguaC = filtrarTemperatura(e.valor))
    EventoFormulario.AlternarSignoAire -> s.copy(temperaturaAireC = alternarSigno(s.temperaturaAireC))
    EventoFormulario.AlternarSignoAgua -> s.copy(temperaturaAguaC = alternarSigno(s.temperaturaAguaC))
    is EventoFormulario.VisibilidadElegida -> s.copy(visibilidad = e.visibilidad)
    is EventoFormulario.VisibilidadMetros -> s.copy(visibilidadMetros = e.valor.filter { it.isDigit() })

    is EventoFormulario.EquipoMarcado -> s.copy(
        equipamiento = if (e.marcado) s.equipamiento + e.etiqueta else s.equipamiento - e.etiqueta
    )
    is EventoFormulario.UsoLastre -> s.copy(
        usoLastre = e.usado, lastreKg = if (e.usado) s.lastreKg else "", errores = s.errores - Campo.LASTRE
    )
    is EventoFormulario.Lastre -> s.copy(lastreKg = e.valor.filter { it.isDigit() })
    is EventoFormulario.UsoProteccion -> s.copy(
        usoProteccion = e.usado, proteccionMm = if (e.usado) s.proteccionMm else "", errores = s.errores - Campo.PROTECCION
    )
    is EventoFormulario.ProteccionMm -> s.copy(proteccionMm = e.valor.filter { it.isDigit() })

    is EventoFormulario.Actividad -> s.copy(actividad = e.valor)
    is EventoFormulario.Comentarios -> s.copy(comentarios = e.valor)

    // Con efectos: los resuelve el ViewModel
    EventoFormulario.Guardar, EventoFormulario.UsarMiUbicacion -> s
}

/** Agrega bajadas vacías hasta tener [n]; nunca recorta (lo que sobra queda guardado, oculto). */
private fun ajustarBajadas(lista: List<BajadaForm>, n: Int): List<BajadaForm> =
    if (n <= lista.size) lista else lista + List(n - lista.size) { BajadaForm() }

private fun NuevaInmersionUiState.conBajada(indice: Int, cambio: (BajadaForm) -> BajadaForm) = copy(
    bajadas = bajadas.mapIndexed { i, b -> if (i == indice) cambio(b) else b },
    errores = errores - Campo.BAJADAS
)

/**
 * Normaliza la temperatura que escribe el usuario:
 * - acepta "," o "." como decimal (el teclado en español suele mostrar ",")
 * - un solo "-" y solo al principio (por si el teclado sí lo tiene, o se pega texto)
 * - un solo separador decimal y como mucho 6 caracteres ("-12.55")
 */
internal fun filtrarTemperatura(v: String): String {
    val normalizado = v.replace(',', '.')
    val signo = if (normalizado.startsWith("-")) "-" else ""
    val cuerpo = normalizado.filter { it.isDigit() || it == '.' }
    val punto = cuerpo.indexOf('.')
    val numero = if (punto == -1) cuerpo
    else cuerpo.substring(0, punto + 1) + cuerpo.substring(punto + 1).replace(".", "")
    return (signo + numero).take(6)
}

/** Pone o saca el "-" adelante, sin importar qué muestre el teclado. */
private fun alternarSigno(v: String): String = if (v.startsWith("-")) v.drop(1) else "-$v"
