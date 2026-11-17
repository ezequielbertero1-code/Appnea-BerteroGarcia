# Análisis de la app y plan de mejoras (segunda ronda)

> **Fecha:** 16/11/2026, sobre la rama `refactor/mejoras-analisis` con las fases 0 a 4 ya hechas (ver [`analisis-y-plan-de-mejoras.md`](analisis-y-plan-de-mejoras.md)).
> Este documento sigue la numeración del anterior: el plan empieza en la **Fase 5**.
>
> **Estado (16/11/2026):** las fases 5 a 10 están hechas en la rama `refactor/mejoras-analisis-2`,
> un commit por fase (la 9 al final, como se acordó). Quedan para más adelante: subir Kotlin, KSP
> y AGP juntos (lo piden los 6 avisos que quedan en el baseline de lint, `googleid` 1.2.1 y
> `kotlinx-serialization` 1.10+), registrar en Google Cloud el SHA-1 de Play App Signing y el
> Baseline Profile (ver [`publicacion.md`](publicacion.md)).
>
> Módulos después de la fase 9:
>
> | Módulo | Qué tiene | Tipo |
> |---|---|---|
> | `:app` | pantallas, ViewModels, navegación, `AppContainer`, servicio de entrenamiento, worker, login con Google | Android app |
> | `:core:ui` | tema, componentes compartidos, etiquetas de los datos y sus textos | Android lib + Compose |
> | `:core:data` | Room (y sus esquemas), repositorios, Open-Meteo, GPS, sesión, exportación, contraseñas | Android lib |
> | `:core:domain` | interfaces de repositorios, puertos, sesión, casos de uso | Kotlin puro |
> | `:core:model` | modelos del dominio, `intentar` y `Registro` | Kotlin puro |
> | `:core:testing` | fakes, datos de ejemplo, `ReglaDispatcherMain` | Kotlin puro (solo tests) |
>
> La configuración común (SDK, Java, desugaring, Compose) está en los plugins de `build-logic/`.

## Devolución resumida

La app quedó bastante mejor que en la primera ronda. Los problemas graves ya están resueltos:

- el entrenamiento sobrevive a la pantalla bloqueada;
- guardar una inmersión no espera a internet;
- los formularios no se pierden si Android mata la app;
- ninguna pantalla puede ver datos de otra cuenta;
- el release no lleva usuarios de prueba y tiene R8.

Además, hay tests de la lógica pura y de las migraciones, y CI en GitHub.

Lo que queda ya no son bugs que vea el usuario. Son, sobre todo, **cosas de diseño que hacen difícil seguir creciendo y testear**, y algunos **datos que la app no guarda**:

1. **Las capas están mezcladas.** La UI usa directamente las entidades de Room (39 archivos de `ui/` importan `data.local`), y el paquete `dominio` depende de `data`, cuando tendría que ser al revés. Cualquier cambio en la base se propaga hasta las pantallas.
2. **Los repositorios son clases concretas.** Por eso los ViewModels no se pueden testear con versiones falsas. Hay 43 tests unitarios, pero ninguno de un ViewModel.
3. **Las sesiones de entrenamiento no guardan hora ni duración.** La duración se ve en la pantalla de resultado y después se pierde. Los intentos del cronómetro libre ni siquiera se guardan.
4. **Todo vive en un solo módulo de Gradle**, con archivos grandes que concentran demasiado: `Navigation.kt` (538 líneas, arma los 20 ViewModels) y `NuevaInmersionViewModel.kt` (530 líneas, 39 funciones públicas).

Recomiendo seguir este orden:

1. Cerrar los datos que faltan (Fase 5).
2. Separar dominio de datos (Fase 6).
3. Achicar ViewModels y navegación (Fase 7).
4. Tests de ViewModels (Fase 8).
5. Recién después, la modularización con Gradle (Fase 9). Sin límites claros entre capas, partir en módulos solo mueve el problema de lugar.

## Estado de la ronda anterior

