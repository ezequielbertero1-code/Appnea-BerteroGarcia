# Publicación: App Bundle y Baseline Profile

## 1. Por qué un App Bundle (AAB) y no un APK

El APK lleva el código nativo de MapLibre para las cuatro arquitecturas:

| ABI | Tamaño de las `.so` |
|---|---|
| arm64-v8a (casi todos los teléfonos actuales) | 10,5 MB |
| armeabi-v7a (teléfonos viejos de 32 bits) | 7,7 MB |
| x86_64 / x86 (emuladores, Chromebooks) | 10,9 / 10,7 MB |

Con un AAB, Google Play arma un APK por dispositivo con **una sola** arquitectura (y solo la
densidad de pantalla que corresponde): cada usuario descarga unos **30 MB menos**. Play Store
exige AAB para las apps nuevas.

## 2. Generar el AAB firmado

La firma **no** está en el repositorio (ni el keystore ni las contraseñas) y no tiene que estar.

### Desde Android Studio (recomendado)

1. Subir `appnea.versionCode` (y `appnea.versionName` si corresponde) en `gradle.properties`.
   Play rechaza un `versionCode` repetido.
2. *Build → Generate Signed App Bundle or APK… → Android App Bundle*.
3. Elegir el keystore de subida (*upload key*) y la variante `release`.
4. El archivo queda en `app/release/app-release.aab`.

> `./gradlew assembleRelease` borra la carpeta `app/release/`. Si guardás ahí APKs o AABs
> firmados, copialos a otro lado antes.

### Desde la terminal

```bash
./gradlew :app:bundleRelease
# Sin firma configurada genera app/build/outputs/bundle/release/app-release.aab sin firmar.
# Para firmarlo:
jarsigner -keystore ~/ruta/upload-keystore.jks \
  app/build/outputs/bundle/release/app-release.aab upload
```

### Probar el AAB antes de subirlo

Con [bundletool](https://developer.android.com/tools/bundletool):

```bash
bundletool build-apks --bundle=app-release.aab --output=appnea.apks \
  --ks=~/ruta/upload-keystore.jks --ks-key-alias=upload --connected-device
bundletool install-apks --apks=appnea.apks
```

La versión release pasa por R8. Antes de publicar conviene probar en el teléfono: el login,
Google, guardar una inmersión, el mapa, el clima y un entrenamiento. Si algo falla por R8, el
mapping para leer el stack trace queda en
`app/build/outputs/mapping/release/mapping.txt`. Subilo a Play Console junto con el AAB.

## 3. Play App Signing y el login con Google

Con Play App Signing, Google vuelve a firmar la app con **su** clave, así que lo que se instala
desde Play Store tiene **otro SHA-1** que el de tu keystore. En Google Cloud
(*APIs y servicios → Credenciales*) tiene que haber un cliente OAuth **Android** por cada SHA-1:

| Clave | De dónde sale el SHA-1 |
|---|---|
| debug (Android Studio) | `./gradlew :app:signingReport` |
| upload (tu keystore) | `keytool -list -v -keystore upload-keystore.jks` |
| app signing (la de Play) | Play Console → *Integridad de la app* → *Firma de apps* |

El client ID **web** sigue yendo en `local.properties` (`client_web=...`). Si falta el SHA-1 de
Play, "Iniciar sesión con Google" anda en debug pero falla en la versión de la tienda.

## 4. Baseline Profile: evaluación

Un *Baseline Profile* es la lista de clases y métodos que se usan al arrancar y en los flujos
principales. Play la instala junto con la app y ART los compila antes, así que no se
interpretan la primera vez. En apps con Compose suele mejorar el arranque en frío entre un
20 y un 30 % y reducir los tirones de la primera navegación.

**Lo que ya hay:** Compose, Activity y Lifecycle traen sus propios perfiles, y
`androidx.profileinstaller` 1.4.0 ya entra como dependencia de ellos. Esa parte se aprovecha
sin hacer nada. Lo que falta es el perfil del **código de la app**: `MainActivity`, la apertura
de Room, el grafo de navegación y las pantallas de Inicio y de la bitácora.

**Qué haría falta:**

1. Un módulo `:baselineprofile` (plugin `com.android.test` + `androidx.baselineprofile`) con
   una prueba `BaselineProfileRule` que abra la app, inicie sesión con una cuenta de prueba y
   recorra Inicio → Inmersiones → detalle → Entrenar.
2. El plugin `androidx.baselineprofile` en `:app`, que copia el perfil generado a
   `src/release/generated/baselineProfiles/` (ese archivo se commitea).
3. Generarlo con `./gradlew :app:generateBaselineProfile` en un emulador con API 28+ o en un
   teléfono físico con **API 33+** (el Samsung de pruebas sirve, sin root).
4. Medir antes y después con Macrobenchmark (`StartupTimingMetric`, `CompilationMode.None`
   contra `CompilationMode.Partial`).

**Recomendación:** hacerlo **después de la fase 9** (modularización). Suma un módulo, y si
se crea antes hay que tocarlo otra vez al partir `:app`. Conviene medir primero: si el arranque
en frío de la release en un teléfono de gama media ya está por debajo de ~500 ms, la mejora no
justifica mantener un módulo más. Si está por encima, o si la primera vez que se abre la
bitácora o el mapa se nota el tirón, vale la pena.
