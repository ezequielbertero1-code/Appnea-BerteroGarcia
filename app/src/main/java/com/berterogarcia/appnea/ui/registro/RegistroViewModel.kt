package com.berterogarcia.appnea.ui.registro

import com.berterogarcia.appnea.util.intentar
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.dominio.modelo.CuentaGoogle
import com.berterogarcia.appnea.dominio.modelo.ContactoEmergencia
import com.berterogarcia.appnea.dominio.modelo.PerfilUsuario
import com.berterogarcia.appnea.dominio.GestorSesion
import com.berterogarcia.appnea.dominio.repositorios.RepositorioCuentaGoogle
import com.berterogarcia.appnea.dominio.repositorios.NuevoRegistro
import com.berterogarcia.appnea.dominio.repositorios.RepositorioRegistro
import com.berterogarcia.appnea.dominio.repositorios.ResultadoRegistro
import com.berterogarcia.appnea.ui.components.TextoUi
import com.berterogarcia.appnea.ui.components.textoUi
import com.berterogarcia.appnea.ui.perfil.comun.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Pasos del registro. El nombre que se muestra sale de strings.xml (ver Registro.kt). */
enum class PasoRegistro { CUENTA, PERSONALES, EMERGENCIA, CONFIRMAR }

/** Cada campo sabe en qué paso está: si hay un error, se vuelve a ese paso. */
enum class CampoRegistro(val paso: PasoRegistro) {
    USUARIO(PasoRegistro.CUENTA),
    PASSWORD(PasoRegistro.CUENTA),
    CONFIRMACION(PasoRegistro.CUENTA),
    NOMBRE(PasoRegistro.PERSONALES),
    APELLIDO(PasoRegistro.PERSONALES),
    EMAIL(PasoRegistro.PERSONALES),
    TELEFONO(PasoRegistro.PERSONALES),
    NACIMIENTO(PasoRegistro.PERSONALES),
    CIUDAD(PasoRegistro.PERSONALES),
    CONTACTO_NOMBRE(PasoRegistro.EMERGENCIA),
    CONTACTO_RELACION(PasoRegistro.EMERGENCIA),
    CONTACTO_TELEFONO(PasoRegistro.EMERGENCIA),
    ACEPTA_AVISO(PasoRegistro.CONFIRMAR)
}

data class RegistroUiState(
    /** Pasos de este registro: con Google no hay CUENTA (no hay usuario ni contraseña que elegir). */
    val pasos: List<PasoRegistro> = PasoRegistro.entries,
    val paso: PasoRegistro = PasoRegistro.CUENTA,
    /** Email de la cuenta de Google con la que se está registrando, o null en el registro normal. */
    val emailGoogle: String? = null,
    val usuario: String = "",
    val password: String = "",
    val confirmacion: String = "",
    val nombre: String = "",
    val apellido: String = "",
    val email: String = "",
    val telefono: String = "",
    val fechaNacimiento: LocalDate? = null,
    val ciudad: String = "",
    val contactoNombre: String = "",
    val contactoRelacion: String = "",
    val contactoTelefono: String = "",
    val aceptaAviso: Boolean = false,
    val errores: Map<CampoRegistro, TextoUi> = emptyMap(),
    val verificandoUsuario: Boolean = false,
    val creando: Boolean = false,
    val registroOk: Boolean = false,
    val errorGeneral: TextoUi? = null
) {
    val fuerza: FuerzaPassword get() = ReglasRegistro.fuerza(password)
    val ocupado: Boolean get() = verificandoUsuario || creando
    val indicePaso: Int get() = pasos.indexOf(paso)
    val esPrimerPaso: Boolean get() = paso == pasos.first()
    val esUltimoPaso: Boolean get() = paso == pasos.last()
    val esGoogle: Boolean get() = emailGoogle != null
    val pasosConError: Set<PasoRegistro> get() = errores.keys.map { it.paso }.toSet()
}

/**
 * Registro por pasos. Al terminar crea usuario + perfil + contacto e inicia sesión.
 * Con [cuentaGoogle] (primer ingreso con Google) se saltea el paso de cuenta y se parte de los datos de Google.
 */
