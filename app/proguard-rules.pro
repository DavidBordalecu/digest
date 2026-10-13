# Reglas de R8 para Digest.
#
# AndroidX ya aporta reglas para Room y Compose, así que aquí solo hace
# falta lo que es propio de esta app.

# Los receptores de alarmas y de arranque los llama el sistema por nombre,
# con lo que R8 no puede ver que están en uso.
-keep class com.digest.app.notificaciones.ReceptorAvisoToma { *; }
-keep class com.digest.app.notificaciones.ReceptorAccionToma { *; }
-keep class com.digest.app.notificaciones.ReceptorReinicio { *; }

# Los ViewModel se crean por reflexión desde el contenedor de vistas.
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }