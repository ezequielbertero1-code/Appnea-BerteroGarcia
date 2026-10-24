package com.berterogarcia.appnea.ui.nuevainmersion

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.condiciones.CondicionesRepository
import com.berterogarcia.appnea.data.local.CategoriaEquipo
import com.berterogarcia.appnea.data.local.EquipoEntity
import com.berterogarcia.appnea.data.local.BajadaApnea
import com.berterogarcia.appnea.data.local.Geocodificador
import com.berterogarcia.appnea.data.local.InmersionEntity
import com.berterogarcia.appnea.data.local.RegistroApnea
import com.berterogarcia.appnea.data.local.ResumenApnea
import com.berterogarcia.appnea.data.local.TipoInmersion
import com.berterogarcia.appnea.data.local.UBICACION_COMODORO_RIVADAVIA
import com.berterogarcia.appnea.data.local.Ubicacion
import com.berterogarcia.appnea.data.local.UbicacionHelper
import com.berterogarcia.appnea.data.local.Visibilidad
import com.berterogarcia.appnea.data.local.descripcion
import com.berterogarcia.appnea.data.repository.InmersionRepository
import com.berterogarcia.appnea.data.repository.PerfilRepository
import com.berterogarcia.appnea.ui.components.TextoUi
import com.berterogarcia.appnea.ui.components.textoUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// Rangos válidos de temperatura (°C); se usan en la validación y en los mensajes de error
object LimitesInmersion {
    const val TEMP_AIRE_MIN = -60
    const val TEMP_AIRE_MAX = 45
    const val TEMP_AGUA_MIN = -2
    const val TEMP_AGUA_MAX = 40
}

enum class Campo { FECHA, LUGAR, PROFUNDIDAD, PARADA, TIEMPO_FONDO, HORA_ENTRADA, HORA_SALIDA, LASTRE, PROTECCION, TEMP_AIRE, TEMP_AGUA, VISIBILIDAD, CANTIDAD_BAJADAS, BAJADAS }

enum class Etapa(val campos: Set<Campo>) {
    GENERAL(setOf(Campo.FECHA, Campo.LUGAR, Campo.HORA_ENTRADA, Campo.HORA_SALIDA)),
    INMERSION(setOf(Campo.PROFUNDIDAD, Campo.TIEMPO_FONDO, Campo.PARADA, Campo.CANTIDAD_BAJADAS, Campo.BAJADAS)),
    CONDICIONES(setOf(Campo.TEMP_AIRE, Campo.TEMP_AGUA, Campo.VISIBILIDAD)),
    EQUIPO(setOf(Campo.LASTRE, Campo.PROTECCION)),
    NOTAS(emptySet())
}

private val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.US)
private val FORMATO_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
private val REGEX_TIEMPO_FONDO = Regex("^\\d{2}[0-5]\\d\$") // 4 dígitos, segundos 00-59

/** El campo se llena desde el final ("500" = 05:00), así que se valida el texto completado a 4 dígitos. */
private fun tiempoValido(v: String): Boolean = v.isNotEmpty() && REGEX_TIEMPO_FONDO.matches(v.padStart(4, '0'))

/** Categorías que solo tienen sentido en buceo con tanque: en Apnea no se ofrecen. */
private val SOLO_BUCEO = setOf(CategoriaEquipo.REGULADOR, CategoriaEquipo.CHALECO, CategoriaEquipo.TUBO)

private fun parseTiempoFondo(v: String): Duration {
    val digitos = v.padStart(4, '0')
    val minutos = digitos.substring(0, 2).toInt()
    val segundos = digitos.substring(2, 4).toInt()
    return (minutos * 60 + segundos).seconds
}

// ---- De lo guardado al texto de los campos (modo edición) ----

/** 18.0 → "18", 18.5 → "18.5" */
private fun numeroEditable(v: Float): String = if (v % 1f == 0f) v.toInt().toString() else v.toString()

/** Campos que solo aceptan dígitos (lastre, protección, visibilidad). */
private fun enteroEditable(v: Float?): String = v?.roundToInt()?.coerceAtLeast(0)?.toString() ?: ""

/** Una bajada en el formulario: profundidad en metros y tiempo "mmss" (solo dígitos). */
data class BajadaForm(val profundidad: String = "", val tiempo: String = "")

