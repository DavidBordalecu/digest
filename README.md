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
- **Registro de comidas** con lo que se ha comido, no solo el tipo. Las
  comidas anteriores se ofrecen con un toque, porque se repiten.
- **Control posterior a la comida**: dos horas después se pregunta si se
  ha notado algún cambio. Sirve para construir la relación entre lo que se
  ha comido y lo que ha pasado después, que el paciente no consigue apuntar
  por su cuenta.
- **Registro del cuerpo**: deposiciones con la escala de Bristol y la
  cantidad de sangre, episodios de dolor abdominal con su carácter y su
  localización, y volumen de nutrición enteral.
- **Carpeta de informes médicos**: adjuntar análisis, colonoscopias,
  biopsias o pruebas de imagen como archivos. Se copian dentro de la app,
  no se guardan como referencia, y se comparten uno a uno o todos juntos.
- **Señales de alarma** que avisan de lo que no debería esperar, siempre
  con el porqué y sin diagnosticar.
- **Compartir mi historia con el médico** en un solo botón: el resumen
  escrito y todos los informes adjuntos van en el mismo envío, con el
  periodo que elijas (7, 15 o 30 días).
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

## Documentación

- [Documentación funcional v1.7 (PDF)](documentacion/Digest-documentacion-v1.7.pdf)
- [Documentación funcional v1.7 (Markdown editable)](documentacion/Digest-documentacion-v1.7.md)

## Desarrollo

```bash
source ~/tools/android-env.sh     # JDK 21 + Android SDK

./gradlew assembleDebug           # Compila el APK de depuración
./gradlew assembleRelease         # Compila el APK firmado (1,7 MB)
./gradlew installDebug            # Instala en el dispositivo conectado

./herramientas/revisar-codigo.sh  # Detecta caracteres o errores de tecleo
~/tools/emulador.sh iniciar       # Arranca el emulador
```

### Ver el informe sin montar en el móvil

`InformeMuestraTest` no comprueba nada: imprime en la consola el informe
completo con datos de ejemplo de Crohn. Es la forma rápida de revisar cómo
se lee lo que verá el médico, sin instalar nada.

```bash
./gradlew :app:testDebugUnitTest --tests '*InformeMuestraTest*'
# La salida está en app/build/test-results/.../system-out
```

## Instalar en un móvil sin Play Store

El APK firmado queda en `Digest-v1.7.apk`. Para instalarlo:

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
horario, aviso a la hora exacta, registro de la toma desde la
notificación, registro de deposiciones, dolor y nutrición enteral, señales
de alarma e informe exportable.

Queda por delante la publicación en Google Play Store (hace falta una
cuenta de desarrollador y una URL de política de privacidad) y un modo de
copia de seguridad de los datos, que ahora mismo solo existen en el
dispositivo: si se cambia de móvil, se pierden.

### Aparcado: cuestionario de calidad de vida (SIBDQ)

Decidido no hacerlo **por ahora**, por un bloqueo legal, no técnico.
Queda aquíToda la investigación para retomarlo sin repetir el trabajo.

**Lo que se quiere**: un cuestionario de calidad de vida validado, al
entrar por primera vez y **repetido cada dos semanas**. La repetición es
la parte importante: un score aislado envejece mal y deja de significar
nada a las pocas semanas; la tendencia es lo que mira el médico.

**Cuál, si se hace**: el **SIBDQ** (10 ítems, escala 1-7, puntuación
10-70) y no el IBDQ-32 (32 ítems). El SIBDQ se construyó a partir del
IBDQ-32 y está validado contra él (Irvine 1996, *Am J Gastroenterol*
91(8):1571-1578), mide los mismos cuatro dominios —síntomas
intestinales, síntomas sistémicos, función emocional y función social— y
se rellena en menos de un minuto. El IBDQ-32 añade granularidad que aquí
no aporta y una barrera de entrada que, en un primer arranque que ya pide
diagnóstico, año, peso, altura, zonas y antecedentes, abandona a la
gente.

**El bloqueo**: el SIBDQ es ©1989 McMaster University. PhenX y la
literatura clínica indican que reutilizarlo requiere permiso del titular
(McMaster Industry Liaison Office, `milo@mcmaster.ca`), también en una
app de pago.

**Si se retoma**, el orden sensato es:

1. Pedir la licencia antes de escribir nada. Es un correo, no código.
2. Mientras tanto se puede preparar el hueco: tabla en Room, pantalla,
   cálculo del 10-70, curva histórica y su sección en el informe. Sin las
   frases literales: inventarlas de memoria produciría algo que no es el
   SIBDQ y no podría llamarse así.

**Alternativa sin licencias**: 10 ítems propios que cubran los mismos
cuatro dominios, puntuados 1-7. La estructura y el cálculo serían
equivalentes, pero no sería el SIBDQ ni un instrumento validado, y no
debe presentarse como tal.