class RegistroViewModel(
    private val repository: RepositorioRegistro,
    private val cuentaGoogleRepository: RepositorioCuentaGoogle,
    private val sessionManager: GestorSesion,
    private val cuentaGoogle: CuentaGoogle? = null,
    /** Borrador del formulario por si Android mata la app en segundo plano (sin contraseñas). */
    private val estadoGuardado: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        if (cuentaGoogle == null) RegistroUiState()
        else RegistroUiState(
            pasos = PasoRegistro.entries - PasoRegistro.CUENTA,
            paso = PasoRegistro.PERSONALES,
            emailGoogle = cuentaGoogle.email,
            usuario = cuentaGoogle.email,
            nombre = cuentaGoogle.nombre,
            apellido = cuentaGoogle.apellido,
            email = cuentaGoogle.email
        )
    )
    val uiState: StateFlow<RegistroUiState> = _uiState.asStateFlow()

    init {
        estadoGuardado.get<String>(CLAVE_BORRADOR)?.let { json -> _uiState.update { it.conBorrador(json) } }
        viewModelScope.launch {
            _uiState.filter { !it.registroOk }.map { it.aBorradorJson() }.distinctUntilChanged()
                .collect { estadoGuardado[CLAVE_BORRADOR] = it }
        }
    }

    // ------------------------------- Cambios de campos -------------------------------

    fun onUsuarioChange(v: String) = actualizar(CampoRegistro.USUARIO) { copy(usuario = v.trim()) }
    fun onPasswordChange(v: String) = actualizar(CampoRegistro.PASSWORD) { copy(password = v) }
    fun onConfirmacionChange(v: String) = actualizar(CampoRegistro.CONFIRMACION) { copy(confirmacion = v) }
    fun onNombreChange(v: String) = actualizar(CampoRegistro.NOMBRE) { copy(nombre = v) }
    fun onApellidoChange(v: String) = actualizar(CampoRegistro.APELLIDO) { copy(apellido = v) }
    fun onEmailChange(v: String) = actualizar(CampoRegistro.EMAIL) { copy(email = v) }
    fun onTelefonoChange(v: String) = actualizar(CampoRegistro.TELEFONO) { copy(telefono = v) }
    fun onNacimientoChange(v: LocalDate?) = actualizar(CampoRegistro.NACIMIENTO) { copy(fechaNacimiento = v) }
    fun onCiudadChange(v: String) = actualizar(CampoRegistro.CIUDAD) { copy(ciudad = v) }
    fun onContactoNombreChange(v: String) = actualizar(CampoRegistro.CONTACTO_NOMBRE) { copy(contactoNombre = v) }
    fun onContactoRelacionChange(v: String) = actualizar(CampoRegistro.CONTACTO_RELACION) { copy(contactoRelacion = v) }
    fun onContactoTelefonoChange(v: String) = actualizar(CampoRegistro.CONTACTO_TELEFONO) { copy(contactoTelefono = v) }
    fun onAceptaAvisoChange(v: Boolean) = actualizar(CampoRegistro.ACEPTA_AVISO) { copy(aceptaAviso = v) }

    /** Aplica el cambio y limpia el error de ESE campo (y el error general). */
    private fun actualizar(campo: CampoRegistro, cambio: RegistroUiState.() -> RegistroUiState) {
        _uiState.update { it.cambio().copy(errores = it.errores - campo, errorGeneral = null) }
    }

    // ---------------------------------- Navegación ----------------------------------

    /** Valida el paso actual y avanza. En el paso 1 además verifica que el usuario esté libre. */
    fun siguiente() {
        val s = _uiState.value
        if (s.ocupado || s.esUltimoPaso) return

        val errores = validar(s).filterKeys { it.paso == s.paso }
        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(errores = it.errores + errores) }
            return
        }
        if (s.paso == PasoRegistro.CUENTA) verificarUsuarioYAvanzar(s.usuario) else avanzar()
    }

    /** @return false si ya estaba en el primer paso (la pantalla vuelve al login). */
    fun anterior(): Boolean {
        val s = _uiState.value
        if (s.esPrimerPaso || s.ocupado) return false
        _uiState.update { it.copy(paso = s.pasos[s.indicePaso - 1], errorGeneral = null) }
        return true
    }

    /** Ir a un paso anterior (indicador o "Editar" del resumen). No se puede saltar hacia adelante. */
    fun irA(paso: PasoRegistro) {
        val s = _uiState.value
        if (!s.ocupado && paso in s.pasos && s.pasos.indexOf(paso) < s.indicePaso) _uiState.update { it.copy(paso = paso) }
    }

    private fun avanzar() {
        _uiState.update { it.copy(paso = it.pasos[it.indicePaso + 1]) }
    }

    private fun verificarUsuarioYAvanzar(usuario: String) {
        _uiState.update { it.copy(verificandoUsuario = true) }
        viewModelScope.launch {
            // Si falla la consulta dejamos avanzar: al crear la cuenta se vuelve a verificar.
            val disponible = intentar("RegistroViewModel") { repository.usuarioDisponible(usuario) }.getOrDefault(true)
            _uiState.update {
                if (disponible) it.copy(verificandoUsuario = false, paso = PasoRegistro.PERSONALES)
                else it.copy(
                    verificandoUsuario = false,
                    errores = it.errores + (CampoRegistro.USUARIO to textoUi(R.string.registro_error_usuario_existe))
                )
            }
        }
    }

    // --------------------------------- Crear cuenta ---------------------------------

    fun crearCuenta() {
        val s = _uiState.value
        if (s.ocupado || s.registroOk) return

        // Se valida toodoo de nuevo y, si algo falla, se vuelve al primer paso con error.
        val errores = validar(s)
        if (errores.isNotEmpty()) {
            val primerPaso = errores.keys.minOf { s.pasos.indexOf(it.paso) }
            _uiState.update { it.copy(errores = errores, paso = s.pasos[primerPaso]) }
            return
        }

        _uiState.update { it.copy(creando = true, errorGeneral = null) }
        viewModelScope.launch {
            val resultado = intentar("RegistroViewModel") {
                if (cuentaGoogle == null) repository.registrar(armarRegistro(s))
                else {
                    val datos = armarRegistro(s)
                    cuentaGoogleRepository.registrar(cuentaGoogle.email, datos.perfil, datos.contacto)
                    ResultadoRegistro.Creado
                }
            }
            _uiState.update { actual ->
                resultado.fold(
                    onSuccess = { r ->
                        when (r) {
                            ResultadoRegistro.Creado -> {
                                sessionManager.guardarUsuario(s.usuario) // (con Google, usuario = email) entra directo, sin pasar por el login
                                actual.copy(creando = false, registroOk = true)
                            }
                            ResultadoRegistro.UsuarioExistente -> actual.copy(
                                creando = false,
                                paso = PasoRegistro.CUENTA,
                                errores = actual.errores + (CampoRegistro.USUARIO to textoUi(R.string.registro_error_usuario_existe))
                            )
                        }
                    },
                    onFailure = {
                        actual.copy(creando = false, errorGeneral = textoUi(R.string.registro_error_crear))
                    }
                )
            }
        }
    }

    private fun armarRegistro(s: RegistroUiState) = NuevoRegistro(
        usuario = s.usuario,
        password = s.password,
        perfil = PerfilUsuario(
            nombre = s.nombre.trim(),
            apellido = s.apellido.trim(),
            email = s.email.trim(),
            telefono = s.telefono.trim(),
            fechaNacimiento = s.fechaNacimiento,
            ciudad = s.ciudad.trim()
        ),
        contacto = ContactoEmergencia(
            nombre = s.contactoNombre.trim(),
            relacion = s.contactoRelacion.trim(),
            telefono = s.contactoTelefono.trim(),
            esPrincipal = true
        )
    )

    // ---------------------------------- Validación ----------------------------------

    private fun validar(s: RegistroUiState): Map<CampoRegistro, TextoUi> = buildMap {
        if (!s.esGoogle) {
            ReglasRegistro.usuario(s.usuario)?.let { put(CampoRegistro.USUARIO, it) }
            ReglasRegistro.password(s.password)?.let { put(CampoRegistro.PASSWORD, it) }
            ReglasRegistro.confirmacion(s.password, s.confirmacion)?.let { put(CampoRegistro.CONFIRMACION, it) }
        }

        (Validaciones.obligatorio(s.nombre, textoUi(R.string.campo_tu_nombre)) ?: Validaciones.largoMaximo(s.nombre, 60))
            ?.let { put(CampoRegistro.NOMBRE, it) }
        (Validaciones.obligatorio(s.apellido, textoUi(R.string.campo_tu_apellido)) ?: Validaciones.largoMaximo(s.apellido, 60))
            ?.let { put(CampoRegistro.APELLIDO, it) }
        (Validaciones.obligatorio(s.email, textoUi(R.string.campo_tu_email)) ?: Validaciones.email(s.email))
            ?.let { put(CampoRegistro.EMAIL, it) }
        Validaciones.telefono(s.telefono, obligatorio = true)?.let { put(CampoRegistro.TELEFONO, it) }
        when {
            s.fechaNacimiento == null -> put(CampoRegistro.NACIMIENTO, textoUi(R.string.registro_error_nacimiento))
            else -> Validaciones.noFutura(s.fechaNacimiento)?.let { put(CampoRegistro.NACIMIENTO, it) }
        }
        Validaciones.largoMaximo(s.ciudad, 80)?.let { put(CampoRegistro.CIUDAD, it) }

        (Validaciones.obligatorio(s.contactoNombre, textoUi(R.string.campo_nombre_contacto)) ?: Validaciones.largoMaximo(s.contactoNombre, 60))
            ?.let { put(CampoRegistro.CONTACTO_NOMBRE, it) }
        Validaciones.largoMaximo(s.contactoRelacion, 40)?.let { put(CampoRegistro.CONTACTO_RELACION, it) }
        Validaciones.telefono(s.contactoTelefono, obligatorio = true)?.let { put(CampoRegistro.CONTACTO_TELEFONO, it) }
        if (s.contactoTelefono.isNotBlank() && s.contactoTelefono.trim() == s.telefono.trim()) {
            put(CampoRegistro.CONTACTO_TELEFONO, textoUi(R.string.registro_error_contacto_propio))
        }

        if (!s.aceptaAviso) put(CampoRegistro.ACEPTA_AVISO, textoUi(R.string.registro_error_aviso))
    }

    companion object {

        private const val CLAVE_BORRADOR = "borrador_registro"
    }
}
/**
 * Campos del registro como JSON para el SavedStateHandle. La contraseña y su confirmación NO se
 * guardan: el SavedStateHandle puede terminar escrito en disco. Al volver, se piden de nuevo.
 */
