package com.berterogarcia.appnea.ui.registro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.data.local.ContactoEmergenciaEntity
import com.berterogarcia.appnea.data.local.PerfilUsuarioEntity
import com.berterogarcia.appnea.data.local.SessionManager
import com.berterogarcia.appnea.data.repository.NuevoRegistro
import com.berterogarcia.appnea.data.repository.RegistroRepository
import com.berterogarcia.appnea.data.repository.ResultadoRegistro
import com.berterogarcia.appnea.ui.perfil.comun.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class PasoRegistro(val titulo: String) {
    CUENTA("Tu cuenta"),
    PERSONALES("Datos personales"),
    EMERGENCIA("Contacto de emergencia"),
    CONFIRMAR("Confirmá y creá tu cuenta")
}

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
    val paso: PasoRegistro = PasoRegistro.CUENTA,
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
    val errores: Map<CampoRegistro, String> = emptyMap(),
    val verificandoUsuario: Boolean = false,
    val creando: Boolean = false,
    val registroOk: Boolean = false,
    val errorGeneral: String? = null
) {
    val fuerza: FuerzaPassword get() = ReglasRegistro.fuerza(password)
    val ocupado: Boolean get() = verificandoUsuario || creando
    val esPrimerPaso: Boolean get() = paso == PasoRegistro.entries.first()
    val esUltimoPaso: Boolean get() = paso == PasoRegistro.entries.last()
    val pasosConError: Set<PasoRegistro> get() = errores.keys.map { it.paso }.toSet()
}