/** Tiempo "mmss" (4 dígitos, segundos 00-59) → segundos; null si no es válido. */
private fun tiempoEnSegundos(v: String): Int? =
    if (tiempoValido(v)) parseTiempoFondo(v).inWholeSeconds.toInt() else null

/** La bajada tiene profundidad > 0 y tiempo válido. */
fun BajadaForm.esValida(): Boolean =
    (profundidad.toFloatOrNull() ?: 0f) > 0f && tiempoEnSegundos(tiempo) != null

private fun BajadaForm.aBajadaApnea(): BajadaApnea? {
    val p = profundidad.toFloatOrNull() ?: return null
    val t = tiempoEnSegundos(tiempo) ?: return null
    return if (p > 0f) BajadaApnea(p, t) else null
}

private fun BajadaApnea.aForm() = BajadaForm(numeroEditable(profundidadMetros), tiempoFondoEditable(tiempoSegundos.seconds))

/** 41 min 05 s → "4105" (el campo guarda solo dígitos y muestra mm:ss). */
private fun tiempoFondoEditable(d: Duration): String {
    val total = d.inWholeSeconds.coerceAtLeast(0)
    val minutos = (total / 60).coerceAtMost(99)
    val segundos = if (total / 60 > 99) 59L else total % 60
    return "%02d%02d".format(minutos, segundos)
}

/**
 * Texto con el que un equipo del perfil queda guardado en la inmersión:
 * "Aletas · Mares Razor (42-43)". Se guarda TEXTO y no el id, así si después se borra o
 * edita el equipo, la inmersión vieja sigue mostrando lo que se usó.
 * Sin comas: el Set<String> se guarda separado por "," (ver Converters.fromSet).
 */
val EquipoEntity.etiquetaInmersion: String
    get() = (if (talle.isBlank()) descripcion else "$descripcion ($talle)").replace(',', ' ')

data class NuevaInmersionUiState(
    val etapa: Etapa = Etapa.GENERAL,
    val tipo: TipoInmersion = TipoInmersion.BUCEO,
    val fecha: String = "",
    val lugar: String = "",
    val latitud: Double? = null,          // lugar marcado (mapa o GPS); null = sin marcar
    val longitud: Double? = null,
    val buscandoUbicacion: Boolean = false,
    val avisoUbicacion: TextoUi? = null,
    val profundidadMetros: String = "",
    val paradaSeguridadMin: String = "",
    val tiempoFondo: String = "",
    val horaEntrada: String = "",
    val horaSalida: String = "",
    val caracteristicasAgua: Set<String> = emptySet(),
    val usoLastre: Boolean = false,       // "¿usaste más lastre?" (además del equipo del perfil)
    val lastreKg: String = "",
    val usoProteccion: Boolean = false,   // "¿usaste protección extra?"
    val proteccionMm: String = "",
    val equipamiento: Set<String> = emptySet(),          // etiquetas (etiquetaInmersion) marcadas
    val equipoPerfil: List<EquipoEntity> = emptyList(),  // equipo EN USO del usuario
    val equipoCargado: Boolean = false,                  // false mientras se lee de la base
    val temperaturaAireC: String = "",
    val temperaturaAguaC: String = "",
    val visibilidad: Visibilidad? = null,
    val visibilidadMetros: String = "",
    // Apnea: cantidad + profundidad máxima (profundidadMetros) siempre; detalle de cada bajada opcional
    val cantidadBajadas: String = "",
    val cargarDetalleBajadas: Boolean = false,
    val bajadas: List<BajadaForm> = emptyList(),
    val sincope: Boolean = false,
    val actividad: String = "",
    val comentarios: String = "",
    val guardadoOk: Boolean = false,
    val guardando: Boolean = false,
    val errores: Map<Campo, TextoUi> = emptyMap(),
    val editando: Boolean = false,          // true = se está editando una inmersión existente
    val cargandoEdicion: Boolean = false,   // leyendo la inmersión a editar
    val errorCarga: TextoUi? = null,         // no se pudo leer la inmersión a editar
    val errorGuardado: TextoUi? = null,
) {
    /** Equipo que se ofrece según el tipo (en Apnea sin regulador, chaleco ni tubo). */
    val equipoVisible: List<EquipoEntity>
        get() = equipoPerfil.filter { tipo == TipoInmersion.BUCEO || it.categoria !in SOLO_BUCEO }

    /** Registro de apnea tal como está cargado ahora (null si falta algo válido); alimenta el resumen en vivo. */
    val registroApnea: RegistroApnea?
        get() {
            if (cargarDetalleBajadas) {
                val validas = bajadasActivas.mapNotNull { it.aBajadaApnea() }
                if (validas.isEmpty()) return null
                return RegistroApnea(validas.size, validas.maxOf { it.profundidadMetros }, sincope, validas)
            }
            val cantidad = cantidadBajadas.toIntOrNull() ?: return null
            val profundidad = profundidadMetros.toFloatOrNull() ?: return null
            if (cantidad < 1) return null
            return RegistroApnea(cantidad, profundidad, sincope)
        }

    val resumenApnea: ResumenApnea? get() = registroApnea?.resumen()

    /**
     * Las bajadas que cuentan: las primeras [cantidadBajadas]. La lista completa se conserva aunque
     * se baje la cantidad (p. ej. al borrar "20" para escribir "7" pasa por "2"), así no se pierde lo cargado.
     */
    val bajadasActivas: List<BajadaForm>
        get() = cantidadBajadas.toIntOrNull()?.takeIf { it >= 1 }?.let { bajadas.take(it) } ?: bajadas

    private fun profundidadDe(b: BajadaForm) = b.profundidad.toFloatOrNull() ?: 0f

    /** La mejor bajada: la más profunda y, a igual profundidad, la más rápida. Posición en la lista del formulario. */
    val indiceMejorBajada: Int?
        get() {
            val activas = bajadasActivas
            val max = activas.maxOfOrNull { profundidadDe(it) } ?: return null
            if (max <= 0f) return null
            return activas.indices
                .filter { profundidadDe(activas[it]) == max }
                .minByOrNull { tiempoEnSegundos(activas[it].tiempo) ?: Int.MAX_VALUE }
        }
}

