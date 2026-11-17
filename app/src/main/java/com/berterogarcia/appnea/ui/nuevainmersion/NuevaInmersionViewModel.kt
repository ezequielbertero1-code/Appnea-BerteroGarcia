package com.berterogarcia.appnea.ui.nuevainmersion

import com.berterogarcia.appnea.ui.components.nombre
import java.time.Clock
import com.berterogarcia.appnea.util.intentar
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.dominio.RegistrarInmersion
import com.berterogarcia.appnea.dominio.ServicioUbicacion
import com.berterogarcia.appnea.dominio.modelo.BajadaApnea
import com.berterogarcia.appnea.dominio.modelo.CategoriaEquipo
import com.berterogarcia.appnea.dominio.modelo.Equipo
import com.berterogarcia.appnea.dominio.modelo.Inmersion
import com.berterogarcia.appnea.dominio.modelo.RegistroApnea
import com.berterogarcia.appnea.dominio.modelo.ResumenApnea
import com.berterogarcia.appnea.dominio.modelo.TipoInmersion
import com.berterogarcia.appnea.dominio.modelo.UBICACION_COMODORO_RIVADAVIA
import com.berterogarcia.appnea.dominio.modelo.Ubicacion
import com.berterogarcia.appnea.dominio.modelo.Visibilidad
import com.berterogarcia.appnea.dominio.repositorios.RepositorioInmersiones
import com.berterogarcia.appnea.dominio.repositorios.RepositorioPerfil
import com.berterogarcia.appnea.ui.components.TextoUi
import com.berterogarcia.appnea.ui.components.textoUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
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

internal val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.US)
internal val FORMATO_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
private val REGEX_TIEMPO_FONDO = Regex("^\\d{2}[0-5]\\d\$") // 4 dígitos, segundos 00-59

/** El campo se llena desde el final ("500" = 05:00), así que se valida el texto completado a 4 dígitos. */
internal fun tiempoValido(v: String): Boolean = v.isNotEmpty() && REGEX_TIEMPO_FONDO.matches(v.padStart(4, '0'))

/** Categorías que solo tienen sentido en buceo con tanque: en Apnea no se ofrecen. */
private val SOLO_BUCEO = setOf(CategoriaEquipo.REGULADOR, CategoriaEquipo.CHALECO, CategoriaEquipo.TUBO)

internal fun parseTiempoFondo(v: String): Duration {
    val digitos = v.padStart(4, '0')
    val minutos = digitos.substring(0, 2).toInt()
    val segundos = digitos.substring(2, 4).toInt()
    return (minutos * 60 + segundos).seconds
}

// ---- De lo guardado al texto de los campos (modo edición) ----

/** 18.0 → "18", 18.5 → "18.5" */
internal fun numeroEditable(v: Float): String = if (v % 1f == 0f) v.toInt().toString() else v.toString()

/** Campos que solo aceptan dígitos (lastre, protección, visibilidad). */
internal fun enteroEditable(v: Float?): String = v?.roundToInt()?.coerceAtLeast(0)?.toString() ?: ""

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

internal fun BajadaApnea.aForm() = BajadaForm(numeroEditable(profundidadMetros), tiempoFondoEditable(tiempoSegundos.seconds))

