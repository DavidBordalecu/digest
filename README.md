# Digest

Diario de medicación, comidas y síntomas para pacientes con enfermedad
inflamatoria intestinal (colitis ulcerosa, Crohn y colitis indeterminada).

## Qué hace

- **Avisos de toma de medicación** con Exact Alarm, con acción directa
  *Tomada* / *Omitir* desde la notificación y aviso de refuerzo si la toma
  se pasa de hora.
- **Agenda de medicación** con varios horarios por fármaco y días de la
  semana concretos.
- **Registro de comidas** con tipo y hora, para cruzar lo que se ha comido
  con los síntomas posteriores.
- **Cuestionario de síntomas** adaptado a la enfermedad del paciente.
- **Diario diario**: si la medicación mejora, cómo se siente, estado del
  brote e hidratación.
- **Ficha del paciente** con diagnóstico, localización, peso/IMC e
  historial quirúrgico.

## Decisiones de diseño

**Todo local.** Los datos de salud no salen del dispositivo. No hay
servidor, no hay cuenta y la app funciona sin conexión, lo que importa
cuando el paciente está malo.

**La enfermedad es un dato, no una suposición.** `Condicion` decide qué
zonas del tubo digestivo se ofrecen y qué síntomas se preguntan. Una
ileítis no aparece en una colitis ulcerosa, y las manifestaciones
perianales solo se preguntan en Crohn, donde son frecuentes.

**El avisos nunca se pierden del todo.** Si el paciente no concede el
permiso de alarma exacta (Android 12+), la app degrada a una alarma
inexacta en vez de dejar de avisar. Y los avisos se reprograman tras cada
reinicio, porque Android borra todas las alarmas al apagar.

**Cuestionarios pensados para rellenarse rápido.** Se pregunta lo
mínimo accionable; los detalles (Bristol, dolor localizado,
manifestaciones perianales) están ya en el modelo de datos, listos para
usarse.

## Pila técnica

| | |
|---|---|
| Lenguaje | Kotlin 2.3.20 |
| Interfaz | Jetpack Compose + Material 3 |
| Arquitectura | AGP 9.0.1, MVVM con `StateFlow` |
| Persistencia | Room 2.8.5 (13 tablas) |
| Avisos | `AlarmManager` + `NotificationManager` |
| Inyección | Manual (`DigestApplication`), sin Hilt |
| SDK mínimo | 26 (Android 8.0) |

## Estructura

```
app/src/main/java/com/digest/app/
├── model/          Enums de dominio (condición, síntomas, Bristol, dolor…)
├── data/
│   ├── local/      Base de datos, DAO y conversores
│   └── DigestRepository
├── notificaciones/ Alarmas, avisos y receptores
└── ui/             Pantallas (hoy, medicamentos, comidas, diario, perfil)
```

## Desarrollo

```bash
source ~/tools/android-env.sh     # JDK 21 + Android SDK

./gradlew assembleDebug           # Compila el APK de depuración
./gradlew assembleRelease         # Compila el APK firmado (1,7 MB)
./gradlew installDebug            # Instala en el dispositivo conectado

./herramientas/revisar-codigo.sh  # Detecta caracteres o errores de tecleo
~/tools/emulador.sh iniciar       # Arranca el emulador
```

## Instalar en un móvil sin Play Store

El APK firmado queda en `Digest-v1.0.apk`. Para instalarlo:

1. Pásalo al móvil (cable, tarjeta SD, Drive, correo…)
2. Ábrelo desde el explorador de archivos
3. Android pedirá permiso de **"instalar apps de orígenes desconocidos"**
4. **Play Protect pedirá confirmar** que confías en el archivo: es normal,
   aparece en cualquier APK que no venga de la tienda. Se marca porque no
   hay una cuenta de desarrollador detrás.

El APK ya está firmado, así que se puede actualizar sin desinstalar la
versión anterior.

### Sobre la clave de firma

`digest-firma.jks` es la clave con la que se firma la app. **Si se pierde,
no se puede publicar ninguna actualización de Digest en Google Play**: la
tienda rechaza cualquier APK firmado con una clave distinta.

Está fuera de git a propósito. Conviene copiarla a un lugar seguro (nube
cifrada, por ejemplo) y cambiar la contraseña de `keystore.properties` por
algo más robusto que la de ejemplo.

## Estado

Funciona de extremo a extremo: alta del paciente, alta de medicación con
horario, aviso a la hora exacta y registro de la toma desde la
notificación. Queda por delante el registro detallado de deposiciones
(Bristol), dolor localizado, nutrición enteral y las estadísticas.