# Análisis de la app y plan de mejoras

Revisión completa de AppneaMe por capas: datos (Room, DAOs, migraciones, seeds), seguridad (login, contraseñas, sesión), dominio (condiciones, mareas, perfil simulado), ViewModels, UI y navegación, y build/tests. Son unas 20.000 líneas en 145 archivos.

Para cada punto se tuvo en cuenta cómo se usa la app: se registra la inmersión en la playa, a veces sin señal, y se entrena apnea con el teléfono apoyado y la pantalla bloqueada.

## Devolución resumida

La base es buena, mejor que la de un proyecto típico de este tamaño. La arquitectura es MVVM con repositorios: estado de pantalla en `StateFlow`, Room con migraciones escritas a mano y esquemas exportados, y los textos en `strings.xml` (632 usos, ningún texto suelto en la UI). Varias cosas ya están bien hechas:

- **Seguridad:** contraseñas con PBKDF2 y salt (se re-hashean solas si quedaron viejas), comparación en tiempo constante y bloqueo progresivo contra fuerza bruta.
- **Datos por usuario:** cada tabla tiene clave foránea a `usuarios` con borrado en cascada, y los DAOs filtran por usuario.
- **ACID:** el registro (usuario + perfil + contacto) va en una sola transacción, igual que las migraciones y el seed de Demo. La regla "siempre hay un contacto principal" está dentro de `@Transaction`.
- **Detalles finos:** los cronómetros usan reloj monotónico, el trabajo pesado corre fuera del hilo principal, y se usa `@Upsert` en vez de `REPLACE`.

Los problemas importantes son pocos y se concentran en cuatro áreas:

- **Cómo se comporta la app en un teléfono real:** en segundo plano, sin señal, cuando Android mata el proceso.
- **La inconsistencia en el manejo del usuario logueado.**
- **Cero tests.**
- **Dependencias concretas** (singletons `object`) que no se pueden reemplazar en tests.

## Hallazgos

### Prioridad alta

1. **El entrenamiento de apnea depende de que la app esté en primer plano.** Las tablas y los avisos sonoros corren en el `viewModelScope` (`EntrenamientoApneaViewModel.kt`), sin un foreground service. Si el usuario bloquea el teléfono o cambia de app, Android puede frenar o matar el proceso. En Xiaomi/MIUI pasa muy seguido. En una app de seguridad para apnea, que se corten los avisos es grave.

2. **Guardar una inmersión espera a internet.** `NuevaInmersionViewModel.guardar()` pide clima y mar a Open-Meteo antes de guardar: hasta 2 pedidos de 10 s de conexión más 10 s de lectura cada uno. En la playa con señal débil, el botón queda "guardando" hasta 40 s. Lo correcto para esta app es guardar al instante y completar las condiciones después.

3. **El usuario logueado se maneja de dos formas distintas.**
   - `InmersionRepository`, `SesionApneaRepository` y `TablaPersonalizadaRepository` lo leen de la sesión en cada operación (`ProveedorUsuario`), y eso está bien.
   - `PerfilRepository` y los ViewModels de Cuenta y Perfil reciben el `nombreUsuario` como foto tomada al crear el ViewModel (`CuentaViewModel.kt`: `val usuario = sessionManager.obtenerUsuario()`; además hay 11 llamadas a `obtenerUsuario()` en `Navigation.kt`).

   Es el mismo tipo de bug que el del cronómetro con la otra cuenta: si un ViewModel sobrevive al cambio de sesión, sigue apuntando al usuario anterior.

4. **No hay tests.** Solo están los `Example*Test`. Hay mucha lógica pura que es fácil de testear y crítica: `PoliticaIntentos`, `SeguridadPassword`, `armarEstado`, la validación del formulario, `Mareas`, `PerfilSimulado` y las migraciones. Incluso hay comentarios que dicen "Kotlin puro para poder testearlo", pero el test no existe.

5. **Usuarios de prueba con contraseña en el código y en el APK de release.** Franco y Demo (`Demo1234`) se crean en cualquier instalación (`SeedDatos.kt`, `SeedDemo.kt`, `AppDatabase.kt`). Cualquiera puede entrar a esas cuentas, y la contraseña queda en el historial de git.

### Prioridad media

6. **Si Android mata el proceso, se pierde lo cargado.** Ningún ViewModel usa `SavedStateHandle`. El formulario de inmersión tiene 5 pasos y se pierde entero si Android mata la app mientras el usuario va a Ajustes a dar un permiso o a otra app.

7. **SRP (responsabilidad única).** `NuevaInmersionViewModel` (651 líneas) hace de todo: estado del formulario, parseo, validación, GPS, geocodificación, pedido de condiciones y guardado. Las pantallas también son enormes: `NuevaInmersion.kt` (788), `DetalleInmersion.kt` (668), `Condiciones.kt` (643).

8. **DIP (inversión de dependencias).** `OpenMeteoService`, `CondicionesRepository`, `UbicacionHelper`, `Geocodificador` y `GoogleAuth` son `object` que los ViewModels llaman directamente. No se pueden reemplazar por versiones falsas en tests.

9. **Se traga los errores sin registrarlos.** Hay muchos `runCatching { … }.getOrNull()` sin log. Así pasó con el 403 del clima: la app mostraba "revisá tu conexión" y no había forma de saber qué pasaba. Además, `runCatching` dentro de corrutinas también atrapa la `CancellationException`.