internal fun RegistroUiState.aBorradorJson(): String = org.json.JSONObject().apply {
    put("paso", paso.name)
    put("usuario", usuario)
    put("nombre", nombre)
    put("apellido", apellido)
    put("email", email)
    put("telefono", telefono)
    fechaNacimiento?.let { put("fechaNacimiento", it.toString()) }
    put("ciudad", ciudad)
    put("contactoNombre", contactoNombre)
    put("contactoRelacion", contactoRelacion)
    put("contactoTelefono", contactoTelefono)
    put("aceptaAviso", aceptaAviso)
}.toString()

internal fun RegistroUiState.conBorrador(json: String): RegistroUiState = runCatching {
    val j = org.json.JSONObject(json)
    val pasoGuardado = runCatching { PasoRegistro.valueOf(j.getString("paso")) }.getOrNull()
    copy(
        // Sin contraseña recuperada, el registro normal vuelve al paso de la cuenta
        paso = if (emailGoogle == null) PasoRegistro.CUENTA else pasoGuardado?.takeIf { it in pasos } ?: paso,
        usuario = if (emailGoogle == null) j.optString("usuario") else usuario,
        nombre = j.optString("nombre"),
        apellido = j.optString("apellido"),
        email = j.optString("email"),
        telefono = j.optString("telefono"),
        fechaNacimiento = j.optString("fechaNacimiento").takeIf { it.isNotEmpty() }?.let(LocalDate::parse),
        ciudad = j.optString("ciudad"),
        contactoNombre = j.optString("contactoNombre"),
        contactoRelacion = j.optString("contactoRelacion"),
        contactoTelefono = j.optString("contactoTelefono"),
        aceptaAviso = j.optBoolean("aceptaAviso")
    )
}.getOrDefault(this)
