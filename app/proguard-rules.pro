# Reglas de R8 para el build release (minify + shrinkResources).
# Room, WorkManager, Compose, Navigation, MapLibre y coroutines traen sus propias reglas
# ("consumer rules") dentro de cada librería: acá va solo lo que no cubren.

# Credential Manager (login con Google): la implementación de Play Services se carga por
# reflexión. Regla indicada en la documentación de androidx.credentials.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}

# Stack traces legibles en los reportes de errores (sin exponer nombres internos).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