10. **Se guardan en la base textos que se muestran en pantalla.** Los factores de entrenamiento ("Dormí bien", etc.) se guardan en español. Si mañana cambia un texto o se traduce la app, las estadísticas viejas dejan de coincidir. Además, `EstadisticasViewModel` importa esa lista del paquete `ui.entrenamiento`, lo que acopla una pantalla con la otra.

11. **Release sin optimizar.** R8 está apagado (`optimization { enable = false }`) y el APK pesa 53 MB, casi todo por MapLibre. Tampoco hay splits por ABI ni AAB, y `versionCode` sigue en 1.

12. **Borrar una inmersión puede cerrar la app.** `DetalleInmersionViewModel.eliminar()` no maneja errores de la base y le pasa un callback de navegación al ViewModel, cuando lo correcto es avisar con un evento o un estado.

### Prioridad baja

13. **Paquete `data/local` mezclado.** Junta entidades, DAOs y helpers que no son de base de datos: `SonidoManager`, `UbicacionHelper`, `Geocodificador`, `SeguridadPassword`, `TablaApnea`.

14. **Inyección de dependencias manual.** Funciona, pero tiene 19 `Factory` repetidas y un service locator (`MyApplication`) que se lee desde la navegación.

15. **Comprobar y después actualizar, en dos pasos.** `InmersionRepository.actualizar` y `PerfilRepository.guardarEquipo` primero leen y después actualizan. Se puede hacer en una sola consulta con `UPDATE … WHERE id AND nombreUsuario`. El seed de Franco no corre en una transacción, y su marca en SharedPreferences no es atómica con la base.

16. **Modelo de datos.** `SesionApneaEntity` guarda solo la fecha, sin hora ni duración, así que no se pueden ordenar dos sesiones del mismo día. Los `Set<String>` se guardan separados por comas: es intencional y está documentado, pero impide filtrar por SQL.

17. **Rutas de navegación como strings.** Navigation 2.8 ya permite rutas tipadas.

## Plan paso a paso

### Fase 0: red de seguridad (antes de tocar nada)

1. Configurar tests unitarios de JVM con JUnit, `kotlinx-coroutines-test` y Turbine.
2. Escribir tests de la lógica pura que ya existe: `PoliticaIntentos`, `SeguridadPassword`, `armarEstado`, `validar` del formulario, `Mareas` y `PerfilSimulado`.
3. Escribir tests de migraciones con `MigrationTestHelper`, usando los esquemas de `app/schemas`.
4. Agregar un logger único (wrapper de `Log` o Timber) y reemplazar los `runCatching` silenciosos por versiones que logueen y relancen la `CancellationException`.

### Fase 1: problemas que ve el usuario

5. Pasar el entrenamiento a un **foreground service** con notificación. El ViewModel observa su estado; los sonidos y el timer viven en el servicio.
6. **Guardar sin esperar a internet.** Guardar la inmersión al instante y completar las condiciones con **WorkManager** (con restricción de red), que actualiza la fila cuando llegan.
7. Agregar `SavedStateHandle` al formulario de nueva inmersión, al de registro y a la selección de entrenamiento.
8. Manejar errores en `eliminar()` y reemplazar el callback por un estado o evento de una sola vez.

### Fase 2: consistencia y seguridad

9. Unificar el usuario logueado: `PerfilRepository` pasa a usar `ProveedorUsuario` como los demás. Los ViewModels dejan de recibir `nombreUsuario` y se borran las llamadas a `obtenerUsuario()` en `Navigation.kt`. Con eso desaparece toda esa familia de bugs.
10. Seeds solo en debug: mover `SeedDatos` y `SeedDemo` a `src/debug` (o condicionarlos con `BuildConfig.DEBUG`), sacar la contraseña de Franco del código y hacer el seed transaccional.
11. Hacer atómicos los "comprobar y después actualizar" con `UPDATE … WHERE id = ? AND nombreUsuario = ?`.

### Fase 3: diseño (SOLID)

12. Crear interfaces para las dependencias externas (`ProveedorClima`, `ProveedorUbicacion`, `Geocodificador`, `AutenticadorGoogle`) e inyectarlas en los ViewModels, en vez de llamar a los `object`.
13. Partir `NuevaInmersionViewModel`:
    - `ValidadorInmersion`: puro y testeado.
    - `InmersionFormMapper`: pasa del formulario a la entidad y al revés.
    - Caso de uso `RegistrarInmersion`.
    - El ViewModel queda solo con el estado de la pantalla.
14. Pasar los factores de entrenamiento (y cualquier otro texto que se guarde) a `enum` con clave estable, con una migración que convierta los textos viejos. Moverlos al paquete de dominio.
15. Partir las pantallas grandes en composables por sección, cada uno en su archivo.

### Fase 4: infraestructura

16. Reemplazar las 19 factories por **Hilt**, o como mínimo por una interfaz `AppContainer`.
17. Reorganizar paquetes por capa y feature: `data/db`, `data/remote`, `domain`, `ui/<feature>`.
18. Release: activar R8 y `shrinkResources` con reglas de keep, publicar como AAB (o con splits por ABI) y versionar con `versionCode`/`versionName` reales.
19. Agregar Lint/detekt y un workflow de CI que corra los tests en cada push.

Se recomienda empezar por la Fase 0: después del paso 4, cada fase siguiente se puede verificar con tests.