| Fase | Qué se hizo | Qué quedó a medias |
|---|---|---|
| 0 | Tests de lógica pura y migraciones; `intentar()` que loguea y deja pasar la cancelación | No se agregó Turbine (para testear flujos) |
| 1 | Foreground service del entrenamiento, guardado offline con WorkManager, borradores con `SavedStateHandle`, borrar con estado | **La tabla elegida en Entrenamiento se pierde** si Android mata la app (la que está corriendo sí sobrevive) |
| 2 | Usuario único desde la sesión, seeds solo en debug, updates atómicos | — |
| 3 | Puertos de ubicación y condiciones, mapper y validador del formulario, factores como `enum`, pantallas partidas | **Sin interfaz para el login con Google** (quedó en pausa); el `Geocodificador` y el exportador UDDF siguen siendo `object` |
| 4 | `AppContainer` en vez de factories, R8, versión, lint con baseline, CI | **Reorganización de paquetes parcial**: `data/local` todavía junta entidades, DAOs y `TablaApnea`. No se agregó detekt |

## Lo que está bien (y conviene conservar)

- **Persistencia (ACID):**
  - las claves foráneas tienen `CASCADE` e índice por `nombreUsuario`;
  - los borrados y updates filtran por `id` **y** usuario en la misma consulta;
  - el registro (normal y con Google) corre en una transacción;
  - las migraciones están escritas a mano y testeadas con `MigrationTestHelper`.
- **Seguridad:**
  - PBKDF2 con salt y re-hash automático;
  - comparación en tiempo constante;
  - bloqueo progresivo de intentos;
  - las cuentas de Google no aceptan contraseña, y un usuario normal no puede llamarse como un email, porque la regex no permite `@`.
- **El entrenamiento:** `MotorEntrenamiento` no depende de Android (el reloj y el aviso al servicio se inyectan) y está testeado. El servicio solo lo observa.
- **Manejo de errores:** `intentar()` deja pasar la `CancellationException` y loguea; ya no hay errores tragados en silencio.
- **Textos:** todos están en `strings.xml`, y la UI usa `TextoUi` para que los ViewModels no dependan de `Context`.

## Hallazgos

### Prioridad alta

1. **Las sesiones de entrenamiento no guardan hora ni duración.** `SesionApneaEntity` tiene solo `fecha`. Eso trae tres problemas:
   - la duración que muestra la pantalla de resultado se pierde al guardar;
   - dos sesiones del mismo día no se pueden ordenar;
   - Estadísticas no puede mostrar tiempo entrenado.

   Además, una sesión cortada en la preparación tiene marca 0 y desaparece del gráfico de "apnea más larga", sin ningún aviso (se vio en las pruebas).

2. **Los intentos del cronómetro libre se pierden.** `CronometroViewModel` los guarda solo en memoria: se pierden al salir de la pantalla, al cerrar sesión o si Android mata la app. La pantalla ya tiene preparado un `onGuardarIntentos` que nunca se conectó. Para la apnea estática, que es justamente lo que se quiere seguir en el tiempo, es el dato más importante.

3. **La UI depende de las entidades de Room (acoplamiento entre capas).**
   - 39 archivos de `ui/` importan clases de `data.local` (`InmersionEntity`, `EquipoEntity`, `SesionApneaEntity`…).
   - `dominio/Puertos.kt` y `dominio/RegistrarInmersion.kt` importan de `data`: el dominio depende de los datos, cuando la regla es la inversa.
   - `RegistrarInmersion` llama directamente al `object CondicionesRepository`, así que el único caso de uso que hay no se puede testear.

4. **No hay tests de ViewModels ni de repositorios.** Los ViewModels concentran la lógica que ve el usuario y nada la cubre:
   - el flujo de login con bloqueo;
   - el registro;
   - guardar una inmersión;
   - el análisis de factores de Estadísticas;
   - guardar una sesión de entrenamiento.

   La causa es el hallazgo 5: no hay forma de pasarles versiones falsas.

### Prioridad media

5. **DIP: los repositorios son clases concretas.**
   - `InmersionRepository`, `PerfilRepository`, `SesionApneaRepository`, etc. no tienen interfaz.
   - Siguen siendo `object`: `GoogleAuth` (la pantalla de Login lo llama directamente), `Geocodificador`, `UbicacionHelper`, `UddfExporter`, `OpenMeteoService` y `CondicionesRepository`.
   - El reloj (`LocalDate.now()`, en 5 ViewModels) y los dispatchers (`Dispatchers.IO`, en 4 archivos) están fijos en el código, así que los tests no pueden controlar el tiempo.