class NuevaInmersionViewModel(
    private val repository: InmersionRepository,
    perfilRepository: PerfilRepository,
    nombreUsuario: String?,
    private val appContext: Context,
    /** null = alta de una inmersión nueva; con id = edición de esa inmersión. */
    private val idEditar: Long? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        NuevaInmersionUiState(
            fecha = LocalDate.now().format(FORMATO_FECHA),
            editando = idEditar != null,
            cargandoEdicion = idEditar != null
        )
    )
    val uiState: StateFlow<NuevaInmersionUiState> = _uiState

    /** Inmersiones anteriores, para pre-marcar el equipo de la última del mismo tipo. */
    private var inmersionesPrevias: List<InmersionEntity> = emptyList()

    /** true cuando el usuario tocó algún equipo: desde ahí no se pre-marca nada solo. */
    private var equipoTocado = false

    /** La inmersión tal como estaba guardada (solo en modo edición). */
    private var original: InmersionEntity? = null

    init {
        if (idEditar != null) cargarParaEditar(idEditar)
        viewModelScope.launch {
            inmersionesPrevias = runCatching { repository.todasLasInmersiones.first() }
                .getOrDefault(emptyList())
            preseleccionarEquipo()
        }
        if (nombreUsuario == null) {
            _uiState.update { it.copy(equipoCargado = true) } // sin sesión: lista vacía, no ruedita eterna
        } else {
            viewModelScope.launch {
                perfilRepository.observarEquipamiento(nombreUsuario)
                    .catch { emit(emptyList()) } // si falla la base: lista vacía en vez de crashear
                    .collect { lista ->
                        _uiState.update {
                            it.copy(equipoPerfil = lista.filter { e -> e.enUso }, equipoCargado = true)
                        }
                        preseleccionarEquipo()
                    }
            }
        }
    }

    /**
     * Sin tocar: marca el equipo de la última inmersión del mismo tipo.
     * Tocado: respeta lo elegido. En los dos casos se descarta lo que ya no se ofrece
     * (equipo que se pasó a "no en uso", o regulador al cambiar a Apnea).
     */
    private fun preseleccionarEquipo() {
        // Editando: se respeta exactamente el equipo guardado (aunque ya no esté "en uso")
        if (idEditar != null) return
        _uiState.update { s ->
            val visibles = s.equipoVisible.map { it.etiquetaInmersion }.toSet()
            val base = if (equipoTocado) s.equipamiento else {
                inmersionesPrevias
                    .filter { it.tipo == s.tipo }
                    .maxWithOrNull(compareBy<InmersionEntity> { it.fecha }.thenBy { it.horaEntrada })
                    ?.equipamiento
                    .orEmpty()
            }
            s.copy(equipamiento = base intersect visibles)
        }
    }

    /** Lee la inmersión y completa el formulario con sus datos. */
    private fun cargarParaEditar(id: Long) {
        viewModelScope.launch {
            val i = runCatching { repository.obtenerPorId(id).first() }.getOrNull()
            if (i == null) {
                _uiState.update {
                    it.copy(cargandoEdicion = false, errorCarga = textoUi(R.string.nueva_error_no_encontrada))
                }
                return@launch
            }
            original = i
            equipoTocado = true
            _uiState.update {
                it.copy(
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
            }
        }
    }

    fun siguiente() {
        val s = _uiState.value
        val erroresEtapa = validar(s).filterKeys { it in s.etapa.campos }
        if (erroresEtapa.isNotEmpty()) {
            _uiState.update { it.copy(errores = it.errores - s.etapa.campos + erroresEtapa) }
            return
        }
        val sig = Etapa.entries.getOrNull(s.etapa.ordinal + 1) ?: return
        _uiState.update { it.copy(etapa = sig, errores = it.errores - s.etapa.campos) }
    }

    fun anterior() {
        val ant = Etapa.entries.getOrNull(_uiState.value.etapa.ordinal - 1) ?: return
        _uiState.update { it.copy(etapa = ant) }
    }

    fun irA(etapa: Etapa) {
        if (etapa.ordinal < _uiState.value.etapa.ordinal) _uiState.update { it.copy(etapa = etapa) }
    }

    fun onFechaChange(v: String) = _uiState.update { it.copy(fecha = v) }
    fun onLugarChange(v: String) = _uiState.update { it.copy(lugar = v) }
    /** Centro con el que abre el mapa: lo ya marcado → última ubicación del teléfono → Comodoro. */
    fun centroInicialMapa(): Ubicacion {
        val s = _uiState.value
        return if (s.latitud != null && s.longitud != null) Ubicacion(s.latitud, s.longitud)
        else UbicacionHelper.obtenerUltimaUbicacion(appContext) ?: UBICACION_COMODORO_RIVADAVIA
    }

    /** Viene del mapa. Si el nombre quedó vacío, se mantiene el que ya estaba escrito. */
    fun onUbicacionElegida(latitud: Double, longitud: Double, nombre: String) = _uiState.update {
        val nuevoLugar = nombre.trim().ifBlank { it.lugar }
        it.copy(
            latitud = latitud, longitud = longitud, lugar = nuevoLugar, avisoUbicacion = null,
            errores = if (nuevoLugar.isBlank()) it.errores else it.errores - Campo.LUGAR
        )
    }

    fun quitarUbicacion() = _uiState.update { it.copy(latitud = null, longitud = null) }

    /** Botón "Mi ubicación" de la tarjeta (la pantalla ya pidió el permiso). */
    fun usarMiUbicacion() {
        if (_uiState.value.buscandoUbicacion) return
        _uiState.update { it.copy(buscandoUbicacion = true, avisoUbicacion = null) }
        viewModelScope.launch {
            val u = runCatching { UbicacionHelper.obtenerUbicacionActual(appContext, 15.seconds) }.getOrNull()
            _uiState.update {
                if (u != null) it.copy(latitud = u.latitud, longitud = u.longitud, buscandoUbicacion = false)
                else it.copy(
                    buscandoUbicacion = false,
                    avisoUbicacion = if (!UbicacionHelper.ubicacionActivada(appContext))
                        textoUi(R.string.nueva_aviso_activar_ubicacion)
                    else textoUi(R.string.nueva_aviso_sin_ubicacion)
                )
            }
            // Si el campo Lugar está vacío, se completa con el nombre encontrado
            if (u != null && _uiState.value.lugar.isBlank()) {
                Geocodificador.nombrePara(appContext, u.latitud, u.longitud)?.let { nombre ->
                    _uiState.update { s ->
                        if (s.lugar.isBlank()) s.copy(lugar = nombre, errores = s.errores - Campo.LUGAR) else s
                    }
                }
            }
        }
    }

    fun permisoUbicacionDenegado() = _uiState.update {
        it.copy(avisoUbicacion = textoUi(R.string.nueva_aviso_sin_permiso))
    }
    fun onProfundidadChange(v: String) = _uiState.update { it.copy(profundidadMetros = v) }
    fun onParadaChange(v: String) = _uiState.update { it.copy(paradaSeguridadMin = v.filter { c -> c.isDigit() || c == '.'}) }
    fun onTiempoFondoChange(v: String) = _uiState.update { it.copy(tiempoFondo = v.filter { c -> c.isDigit() }.take(4)) }
    fun onHoraEntradaChange(v: String) = _uiState.update { it.copy(horaEntrada = v) }
    fun onHoraSalidaChange(v: String) = _uiState.update { it.copy(horaSalida = v) }
    fun onUsoLastreChange(v: Boolean) = _uiState.update {
        it.copy(usoLastre = v, lastreKg = if (v) it.lastreKg else "", errores = it.errores - Campo.LASTRE)
    }
    fun onUsoProteccionChange(v: Boolean) = _uiState.update {
        it.copy(usoProteccion = v, proteccionMm = if (v) it.proteccionMm else "", errores = it.errores - Campo.PROTECCION)
    }
    fun onLastreChange(v: String) = _uiState.update { it.copy(lastreKg = v.filter { c -> c.isDigit() }) }
    fun onProteccionMmChange(v: String) = _uiState.update { it.copy(proteccionMm = v.filter { c -> c.isDigit() }) }

    // ---- Apnea ----
    /** Agrega bajadas vacías hasta tener [n]; nunca recorta (lo que sobra queda guardado, oculto). */
    private fun ajustarBajadas(lista: List<BajadaForm>, n: Int): List<BajadaForm> =
        if (n <= lista.size) lista else lista + List(n - lista.size) { BajadaForm() }

    fun onCantidadBajadasChange(v: String) = _uiState.update {
        val cantidad = v.filter { c -> c.isDigit() }.take(2)
        val n = cantidad.toIntOrNull()
        it.copy(
            cantidadBajadas = cantidad,
            // Con el detalle activo la lista acompaña a la cantidad (si está vacía no se toca)
            bajadas = if (it.cargarDetalleBajadas && n != null && n >= 1) ajustarBajadas(it.bajadas, n) else it.bajadas,
            errores = it.errores - Campo.CANTIDAD_BAJADAS
        )
    }

    /**
     * Activar el detalle pre-genera una fila por cada bajada de la cantidad ya ingresada.
     * Al desactivarlo, la profundidad máxima queda con la de la bajada más profunda cargada.
     */
    fun onCargarDetalleBajadasChange(v: Boolean) = _uiState.update {
        val cantidad = it.cantidadBajadas.toIntOrNull()?.coerceAtLeast(1) ?: it.bajadas.size.coerceAtLeast(1)
        val maxima = it.bajadasActivas.mapNotNull { b -> b.profundidad.toFloatOrNull() }.maxOrNull()
        it.copy(
            cargarDetalleBajadas = v,
            cantidadBajadas = if (v) cantidad.toString() else it.cantidadBajadas,
            bajadas = if (v) ajustarBajadas(it.bajadas, cantidad) else it.bajadas,
            profundidadMetros = if (!v && maxima != null && maxima > 0f) numeroEditable(maxima) else it.profundidadMetros,
            errores = it.errores - Campo.BAJADAS - Campo.CANTIDAD_BAJADAS - Campo.PROFUNDIDAD
        )
    }
    fun onAgregarBajada() = _uiState.update {
        val nuevas = it.bajadasActivas + BajadaForm()
        it.copy(bajadas = nuevas, cantidadBajadas = nuevas.size.toString(), errores = it.errores - Campo.CANTIDAD_BAJADAS)
    }
    fun onQuitarBajada(indice: Int) = _uiState.update {
        val nuevas = it.bajadas.filterIndexed { i, _ -> i != indice }
        it.copy(bajadas = nuevas, cantidadBajadas = (it.bajadasActivas.size - 1).toString())
    }
    fun onBajadaProfundidadChange(indice: Int, v: String) = actualizarBajada(indice) {
        it.copy(profundidad = v.replace(',', '.').filter { c -> c.isDigit() || c == '.' }.take(5))
    }
    fun onBajadaTiempoChange(indice: Int, v: String) = actualizarBajada(indice) {
        it.copy(tiempo = v.filter { c -> c.isDigit() }.take(4))
    }
    private fun actualizarBajada(indice: Int, cambio: (BajadaForm) -> BajadaForm) = _uiState.update {
        it.copy(
            bajadas = it.bajadas.mapIndexed { i, b -> if (i == indice) cambio(b) else b },
            errores = it.errores - Campo.BAJADAS
        )
    }
    fun onSincopeChange(v: Boolean) = _uiState.update { it.copy(sincope = v) }
    fun onVisibilidadElegida(v: Visibilidad?) = _uiState.update { it.copy(visibilidad = v) }

    // Normaliza lo que escribe el usuario:
    // - acepta "," o "." como decimal (el teclado en español suele mostrar ",")
    // - un solo "-" y solo al principio (por si el teclado sí lo tiene, o se pega texto)
    // - un solo separador decimal y como mucho 6 caracteres ("-12.55")
    private fun filtrarTemperatura(v: String): String {
        val normalizado = v.replace(',', '.')
        val signo = if (normalizado.startsWith("-")) "-" else ""
        val cuerpo = normalizado.filter { it.isDigit() || it == '.' }
        val punto = cuerpo.indexOf('.')
        val numero = if (punto == -1) cuerpo
        else cuerpo.substring(0, punto + 1) + cuerpo.substring(punto + 1).replace(".", "")
        return (signo + numero).take(6)
    }

    // Pone o saca el "-" adelante, sin importar qué muestre el teclado
    private fun alternarSigno(v: String): String = if (v.startsWith("-")) v.drop(1) else "-$v"

    fun onTempAireChange(v: String) = _uiState.update { it.copy(temperaturaAireC = filtrarTemperatura(v)) }
    fun onTempAguaChange(v: String) = _uiState.update { it.copy(temperaturaAguaC = filtrarTemperatura(v)) }
    fun onAlternarSignoAire() = _uiState.update { it.copy(temperaturaAireC = alternarSigno(it.temperaturaAireC)) }
    fun onAlternarSignoAgua() = _uiState.update { it.copy(temperaturaAguaC = alternarSigno(it.temperaturaAguaC)) }

    fun onVisibilidadChange(v: String) = _uiState.update { it.copy(visibilidadMetros = v.filter { c -> c.isDigit() }) }
    fun onActividadChange(v: String) = _uiState.update { it.copy(actividad = v) }
    fun onComentariosChange(v: String) = _uiState.update { it.copy(comentarios = v) }

    fun onAguaToggle(opcion: String, marcado: Boolean) = _uiState.update {
        it.copy(
            caracteristicasAgua = if (marcado) it.caracteristicasAgua + opcion
            else it.caracteristicasAgua - opcion
        )
    }

    /** Cambiar a Apnea borra el error de parada (en apnea no se pide) y recalcula el equipo. */
    fun onTipoChange(tipo: TipoInmersion) {
        _uiState.update { it.copy(tipo = tipo, errores = it.errores - Campo.PARADA) }
        preseleccionarEquipo()
    }

    /**
     * Grupos de una sola opción (Dulce/Salada, Orilla/Bote): saca todas las opciones del
     * [grupo] y pone la elegida. [opcion] null = ninguna.
     */
    fun onAguaUnica(grupo: List<String>, opcion: String?) = _uiState.update {
        it.copy(caracteristicasAgua = it.caracteristicasAgua - grupo.toSet() + listOfNotNull(opcion))
    }

    fun onEquipamientoToggle(etiqueta: String, marcado: Boolean) {
        equipoTocado = true
        _uiState.update {
            it.copy(equipamiento = if (marcado) it.equipamiento + etiqueta else it.equipamiento - etiqueta)
        }
    }

    fun guardar() {
        val s = _uiState.value
        val errores = validar(s)
        if (errores.isNotEmpty()) {
            val primera = Etapa.entries.first { e -> errores.keys.any { it in e.campos } }
            _uiState.update { it.copy(errores = errores, etapa = primera) }
            return
        }
        if (s.guardando) return
        _uiState.update { it.copy(guardando = true, errores = emptyMap(), errorGuardado = null) }

        viewModelScope.launch {
            val fecha = LocalDate.parse(s.fecha, FORMATO_FECHA)
            val horaEntrada = LocalTime.parse(s.horaEntrada)
            val anterior = original

            // Editando sin cambiar fecha, hora de entrada ni lugar marcado: se conservan las
            // condiciones que ya estaban (no tiene sentido volver a pedirlas a internet).
            val mismoMomentoYLugar = anterior != null &&
                anterior.fecha == fecha && anterior.horaEntrada == horaEntrada &&
                anterior.latitud == s.latitud && anterior.longitud == s.longitud

            val condiciones = if (mismoMomentoYLugar) anterior.condiciones else {
                // Condiciones del LUGAR marcado. Sin marcar: ubicación del teléfono → Comodoro.
                // Sin internet guarda solo la luna.
                val ubicacion = if (s.latitud != null && s.longitud != null) Ubicacion(s.latitud, s.longitud)
                else UbicacionHelper.obtenerUltimaUbicacion(appContext) ?: UBICACION_COMODORO_RIVADAVIA
                runCatching {
                    CondicionesRepository.registroPara(ubicacion.latitud, ubicacion.longitud, fecha.atTime(horaEntrada))
                }.getOrNull() ?: anterior?.condiciones
            }
            val esApnea = s.tipo == TipoInmersion.APNEA
            val registroApnea = if (esApnea) s.registroApnea else null
            val entidad = InmersionEntity(
                    fecha = fecha,
                    lugar = s.lugar,
                    // Apnea: la máxima del registro; buceo: lo escrito (ya validado)
                    profundidadMetros = registroApnea?.profundidadMaximaMetros ?: s.profundidadMetros.toFloat(),
                    paradaSeguridadMin =
                        if (s.tipo == TipoInmersion.APNEA) 0f
                        else s.paradaSeguridadMin.toFloatOrNull() ?: 0f,
                    // Apnea: suma de las bajadas cargadas (0 si no se cargó el detalle)
                    tiempoFondo = if (esApnea) (registroApnea?.resumen()?.tiempoTotalSegundos ?: 0).seconds
                    else parseTiempoFondo(s.tiempoFondo), // "mm:ss" -> Duration
                    horaEntrada = horaEntrada,                      // "HH:mm" -> LocalTime
                    horaSalida = LocalTime.parse(s.horaSalida),
                    caracteristicasAgua = s.caracteristicasAgua,
                    lastreKg = if (s.usoLastre) s.lastreKg.toFloatOrNull() else null,
                    proteccionMm = if (s.usoProteccion) s.proteccionMm.toFloatOrNull() else null,
                    equipamiento = s.equipamiento,
                    temperaturaAireC = s.temperaturaAireC.toFloatOrNull() ?: 0f,
                    temperaturaAguaC = s.temperaturaAguaC.toFloatOrNull() ?: 0f,
                    visibilidad = s.visibilidad,
                    visibilidadMetros = s.visibilidadMetros.toFloatOrNull(),
                    actividad = s.actividad,
                    comentarios = s.comentarios,
                    condiciones = condiciones,
                    tipo = s.tipo,
                    latitud = s.latitud,   // solo lo marcado; null si no se marcó nada
                    longitud = s.longitud,
                    apnea = registroApnea
                )
            val ok = runCatching {
                if (anterior != null) repository.actualizar(entidad.copy(id = anterior.id))
                else repository.guardar(entidad)
            }.getOrDefault(false)
            _uiState.update {
                if (ok) it.copy(guardadoOk = true, guardando = false)
                else it.copy(guardando = false, errorGuardado = textoUi(R.string.nueva_error_guardar))
            }
        }
    }

    private fun validar(s: NuevaInmersionUiState): Map<Campo, TextoUi> {
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

    companion object {
        fun Factory(
            repository: InmersionRepository,
            perfilRepository: PerfilRepository,
            nombreUsuario: String?,
            appContext: Context,
            idEditar: Long? = null
        ) = viewModelFactory {
            initializer { NuevaInmersionViewModel(repository, perfilRepository, nombreUsuario, appContext, idEditar) }
        }
    }
}