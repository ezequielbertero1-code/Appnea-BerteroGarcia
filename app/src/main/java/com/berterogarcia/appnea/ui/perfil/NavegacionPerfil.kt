package com.berterogarcia.appnea.ui.perfil

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.berterogarcia.appnea.AppContainer
import com.berterogarcia.appnea.navigation.Ruta
import com.berterogarcia.appnea.navigation.abrir
import com.berterogarcia.appnea.navigation.idEdicion
import com.berterogarcia.appnea.navigation.irAlLoginSinEstadoGuardado
import com.berterogarcia.appnea.navigation.volverDesde
import com.berterogarcia.appnea.ui.cuenta.CuentaScreen
import com.berterogarcia.appnea.ui.cuenta.CuentaViewModel
import com.berterogarcia.appnea.ui.perfil.certificados.CertificadoFormScreen
import com.berterogarcia.appnea.ui.perfil.certificados.CertificadoFormViewModel
import com.berterogarcia.appnea.ui.perfil.certificados.CertificadosScreen
import com.berterogarcia.appnea.ui.perfil.certificados.CertificadosViewModel
import com.berterogarcia.appnea.ui.perfil.contactos.ContactoFormScreen
import com.berterogarcia.appnea.ui.perfil.contactos.ContactoFormViewModel
import com.berterogarcia.appnea.ui.perfil.contactos.ContactosScreen
import com.berterogarcia.appnea.ui.perfil.contactos.ContactosViewModel
import com.berterogarcia.appnea.ui.perfil.datos.DatosPersonalesScreen
import com.berterogarcia.appnea.ui.perfil.datos.DatosPersonalesViewModel
import com.berterogarcia.appnea.ui.perfil.equipamiento.EquipamientoScreen
import com.berterogarcia.appnea.ui.perfil.equipamiento.EquipamientoViewModel
import com.berterogarcia.appnea.ui.perfil.equipamiento.EquipoFormScreen
import com.berterogarcia.appnea.ui.perfil.equipamiento.EquipoFormViewModel
import kotlinx.coroutines.flow.map

/**
 * Cuenta y las secciones del perfil. Todo trabaja sobre el usuario logueado: sin sesión,
 * las pantallas muestran "no hay sesión activa" y no dejan guardar (no crashean).
 */
fun NavGraphBuilder.perfilGraph(navController: NavHostController, app: AppContainer) {
    composable<Ruta.Cuenta> {
        val viewModel: CuentaViewModel = viewModel {
            CuentaViewModel(
                app.sessionManager,
                app.perfilRepository,
                app.inmersionRepository,
                app.sesionApneaRepository,
                app.motorEntrenamiento.estado.map { it.corriendo }
            )
        }
        CuentaScreen(
            viewModel = viewModel,
            onCerrarSesion = { navController.irAlLoginSinEstadoGuardado() },
            onEditarDatos = { navController.abrir(Ruta.DatosPersonales) },
            onVerEquipamiento = { navController.abrir(Ruta.Equipamiento) },
            onVerCertificados = { navController.abrir(Ruta.Certificados) },
            onVerContactos = { navController.abrir(Ruta.Contactos) },
            onVerEstadisticas = { navController.abrir(Ruta.Estadisticas()) }
        )
    }

    composable<Ruta.DatosPersonales> { entrada ->
        val viewModel: DatosPersonalesViewModel = viewModel { DatosPersonalesViewModel(app.perfilRepository) }
        DatosPersonalesScreen(viewModel = viewModel, onVolver = { navController.volverDesde(entrada) })
    }

    // ---------- Equipamiento ----------
    composable<Ruta.Equipamiento> { entrada ->
        val viewModel: EquipamientoViewModel = viewModel { EquipamientoViewModel(app.perfilRepository) }
        EquipamientoScreen(
            viewModel = viewModel,
            onVolver = { navController.volverDesde(entrada) },
            onAgregar = { navController.abrir(Ruta.FormEquipo()) },
            onEditar = { id -> navController.abrir(Ruta.FormEquipo(id)) }
        )
    }
    composable<Ruta.FormEquipo> { entrada ->
        val id = idEdicion(entrada.toRoute<Ruta.FormEquipo>().id)
        val viewModel: EquipoFormViewModel = viewModel { EquipoFormViewModel(app.perfilRepository, id) }
        EquipoFormScreen(viewModel = viewModel, onVolver = { navController.volverDesde(entrada) })
    }

    // ---------- Certificados ----------
    composable<Ruta.Certificados> { entrada ->
        val viewModel: CertificadosViewModel = viewModel { CertificadosViewModel(app.perfilRepository) }
        CertificadosScreen(
            viewModel = viewModel,
            onVolver = { navController.volverDesde(entrada) },
            onAgregar = { navController.abrir(Ruta.FormCertificado()) },
            onEditar = { id -> navController.abrir(Ruta.FormCertificado(id)) }
        )
    }
    composable<Ruta.FormCertificado> { entrada ->
        val id = idEdicion(entrada.toRoute<Ruta.FormCertificado>().id)
        val viewModel: CertificadoFormViewModel = viewModel { CertificadoFormViewModel(app.perfilRepository, id) }
        CertificadoFormScreen(viewModel = viewModel, onVolver = { navController.volverDesde(entrada) })
    }

    // ---------- Contactos de emergencia ----------
    composable<Ruta.Contactos> { entrada ->
        val viewModel: ContactosViewModel = viewModel { ContactosViewModel(app.perfilRepository) }
        ContactosScreen(
            viewModel = viewModel,
            onVolver = { navController.volverDesde(entrada) },
            onAgregar = { navController.abrir(Ruta.FormContacto()) },
            onEditar = { id -> navController.abrir(Ruta.FormContacto(id)) }
        )
    }
    composable<Ruta.FormContacto> { entrada ->
        val id = idEdicion(entrada.toRoute<Ruta.FormContacto>().id)
        val viewModel: ContactoFormViewModel = viewModel { ContactoFormViewModel(app.perfilRepository, id) }
        ContactoFormScreen(viewModel = viewModel, onVolver = { navController.volverDesde(entrada) })
    }
}
