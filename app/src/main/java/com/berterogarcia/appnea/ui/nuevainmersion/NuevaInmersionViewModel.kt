package com.berterogarcia.appnea.ui.nuevainmersion

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.data.condiciones.CondicionesRepository
import com.berterogarcia.appnea.data.local.CategoriaEquipo
import com.berterogarcia.appnea.data.local.EquipoEntity
import com.berterogarcia.appnea.data.local.InmersionEntity
import com.berterogarcia.appnea.data.local.TipoInmersion
import com.berterogarcia.appnea.data.local.UBICACION_COMODORO_RIVADAVIA
import com.berterogarcia.appnea.data.local.UbicacionHelper
import com.berterogarcia.appnea.data.local.descripcion
import com.berterogarcia.appnea.data.repository.InmersionRepository
import com.berterogarcia.appnea.data.repository.PerfilRepository
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class Campo { FECHA, LUGAR, PROFUNDIDAD, PARADA, TIEMPO_FONDO, HORA_ENTRADA, HORA_SALIDA, LASTRE, PROTECCION, TEMP_AIRE, TEMP_AGUA, VISIBILIDAD }

enum class Etapa(val titulo: String, val campos: Set<Campo>) {
    GENERAL("Datos generales", setOf(Campo.FECHA, Campo.LUGAR, Campo.HORA_ENTRADA, Campo.HORA_SALIDA)),
    INMERSION("Inmersión", setOf(Campo.PROFUNDIDAD, Campo.TIEMPO_FONDO, Campo.PARADA)),
    CONDICIONES("Condiciones", setOf(Campo.TEMP_AIRE, Campo.TEMP_AGUA, Campo.VISIBILIDAD)),
    EQUIPO("Equipo", setOf(Campo.LASTRE, Campo.PROTECCION)),
    NOTAS("Notas", emptySet())
}

private val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.US)
private val REGEX_TIEMPO_FONDO = Regex("^\\d{2}[0-5]\\d\$") // 4 dígitos, segundos 00-59

/** Categorías que solo tienen sentido en buceo con tanque: en Apnea no se ofrecen. */
private val SOLO_BUCEO = setOf(CategoriaEquipo.REGULADOR, CategoriaEquipo.CHALECO, CategoriaEquipo.TUBO)

private fun parseTiempoFondo(v: String): Duration {
    val digitos = v.padStart(4, '0')
    val minutos = digitos.substring(0, 2).toInt()
    val segundos = digitos.substring(2, 4).toInt()
    return (minutos * 60 + segundos).seconds
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
    val profundidadMetros: String = "",
    val paradaSeguridadMin: String = "",
    val tiempoFondo: String = "",
    val horaEntrada: String = "",
    val horaSalida: String = "",
    val caracteristicasAgua: Set<String> = emptySet(),
    val lastreKg: String = "",
    val proteccionMm: String = "",
    val equipamiento: Set<String> = emptySet(),          // etiquetas (etiquetaInmersion) marcadas
    val equipoPerfil: List<EquipoEntity> = emptyList(),  // equipo EN USO del usuario
    val equipoCargado: Boolean = false,                  // false mientras se lee de la base
    val temperaturaAireC: String = "",
    val temperaturaAguaC: String = "",
    val visibilidadMetros: String = "",
    val actividad: String = "",
    val comentarios: String = "",
    val guardadoOk: Boolean = false,
    val guardando: Boolean = false,
    val errores: Map<Campo, String> = emptyMap(),
) {
    /** Equipo que se ofrece según el tipo (en Apnea sin regulador, chaleco ni tubo). */
    val equipoVisible: List<EquipoEntity>
        get() = equipoPerfil.filter { tipo == TipoInmersion.BUCEO || it.categoria !in SOLO_BUCEO }
}

