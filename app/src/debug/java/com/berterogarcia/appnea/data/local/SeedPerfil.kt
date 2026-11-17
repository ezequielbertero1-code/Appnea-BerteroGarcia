package com.berterogarcia.appnea.data.local

import com.berterogarcia.appnea.dominio.modelo.CategoriaEquipo
import com.berterogarcia.appnea.dominio.modelo.TipoCertificado
import java.time.LocalDate

/**
 * Perfil de ejemplo para el usuario de prueba "Franco": son los mismos datos que antes
 * se mostraban "simulados" en Cuenta, pero ahora guardados de verdad en la base.
 *
 * Se llama desde SeedDatos.sembrarSiHaceFalta (DESPUÉS de asegurar que existe el usuario,
 * porque las tablas del perfil tienen FK a `usuarios`).
 *
 * Es idempotente: si Franco ya tiene datos cargados, no duplica nada.
 */
object SeedPerfil {

    suspend fun sembrarPerfilFranco(db: AppDatabase) {
        val usuario = SeedDatos.USUARIO_PRUEBA

        if (db.perfilUsuarioDao().obtener(usuario) == null) {
            db.perfilUsuarioDao().guardar(
                PerfilUsuarioEntity(
                    nombreUsuario = usuario,
                    nombre = "Franco",
                    apellido = "García",
                    email = "franco.garcia@appnea.com.ar",
                    telefono = "+54 297 415-2231",
                    fechaNacimiento = LocalDate.of(1995, 3, 14),
                    ciudad = "Comodoro Rivadavia, Chubut",
                    companeroHabitual = "Eze Bertero"
                )
            )
        }

        if (db.certificadoDao().contar(usuario) == 0) {
            listOf(
                CertificadoEntity(
                    nombreUsuario = usuario,
                    tipo = TipoCertificado.BUCEO,
                    titulo = "Buceador 2 Estrellas",
                    entidad = "CMAS",
                    escuelaInstructor = "Escuela de Buceo Zeus",
                    numero = "ARG-CMAS-2023-04871",
                    fechaEmision = LocalDate.of(2023, 11, 4)
                ),
                CertificadoEntity(
                    nombreUsuario = usuario,
                    tipo = TipoCertificado.APNEA,
                    titulo = "Apnea Nivel 1",
                    entidad = "CMAS",
                    escuelaInstructor = "Escuela de Buceo Zeus",
                    fechaEmision = LocalDate.of(2024, 2, 17)
                ),
                CertificadoEntity(
                    nombreUsuario = usuario,
                    tipo = TipoCertificado.APTO_MEDICO,
                    titulo = "Apto médico",
                    fechaEmision = LocalDate.of(2025, 6, 30),
                    fechaVencimiento = LocalDate.of(2027, 6, 30)
                )
            ).forEach { db.certificadoDao().insertar(it) }
        }

        if (db.equipoDao().contar(usuario) == 0) {
            listOf(
                EquipoEntity(
                    nombreUsuario = usuario, categoria = CategoriaEquipo.ALETAS,
                    marca = "Cressi", modelo = "Gara 3000 LD", talle = "42-43"
                ),
                EquipoEntity(
                    nombreUsuario = usuario, categoria = CategoriaEquipo.MASCARA,
                    marca = "Cressi", modelo = "Calibro"
                ),
                EquipoEntity(
                    nombreUsuario = usuario, categoria = CategoriaEquipo.TRAJE,
                    marca = "Omer", modelo = "Camu 3D", talle = "L · 7 mm"
                ),
                EquipoEntity(
                    nombreUsuario = usuario, categoria = CategoriaEquipo.COMPUTADORA,
                    marca = "Cressi", modelo = "Donatello",
                    proximoMantenimiento = LocalDate.now().plusDays(20),
                    notas = "Cambio de pila"
                )
            ).forEach { db.equipoDao().insertar(it) }
        }

        if (db.contactoEmergenciaDao().contar(usuario) == 0) {
            db.contactoEmergenciaDao().guardar(
                ContactoEmergenciaEntity(
                    nombreUsuario = usuario,
                    nombre = "Laura González",
                    relacion = "Hermana",
                    telefono = "+54 297 430-8812",
                    esPrincipal = true
                )
            )
        }
    }
}