/** 41 min 05 s → "4105" (el campo guarda solo dígitos y muestra mm:ss). */
internal fun tiempoFondoEditable(d: Duration): String {
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
val Equipo.etiquetaInmersion: String
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
    val equipoPerfil: List<Equipo> = emptyList(),  // equipo EN USO del usuario
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
    val equipoVisible: List<Equipo>
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
    private val repository: RepositorioInmersiones,
    private val registrar: RegistrarInmersion,
    perfilRepository: RepositorioPerfil,
    /** GPS y geocoder; también lo usa el selector de lugar en el mapa. */
    val ubicacionTelefono: ServicioUbicacion,
    /** null = alta de una inmersión nueva; con id = edición de esa inmersión. */
    private val idEditar: Long? = null,
    /** Guarda el borrador del formulario por si Android mata la app en segundo plano. */
    private val estadoGuardado: SavedStateHandle = SavedStateHandle(),
    private val reloj: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        NuevaInmersionUiState(
            fecha = LocalDate.now(reloj).format(FORMATO_FECHA),
            editando = idEditar != null,
            cargandoEdicion = idEditar != null
        )
    )
    val uiState: StateFlow<NuevaInmersionUiState> = _uiState

    /** Inmersiones anteriores, para pre-marcar el equipo de la última del mismo tipo. */
    private var inmersionesPrevias: List<Inmersion> = emptyList()

    /** true cuando el usuario tocó algún equipo: desde ahí no se pre-marca nada solo. */
    private var equipoTocado = false

    /** La inmersión tal como estaba guardada (solo en modo edición). */
    private var original: Inmersion? = null

    init {
        val borrador = estadoGuardado.get<String>(CLAVE_BORRADOR)
        if (borrador != null) {
            // Volviendo después de que Android mató la app: se recupera lo escrito
            _uiState.update { it.conBorrador(borrador) }
            equipoTocado = true
        }
        if (idEditar != null) cargarParaEditar(idEditar, soloOriginal = borrador != null)
        viewModelScope.launch {
            _uiState
                .filter { !it.cargandoEdicion && !it.guardadoOk }
                .map { it.aBorradorJson() }
                .distinctUntilChanged()
                .collect { estadoGuardado[CLAVE_BORRADOR] = it }
        }
        viewModelScope.launch {
            inmersionesPrevias = intentar("NuevaInmersionViewModel") { repository.todasLasInmersiones.first() }
                .getOrDefault(emptyList())
            preseleccionarEquipo()
        }
        // Equipo del usuario logueado (sin sesión: lista vacía, no ruedita eterna)
        viewModelScope.launch {
            perfilRepository.equipamientoActual
                .catch { emit(emptyList()) } // si falla la base: lista vacía en vez de crashear
                .collect { lista ->
                    _uiState.update {
                        it.copy(equipoPerfil = lista.filter { e -> e.enUso }, equipoCargado = true)
                    }
                    preseleccionarEquipo()
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
                    .maxWithOrNull(compareBy<Inmersion> { it.fecha }.thenBy { it.horaEntrada })
                    ?.equipamiento
                    .orEmpty()
            }
            s.copy(equipamiento = base intersect visibles)
        }
    }

    /**
     * Lee la inmersión y completa el formulario con sus datos.
     * [soloOriginal]: hay un borrador recuperado, así que solo se lee la original (para guardar
     * después) sin pisar lo que el usuario ya había cambiado.
     */
    private fun cargarParaEditar(id: Long, soloOriginal: Boolean = false) {
        viewModelScope.launch {
            val i = intentar("NuevaInmersionViewModel") { repository.obtenerPorId(id).first() }.getOrNull()
            if (i == null) {
                _uiState.update {
                    it.copy(cargandoEdicion = false, errorCarga = textoUi(R.string.nueva_error_no_encontrada))
                }
                return@launch
            }
            original = i
            equipoTocado = true
            if (soloOriginal) {
                _uiState.update { it.copy(cargandoEdicion = false) }
                return@launch
            }
            _uiState.update { it.conInmersion(i) }
        }
    }

    /** Único punto de entrada de la pantalla. Los cambios de estado están en [reducir]. */
    fun onEvento(e: EventoFormulario) {
        when (e) {
            EventoFormulario.Guardar -> guardar()
            EventoFormulario.UsarMiUbicacion -> usarMiUbicacion()
            else -> {
                // Desde que el usuario toca algún equipo no se pre-marca nada solo
                if (e is EventoFormulario.EquipoMarcado) equipoTocado = true
                _uiState.update { reducir(it, e) }
                // Al cambiar el tipo se recalcula el equipo (ej. el regulador no va en apnea)
                if (e is EventoFormulario.Tipo) preseleccionarEquipo()
            }
        }
    }

    /** Centro con el que abre el mapa: lo ya marcado → última ubicación del teléfono → Comodoro. */
    fun centroInicialMapa(): Ubicacion {
        val s = _uiState.value
        return if (s.latitud != null && s.longitud != null) Ubicacion(s.latitud, s.longitud)
        else ubicacionTelefono.ultima() ?: UBICACION_COMODORO_RIVADAVIA
    }

    /** Botón "Mi ubicación" de la tarjeta (la pantalla ya pidió el permiso). */
    private fun usarMiUbicacion() {
        if (_uiState.value.buscandoUbicacion) return
        _uiState.update { it.copy(buscandoUbicacion = true, avisoUbicacion = null) }
        viewModelScope.launch {
            val u = intentar("NuevaInmersionViewModel") { ubicacionTelefono.actual(15.seconds) }.getOrNull()
            _uiState.update {
                if (u != null) it.copy(latitud = u.latitud, longitud = u.longitud, buscandoUbicacion = false)
                else it.copy(
                    buscandoUbicacion = false,
                    avisoUbicacion = if (!ubicacionTelefono.ubicacionActivada())
                        textoUi(R.string.nueva_aviso_activar_ubicacion)
                    else textoUi(R.string.nueva_aviso_sin_ubicacion)
                )
            }
            // Si el campo Lugar está vacío, se completa con el nombre encontrado
            if (u != null && _uiState.value.lugar.isBlank()) {
                ubicacionTelefono.nombreDelLugar(u.latitud, u.longitud)?.let { nombre ->
                    _uiState.update { s ->
                        if (s.lugar.isBlank()) s.copy(lugar = nombre, errores = s.errores - Campo.LUGAR) else s
                    }
                }
            }
        }
    }

    private fun guardar() {
        val s = _uiState.value
        val errores = validarInmersion(s)
        if (errores.isNotEmpty()) {
            val primera = Etapa.entries.first { e -> errores.keys.any { it in e.campos } }
            _uiState.update { it.copy(errores = errores, etapa = primera) }
            return
        }
        if (s.guardando) return
        _uiState.update { it.copy(guardando = true, errores = emptyMap(), errorGuardado = null) }

        viewModelScope.launch {
            val anterior = original
            // Condiciones del LUGAR marcado. Sin marcar: ubicación del teléfono → Comodoro.
            val ubicacion = if (s.latitud != null && s.longitud != null) Ubicacion(s.latitud, s.longitud)
            else ubicacionTelefono.ultima() ?: UBICACION_COMODORO_RIVADAVIA
            val inmersion = s.aInmersion()
            // Guarda al instante; clima y mar se completan en segundo plano (RegistrarInmersion)
            val ok = intentar("NuevaInmersionViewModel") { registrar(inmersion, anterior, ubicacion) }.getOrDefault(false)
            _uiState.update {
                if (ok) it.copy(guardadoOk = true, guardando = false)
                else it.copy(guardando = false, errorGuardado = textoUi(R.string.nueva_error_guardar))
            }
        }
    }

    companion object {

        private const val CLAVE_BORRADOR = "borrador_inmersion"
    }
}