class NuevaInmersionViewModel(
    private val repository: InmersionRepository,
    perfilRepository: PerfilRepository,
    nombreUsuario: String?,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        NuevaInmersionUiState(fecha = LocalDate.now().format(FORMATO_FECHA)))
    val uiState: StateFlow<NuevaInmersionUiState> = _uiState

    /** Inmersiones anteriores, para pre-marcar el equipo de la última del mismo tipo. */
    private var inmersionesPrevias: List<InmersionEntity> = emptyList()

    /** true cuando el usuario tocó algún equipo: desde ahí no se pre-marca nada solo. */
    private var equipoTocado = false

    init {
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
    fun onProfundidadChange(v: String) = _uiState.update { it.copy(profundidadMetros = v) }
    fun onParadaChange(v: String) = _uiState.update { it.copy(paradaSeguridadMin = v.filter { c -> c.isDigit() || c == '.'}) }
    fun onTiempoFondoChange(v: String) = _uiState.update { it.copy(tiempoFondo = v.filter { c -> c.isDigit() }.take(4)) }
    fun onHoraEntradaChange(v: String) = _uiState.update { it.copy(horaEntrada = v) }
    fun onHoraSalidaChange(v: String) = _uiState.update { it.copy(horaSalida = v) }
    fun onLastreChange(v: String) = _uiState.update { it.copy(lastreKg = v.filter { c -> c.isDigit() }) }
    fun onProteccionMmChange(v: String) = _uiState.update { it.copy(proteccionMm = v.filter { c -> c.isDigit() }) }

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
        _uiState.update { it.copy(guardando = true, errores = emptyMap()) }

        viewModelScope.launch {
            val fecha = LocalDate.parse(s.fecha, FORMATO_FECHA)
            val horaEntrada = LocalTime.parse(s.horaEntrada)
            // Ubicación del teléfono si hay permiso; si no, Comodoro. Sin internet guarda solo la luna.
            // (En la parte C se va a usar el lugar marcado en el mapa.)
            val ubicacion = UbicacionHelper.obtenerUltimaUbicacion(appContext) ?: UBICACION_COMODORO_RIVADAVIA
            val condiciones = runCatching {
                CondicionesRepository.registroPara(ubicacion.latitud, ubicacion.longitud, fecha.atTime(horaEntrada))
            }.getOrNull()
            repository.guardar(
                InmersionEntity(
                    fecha = fecha,
                    lugar = s.lugar,
                    profundidadMetros = s.profundidadMetros.toFloat(), // ya validado, no hace falta toFloatOrNull
                    paradaSeguridadMin =
                        if (s.tipo == TipoInmersion.APNEA) 0f
                        else s.paradaSeguridadMin.toFloatOrNull() ?: 0f,
                    tiempoFondo = parseTiempoFondo(s.tiempoFondo), // "mm:ss" -> Duration
                    horaEntrada = horaEntrada,                      // "HH:mm" -> LocalTime
                    horaSalida = LocalTime.parse(s.horaSalida),
                    caracteristicasAgua = s.caracteristicasAgua,
                    lastreKg = s.lastreKg.toFloatOrNull() ?: 0f,
                    proteccionMm = s.proteccionMm.toFloatOrNull() ?: 0f,
                    equipamiento = s.equipamiento,
                    temperaturaAireC = s.temperaturaAireC.toFloatOrNull() ?: 0f,
                    temperaturaAguaC = s.temperaturaAguaC.toFloatOrNull() ?: 0f,
                    visibilidadMetros = s.visibilidadMetros.toFloatOrNull() ?: 0f,
                    actividad = s.actividad,
                    comentarios = s.comentarios,
                    condiciones = condiciones,
                    tipo = s.tipo
                )
            )
            _uiState.update { it.copy(guardadoOk = true, guardando = false) }
        }
    }

    private fun validar(s: NuevaInmersionUiState): Map<Campo, String> {
        val errores = mutableMapOf<Campo, String>()
        if (s.fecha.isBlank()) errores[Campo.FECHA] = "Ingresá una fecha."
        if (s.lugar.isBlank()) errores[Campo.LUGAR] = "Ingresá un lugar."
        if (s.profundidadMetros.toFloatOrNull() == null) errores[Campo.PROFUNDIDAD] = "Profundidad inválida."

        // La parada de seguridad solo existe en buceo con tanque
        if (s.tipo == TipoInmersion.BUCEO) {
            val parada = s.paradaSeguridadMin.toFloatOrNull()
            if (parada == null || parada < 3f) errores[Campo.PARADA] = "Mínimo 3 minutos."
        }

        if (!REGEX_TIEMPO_FONDO.matches(s.tiempoFondo)) errores[Campo.TIEMPO_FONDO] = "Formato esperado mm:ss."

        val horaEntrada = s.horaEntrada.takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        val horaSalida = s.horaSalida.takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

        if (horaEntrada == null) errores[Campo.HORA_ENTRADA] = "Elegí la hora de entrada"
        if (horaSalida == null) errores[Campo.HORA_SALIDA] = "Elegí la hora de salida"
        if (horaEntrada != null && horaSalida != null && horaEntrada.isAfter(horaSalida)) {
            errores[Campo.HORA_SALIDA] = "Debe ser posterior a la hora de entrada"
        }

        val lastre = s.lastreKg.toFloatOrNull()
        if (lastre == null || lastre < 0f) errores[Campo.LASTRE] = "Lastre minimo 0kg."

        val proteccion = s.proteccionMm.toFloatOrNull()
        if (proteccion == null || proteccion < 0f) errores[Campo.PROTECCION] = "Proteccion minima 0mm."

        val temperaturaAire = s.temperaturaAireC.toFloatOrNull()
        if (temperaturaAire == null || temperaturaAire < -20f) errores[Campo.TEMP_AIRE] = "Temperatura mínima -20°C."
        else if (temperaturaAire > 45f) errores[Campo.TEMP_AIRE] = "Temperatura máxima 45°C."

        val temperaturaAgua = s.temperaturaAguaC.toFloatOrNull()
        if (temperaturaAgua == null || temperaturaAgua < -2f) errores[Campo.TEMP_AGUA] = "Temperatura mínima -2°C."
        else if (temperaturaAgua > 40f) errores[Campo.TEMP_AGUA] = "Temperatura máxima 40°C."

        val visibilidad = s.visibilidadMetros.toFloatOrNull()
        if (visibilidad == null || visibilidad < 0f) errores[Campo.VISIBILIDAD] = "Visibilidad minima 0m."
        return errores
    }

    companion object {
        fun Factory(
            repository: InmersionRepository,
            perfilRepository: PerfilRepository,
            nombreUsuario: String?,
            appContext: Context
        ) = viewModelFactory {
            initializer { NuevaInmersionViewModel(repository, perfilRepository, nombreUsuario, appContext) }
        }
    }
}