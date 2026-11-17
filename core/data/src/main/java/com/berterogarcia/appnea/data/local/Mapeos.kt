package com.berterogarcia.appnea.data.local

import com.berterogarcia.appnea.dominio.modelo.Certificado
import com.berterogarcia.appnea.dominio.modelo.ContactoEmergencia
import com.berterogarcia.appnea.dominio.modelo.Equipo
import com.berterogarcia.appnea.dominio.modelo.Inmersion
import com.berterogarcia.appnea.dominio.modelo.IntentoLibre
import com.berterogarcia.appnea.dominio.modelo.PerfilUsuario
import com.berterogarcia.appnea.dominio.modelo.SesionApnea
import com.berterogarcia.appnea.dominio.modelo.TablaPersonalizada

/*
 * Entidad de Room <-> modelo de dominio. Las entidades tienen el dueño (nombreUsuario) porque
 * la base es de todos los usuarios del teléfono; los modelos no, porque las pantallas solo ven
 * los datos del usuario logueado. El dueño lo pone el repositorio al guardar.
 */

fun InmersionEntity.aModelo() = Inmersion(
    id = id, fecha = fecha, lugar = lugar, profundidadMetros = profundidadMetros,
    paradaSeguridadMin = paradaSeguridadMin, tiempoFondo = tiempoFondo, horaEntrada = horaEntrada,
    horaSalida = horaSalida, caracteristicasAgua = caracteristicasAgua, lastreKg = lastreKg,
    proteccionMm = proteccionMm, equipamiento = equipamiento, temperaturaAireC = temperaturaAireC,
    temperaturaAguaC = temperaturaAguaC, visibilidad = visibilidad, visibilidadMetros = visibilidadMetros,
    actividad = actividad, comentarios = comentarios, condiciones = condiciones, tipo = tipo,
    latitud = latitud, longitud = longitud, apnea = apnea
)

fun Inmersion.aEntidad(usuario: String) = InmersionEntity(
    id = id, fecha = fecha, lugar = lugar, profundidadMetros = profundidadMetros,
    paradaSeguridadMin = paradaSeguridadMin, tiempoFondo = tiempoFondo, horaEntrada = horaEntrada,
    horaSalida = horaSalida, caracteristicasAgua = caracteristicasAgua, lastreKg = lastreKg,
    proteccionMm = proteccionMm, equipamiento = equipamiento, temperaturaAireC = temperaturaAireC,
    temperaturaAguaC = temperaturaAguaC, visibilidad = visibilidad, visibilidadMetros = visibilidadMetros,
    actividad = actividad, comentarios = comentarios, condiciones = condiciones, nombreUsuario = usuario,
    tipo = tipo, latitud = latitud, longitud = longitud, apnea = apnea
)

fun SesionApneaEntity.aModelo() = SesionApnea(
    id = id, fecha = fecha, nombreTabla = nombreTabla, completada = completada, faseAlcanzada = faseAlcanzada,
    totalFases = totalFases, marcaSegundos = marcaSegundos, factores = factores, sensaciones = sensaciones,
    horaInicio = horaInicio, duracionSegundos = duracionSegundos
)

fun SesionApnea.aEntidad(usuario: String) = SesionApneaEntity(
    id = id, fecha = fecha, nombreTabla = nombreTabla, completada = completada, faseAlcanzada = faseAlcanzada,
    totalFases = totalFases, marcaSegundos = marcaSegundos, factores = factores, sensaciones = sensaciones,
    nombreUsuario = usuario, horaInicio = horaInicio, duracionSegundos = duracionSegundos
)

fun IntentoApneaEntity.aModelo() = IntentoLibre(id = id, fecha = fecha, hora = hora, duracionMs = duracionMs)

fun IntentoLibre.aEntidad(usuario: String) =
    IntentoApneaEntity(id = id, fecha = fecha, hora = hora, duracionMs = duracionMs, nombreUsuario = usuario)

fun TablaPersonalizadaEntity.aModelo() = TablaPersonalizada(id = id, nombre = nombre, fases = fases)

fun TablaPersonalizada.aEntidad(usuario: String) =
    TablaPersonalizadaEntity(id = id, nombre = nombre, fases = fases, nombreUsuario = usuario)

fun PerfilUsuarioEntity.aModelo() = PerfilUsuario(
    nombre = nombre, apellido = apellido, email = email, telefono = telefono, fechaNacimiento = fechaNacimiento,
    ciudad = ciudad, companeroHabitual = companeroHabitual, actualizadoEn = actualizadoEn
)

fun PerfilUsuario.aEntidad(usuario: String) = PerfilUsuarioEntity(
    nombreUsuario = usuario, nombre = nombre, apellido = apellido, email = email, telefono = telefono,
    fechaNacimiento = fechaNacimiento, ciudad = ciudad, companeroHabitual = companeroHabitual,
    actualizadoEn = actualizadoEn
)

fun EquipoEntity.aModelo() = Equipo(
    id = id, categoria = categoria, marca = marca, modelo = modelo, talle = talle, fechaCompra = fechaCompra,
    proximoMantenimiento = proximoMantenimiento, enUso = enUso, notas = notas
)

fun Equipo.aEntidad(usuario: String) = EquipoEntity(
    id = id, nombreUsuario = usuario, categoria = categoria, marca = marca, modelo = modelo, talle = talle,
    fechaCompra = fechaCompra, proximoMantenimiento = proximoMantenimiento, enUso = enUso, notas = notas
)

fun CertificadoEntity.aModelo() = Certificado(
    id = id, tipo = tipo, titulo = titulo, entidad = entidad, escuelaInstructor = escuelaInstructor,
    numero = numero, fechaEmision = fechaEmision, fechaVencimiento = fechaVencimiento, notas = notas
)

fun Certificado.aEntidad(usuario: String) = CertificadoEntity(
    id = id, nombreUsuario = usuario, tipo = tipo, titulo = titulo, entidad = entidad,
    escuelaInstructor = escuelaInstructor, numero = numero, fechaEmision = fechaEmision,
    fechaVencimiento = fechaVencimiento, notas = notas
)

fun ContactoEmergenciaEntity.aModelo() = ContactoEmergencia(
    id = id, nombre = nombre, relacion = relacion, telefono = telefono,
    telefonoAlternativo = telefonoAlternativo, esPrincipal = esPrincipal, notas = notas
)

fun ContactoEmergencia.aEntidad(usuario: String) = ContactoEmergenciaEntity(
    id = id, nombreUsuario = usuario, nombre = nombre, relacion = relacion, telefono = telefono,
    telefonoAlternativo = telefonoAlternativo, esPrincipal = esPrincipal, notas = notas
)
