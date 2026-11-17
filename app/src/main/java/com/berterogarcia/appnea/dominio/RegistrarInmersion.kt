package com.berterogarcia.appnea.dominio

import com.berterogarcia.appnea.dominio.modelo.Inmersion
import com.berterogarcia.appnea.dominio.modelo.Ubicacion
import com.berterogarcia.appnea.dominio.repositorios.RepositorioInmersiones
import java.time.LocalDateTime

/**
 * Caso de uso "guardar una inmersión" (nueva o editada).
 *
 * Guarda AL INSTANTE, sin esperar a internet: en la playa la señal suele ser mala y antes el botón
 * quedaba "guardando" hasta 40 s. Las condiciones se guardan primero con lo que se calcula sin red
 * (fase lunar) y [programador] completa clima y mar en segundo plano cuando hay conexión.
 *
 * Si se edita sin cambiar fecha, hora de entrada ni lugar marcado, se conservan las condiciones
 * que ya tenía (no tiene sentido volver a pedirlas).
 */
class RegistrarInmersion(
    private val repository: RepositorioInmersiones,
    private val condiciones: FuenteCondiciones,
    private val programador: ProgramadorCondiciones
) {
    /**
     * @param ubicacion de dónde pedir las condiciones (lugar marcado, o el teléfono, o Comodoro).
     * @return true si se guardó.
     */
    suspend operator fun invoke(inmersion: Inmersion, anterior: Inmersion?, ubicacion: Ubicacion): Boolean {
        val momento = inmersion.fecha.atTime(inmersion.horaEntrada)
        val mismoMomentoYLugar = anterior != null &&
            anterior.fecha == inmersion.fecha && anterior.horaEntrada == inmersion.horaEntrada &&
            anterior.latitud == inmersion.latitud && anterior.longitud == inmersion.longitud

        val condiciones = if (mismoMomentoYLugar) anterior.condiciones
        else condiciones.registroAstronomico(ubicacion.latitud, ubicacion.longitud, momento)

        val aGuardar = inmersion.copy(condiciones = condiciones)
        val id = if (anterior != null) {
            anterior.id.takeIf { repository.actualizar(aGuardar.copy(id = anterior.id)) }
        } else {
            repository.guardar(aGuardar)
        } ?: return false

        if (!mismoMomentoYLugar) programador.programar(id, ubicacion, momento)
        return true
    }
}