6. **`DetalleInmersionViewModel` recibe `Context` y `Uri`** para exportar UDDF: abre el archivo con el `contentResolver` dentro del ViewModel. La exportación tendría que escribir en un `OutputStream`, y el archivo lo tendría que abrir la capa de Android.

7. **`Navigation.kt` hace demasiado (SRP).** En 538 líneas define las rutas como texto, arma los 20 ViewModels y conecta la navegación de todas las pantallas. Cada pantalla nueva agranda el mismo archivo, que además es el que más conflictos de merge va a generar entre dos personas. Navigation ya permite **rutas tipadas** (`@Serializable`), que evitan errores con los argumentos (`{id}`, `?pestania=`).

8. **`NuevaInmersionViewModel` sigue grande** (530 líneas, 39 funciones públicas). Casi todo son `onXChange` de una línea. Se ordena con un único `onEvento(EventoFormulario)` (una `sealed interface`) o con un estado por etapa del formulario.

9. **Se pierde la tabla elegida en Entrenamiento** si Android mata la app en segundo plano. Le falta `SavedStateHandle`.

10. **Las reglas de backup son las de la plantilla.** `allowBackup="true"` y los XML vienen sin configurar, así que se respalda todo:
    - la base, que incluye los hashes de contraseñas;
    - la sesión abierta: al restaurar en otro teléfono, el usuario entra sin loguearse;
    - el contador de intentos fallidos.

    Hay que decidir qué se respalda (la base sí, la sesión y los intentos no) y escribirlo en `data_extraction_rules.xml` y `backup_rules.xml`.

11. **Es un único módulo de Gradle.** Con 21.500 líneas y dos personas trabajando, todo compila junto, cualquier archivo puede importar cualquier otro y nada impide que la UI use la base directamente. La regla de capas hoy depende solo de la disciplina.

### Prioridad baja

12. **Restos de la plantilla y del desarrollo:**
    - nombres `MyApplication`, `MyApplicationTheme`, `Theme.MyApplication` y el proyecto `"My Application"`;
    - 9 comentarios `[CAMBIO]`/`[NUEVO]`;
    - archivos vacíos: `ui/extracciondatos/*` y `InformacionViewModel` (una clase sin nada).
13. **`ui/components` mezcla genérico con específico.** Por ejemplo, `FilaInmersion` y `MapaLibre` son de una feature, no componentes reutilizables.
14. **Falta funcionalidad de cuenta:** no se puede cambiar la contraseña ni borrar la cuenta. Si la app se publica en Play, **borrar la cuenta es obligatorio** para las apps que permiten crearla.
15. **Login con Google:**
    - depende del SHA-1 registrado en Google Cloud y de `client_web` en `local.properties`;
    - no tiene interfaz, así que no se puede testear.

    La contraseña `"!google"` como marca de "cuenta de Google" funciona, pero una columna explícita (`tipoCuenta`) sería más clara.
16. **Lint:** quedan 54 advertencias en la baseline:
    - 15 de plurales (`PluralsCandidate`): textos como "1 sesiones";
    - 15 de dependencias desactualizadas;
    - 7 de `Log` sin wrapper;
    - el resto, menores.

    No hay detekt ni ktlint para el estilo de Kotlin.
17. **No hay ningún `@Preview`** de Compose. Cada ajuste visual obliga a correr la app en el teléfono.
18. **Ciudad fija:** "Comodoro Rivadavia" es la ubicación por defecto (28 usos). Si una inmersión no tiene lugar marcado ni GPS, las condiciones se piden para Comodoro y no hay nada que lo indique en el detalle.

## Modularización propuesta

Así está hoy:

```
:app
 ├─ data/ (local, remote, repository, sesion, seguridad, ubicacion, condiciones, trabajos, export, perfil, sonido)
 ├─ dominio/          ← depende de data (mal)
 ├─ entrenamiento/
 ├─ navigation/
 └─ ui/<feature>/     ← depende de data.local (mal)
```