/** Registro por pasos. Al terminar crea usuario + perfil + contacto e inicia sesión. */
class RegistroViewModel(
    private val repository: RegistroRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroUiState())
    val uiState: StateFlow<RegistroUiState> = _uiState.asStateFlow()

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
        _uiState.update { it.copy(paso = PasoRegistro.entries[s.paso.ordinal - 1], errorGeneral = null) }
        return true
    }

    /** Ir a un paso anterior (indicador o "Editar" del resumen). No se puede saltar hacia adelante. */
    fun irA(paso: PasoRegistro) {
        val s = _uiState.value
        if (!s.ocupado && paso.ordinal < s.paso.ordinal) _uiState.update { it.copy(paso = paso) }
    }

    private fun avanzar() {
        _uiState.update { it.copy(paso = PasoRegistro.entries[it.paso.ordinal + 1]) }
    }

    private fun verificarUsuarioYAvanzar(usuario: String) {
        _uiState.update { it.copy(verificandoUsuario = true) }
        viewModelScope.launch {
            // Si falla la consulta dejamos avanzar: al crear la cuenta se vuelve a verificar.
            val disponible = runCatching { repository.usuarioDisponible(usuario) }.getOrDefault(true)
            _uiState.update {
                if (disponible) it.copy(verificandoUsuario = false, paso = PasoRegistro.PERSONALES)
                else it.copy(
                    verificandoUsuario = false,
                    errores = it.errores + (CampoRegistro.USUARIO to "Ese usuario ya existe, elegí otro.")
                )
            }
        }
    }

    // --------------------------------- Crear cuenta ---------------------------------

    fun crearCuenta() {
        val s = _uiState.value
        if (s.ocupado || s.registroOk) return

        // Se valida toodo de nuevo y, si algo falla, se vuelve al primer paso con error.
        val errores = validar(s)
        if (errores.isNotEmpty()) {
            val primerPaso = errores.keys.minOf { it.paso.ordinal }
            _uiState.update { it.copy(errores = errores, paso = PasoRegistro.entries[primerPaso]) }
            return
        }

        _uiState.update { it.copy(creando = true, errorGeneral = null) }
        viewModelScope.launch {
            val resultado = runCatching { repository.registrar(armarRegistro(s)) }
            _uiState.update { actual ->
                resultado.fold(
                    onSuccess = { r ->
                        when (r) {
                            ResultadoRegistro.Creado -> {
                                sessionManager.guardarUsuario(s.usuario) // entra directo, sin pasar por el login
                                actual.copy(creando = false, registroOk = true)
                            }
                            ResultadoRegistro.UsuarioExistente -> actual.copy(
                                creando = false,
                                paso = PasoRegistro.CUENTA,
                                errores = actual.errores + (CampoRegistro.USUARIO to "Ese usuario ya existe, elegí otro.")
                            )
                        }
                    },
                    onFailure = {
                        actual.copy(creando = false, errorGeneral = "No se pudo crear la cuenta. Intentá de nuevo.")
                    }
                )
            }
        }
    }

    private fun armarRegistro(s: RegistroUiState) = NuevoRegistro(
        usuario = s.usuario,
        password = s.password,
        perfil = PerfilUsuarioEntity(
            nombreUsuario = s.usuario,
            nombre = s.nombre.trim(),
            apellido = s.apellido.trim(),
            email = s.email.trim(),
            telefono = s.telefono.trim(),
            fechaNacimiento = s.fechaNacimiento,
            ciudad = s.ciudad.trim()
        ),
        contacto = ContactoEmergenciaEntity(
            nombreUsuario = s.usuario,
            nombre = s.contactoNombre.trim(),
            relacion = s.contactoRelacion.trim(),
            telefono = s.contactoTelefono.trim(),
            esPrincipal = true
        )
    )

    // ---------------------------------- Validación ----------------------------------

    private fun validar(s: RegistroUiState): Map<CampoRegistro, String> = buildMap {
        ReglasRegistro.usuario(s.usuario)?.let { put(CampoRegistro.USUARIO, it) }
        ReglasRegistro.password(s.password)?.let { put(CampoRegistro.PASSWORD, it) }
        ReglasRegistro.confirmacion(s.password, s.confirmacion)?.let { put(CampoRegistro.CONFIRMACION, it) }

        (Validaciones.obligatorio(s.nombre, "tu nombre") ?: Validaciones.largoMaximo(s.nombre, 60))
            ?.let { put(CampoRegistro.NOMBRE, it) }
        (Validaciones.obligatorio(s.apellido, "tu apellido") ?: Validaciones.largoMaximo(s.apellido, 60))
            ?.let { put(CampoRegistro.APELLIDO, it) }
        (Validaciones.obligatorio(s.email, "tu email") ?: Validaciones.email(s.email))
            ?.let { put(CampoRegistro.EMAIL, it) }
        Validaciones.telefono(s.telefono, obligatorio = true)?.let { put(CampoRegistro.TELEFONO, it) }
        when {
            s.fechaNacimiento == null -> put(CampoRegistro.NACIMIENTO, "Ingresá tu fecha de nacimiento.")
            else -> Validaciones.noFutura(s.fechaNacimiento)?.let { put(CampoRegistro.NACIMIENTO, it) }
        }
        Validaciones.largoMaximo(s.ciudad, 80)?.let { put(CampoRegistro.CIUDAD, it) }

        (Validaciones.obligatorio(s.contactoNombre, "el nombre del contacto") ?: Validaciones.largoMaximo(s.contactoNombre, 60))
            ?.let { put(CampoRegistro.CONTACTO_NOMBRE, it) }
        Validaciones.largoMaximo(s.contactoRelacion, 40)?.let { put(CampoRegistro.CONTACTO_RELACION, it) }
        Validaciones.telefono(s.contactoTelefono, obligatorio = true)?.let { put(CampoRegistro.CONTACTO_TELEFONO, it) }
        if (s.contactoTelefono.isNotBlank() && s.contactoTelefono.trim() == s.telefono.trim()) {
            put(CampoRegistro.CONTACTO_TELEFONO, "Tiene que ser el teléfono de OTRA persona.")
        }

        if (!s.aceptaAviso) put(CampoRegistro.ACEPTA_AVISO, "Tenés que leer y aceptar el aviso para continuar.")
    }

    companion object {
        fun Factory(repository: RegistroRepository, sessionManager: SessionManager) = viewModelFactory {
            initializer { RegistroViewModel(repository, sessionManager) }
        }
    }
}