Así quedaría, con las dependencias en una sola dirección:

```
:app                     MainActivity, navegación raíz, AppContainer (arma todo)
 ├─ :core:model          modelos puros de Kotlin: Inmersion, SesionApnea, TablaApnea, FactorEntrenamiento…
 ├─ :core:domain         interfaces de repositorios y servicios, casos de uso  (depende de :core:model)
 ├─ :core:data           Room, Open-Meteo, WorkManager, implementaciones de repos  (depende de :core:domain)
 └─ :core:ui             tema, componentes reutilizables, TextoUi
```

Las features (`ui/inmersiones`, `ui/entrenamiento`, `ui/perfil`, `ui/condiciones`, `ui/estadisticas`, `ui/auth`) dependen solo de `:core:domain` y `:core:ui`. **Nunca de `:core:data`**: Gradle lo impide.

**Recomendación:** llegar hasta los módulos `core` y dejar las features como paquetes dentro de `:app`. Partir cada feature en su módulo (`:feature:inmersiones`, etc.) solo vale la pena si crecen los tiempos de compilación o el equipo. Para dos personas, los módulos `core` ya dan lo importante: la regla de capas la hace cumplir el compilador, no la memoria.

## Plan paso a paso

Cada fase va en su propio commit (o varios), en una rama nueva desde `master` una vez mergeada la actual. Las fases 5 y 7 tocan la base o la UI: después de cada una, **pruebas manuales en el teléfono**.

### Fase 5: datos que faltan y cierres de la ronda anterior

1. **Hora y duración en las sesiones.**
   - Agregar `horaInicio` y `duracionSegundos` a `SesionApneaEntity`, con la migración 11→12 (las sesiones viejas quedan en `null`) y su test en `MigracionesTest`.
   - Guardar la duración de `ResultadoTabla`.
   - Ordenar por fecha y hora.
   - Mostrar la duración y el tiempo total entrenado en Estadísticas.
2. **Guardar los intentos del cronómetro libre.** Una tabla nueva `intentos_apnea` (usuario, fecha y hora, duración) con FK y `CASCADE`, más la misma migración.
   - Conectar el `onGuardarIntentos` que ya existe.
   - Agregar los intentos a Estadísticas, en una serie aparte de las tablas.
3. **Avisar en Estadísticas** que las sesiones sin apnea (cortadas en la preparación) no entran en el gráfico, o mostrarlas con marca 0.
4. **`SavedStateHandle` para la tabla elegida** en Entrenamiento (guardar su nombre o id).
5. **Aviso al cerrar sesión con una tabla en curso:** "Hay un entrenamiento en curso, ¿cortarlo y salir?". Es opcional; hoy se corta directamente.
6. **Reglas de backup explícitas:** incluir la base, excluir `sesion.xml` y los intentos de login.

### Fase 6: modelos de dominio y límites entre capas (SOLID: DIP, ISP)

7. **Crear los modelos de dominio** (`Inmersion`, `SesionApnea`, `Equipo`, `Certificado`, `Contacto`, `Perfil`) como clases puras, con mappers `Entity ↔ modelo` en `data`. Los ViewModels y las pantallas usan solo los modelos.
8. **Interfaces de repositorio en `dominio`** (`RepositorioInmersiones`, `RepositorioSesiones`, `RepositorioPerfil`…), implementadas en `data`. `AppContainer` entrega las interfaces.
9. **Que `dominio` no importe nada de `data`.**
   - `RegistrarInmersion` recibe `FuenteCondiciones` en lugar de usar el `object`.
   - `Ubicacion` y `CondicionesRegistro` pasan a `dominio`.
10. **Puertos que faltan:**
    - `AutenticadorGoogle`, para sacar `GoogleAuth` de `Login.kt`;
    - `Geocodificador`;
    - `ExportadorInmersiones`, que escribe en un `OutputStream`. El `Uri` lo abre la pantalla y `DetalleInmersionViewModel` deja de recibir `Context`.
11. **Inyectar el reloj y los dispatchers:** `java.time.Clock` y un `Despachadores` con `io` y `default`, pasados por `AppContainer`.

### Fase 7: ViewModels y navegación más chicas (SRP, OCP)

12. **Navegación por feature.**
    - Cada feature expone una función `NavGraphBuilder.inmersionesGraph(...)` en su paquete.
    - `Navigation.kt` queda solo con el `NavHost` y las llamadas a esas funciones.
    - Pasar a **rutas tipadas** con `@Serializable`. kotlinx-serialization ya está en el proyecto; hay que agregar el plugin.
13. **`NuevaInmersionViewModel` con eventos:** una `sealed interface EventoFormulario` y un único `onEvento()`. La lógica de cada etapa sale a funciones puras y testeables (`reducir(estado, evento)`).
14. **Ordenar `ui/components`:** lo genérico queda en `core/ui`, y lo de una sola feature (`FilaInmersion`, `MapaLibre`) se mueve a esa feature.
15. **Limpieza:**
    - renombrar `MyApplication` → `AppneaApp`, el tema y el proyecto;
    - borrar `ui/extracciondatos` e `InformacionViewModel`;
    - sacar los comentarios `[CAMBIO]`/`[NUEVO]`.

### Fase 8: tests

16. **Agregar Turbine y versiones falsas** de los repositorios (posibles gracias a la Fase 6).
17. **Tests de ViewModels:**
    - Login: bloqueo progresivo y cuenta de Google.
    - Registro: validación y borrador.
    - NuevaInmersion: guardar nueva, editar y offline.
    - Estadísticas: períodos y análisis de factores.
    - EntrenamientoApnea: guardar la sesión con duración.
    - Cuenta.
18. **Tests del caso de uso `RegistrarInmersion`** (cuándo reprograma las condiciones y cuándo las conserva) y **del worker**, con `work-testing`.
19. **Tests de DAOs con Room en memoria** (instrumentados, con `am instrument` y **nunca con `connectedAndroidTest`**, que borra los datos del teléfono).
20. **Agregar `@Preview`** a los componentes principales y dos o tres tests de UI de los flujos críticos: registrar una inmersión e iniciar y abortar una tabla.

### Fase 9: modularización con Gradle

21. **Crear `build-logic`** con plugins de convención: una sola configuración de Android y Kotlin compartida por todos los módulos.
22. **Extraer los módulos en este orden:**
    1. `:core:model`, que no depende de nadie;
    2. `:core:domain`;
    3. `:core:data`: Room (con `schemas/` y migraciones), Open-Meteo y WorkManager;
    4. `:core:ui`: tema y componentes.
23. **Mover los tests a su módulo** (los de lógica pura a `:core:model`/`:core:domain`, los de migraciones a `:core:data`) y ajustar el CI.
24. Confirmar que **las features no pueden importar `:core:data`**. Si alguna lo necesita, es porque falta una interfaz en `:core:domain`.

### Fase 10: cuenta, calidad y publicación

25. **Cambiar contraseña** (hay que pedir la actual) y **borrar la cuenta**, con confirmación; el `CASCADE` ya borra todos sus datos.
26. **Login con Google:** registrar el SHA-1 de la clave de release y de debug en Google Cloud, probar con el APK firmado y reemplazar `"!google"` por una columna `tipoCuenta` (con su migración).
27. **Lint:**
    - pasar los 15 textos a `plurals`;
    - actualizar las dependencias;
    - pasar los `Log` a un wrapper;
    - regenerar la baseline más chica;
    - agregar detekt al CI.
28. **Indicar en el detalle** de una inmersión cuando las condiciones son aproximadas (sin lugar marcado, pedidas para el GPS del teléfono o para Comodoro).
29. **Publicación:** generar un App Bundle (AAB) en lugar de APK y evaluar un Baseline Profile para el arranque.

## Orden y criterio

- **Fase 5** primero: es lo que el usuario nota (datos que hoy se pierden) y es chica.
- **Fase 6** antes que la 8: sin interfaces no hay versiones falsas, y sin ellas no hay tests de ViewModels.
- **Fase 9** después de la 6 y la 7: si los límites entre capas no existen en el código, moverlos a módulos de Gradle es solo cambiar archivos de carpeta.
- **Fase 10** se puede intercalar: los puntos 25 y 26 son independientes del resto.
