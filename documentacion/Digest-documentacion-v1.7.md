# Digest

## Diario de medicación, comidas y síntomas

**Documentación funcional · Versión 1.7**

Aplicación Android para personas con enfermedad inflamatoria intestinal (EII) o colon irritable.

*Actualizado el 19 de octubre de 2026.*

---

## 1. Qué es Digest

Digest es una aplicación Android que ayuda al paciente a registrar, en un mismo sitio, lo que le pasa cada día: la medicación que toma, las comidas que hace, los síntomas que nota y cómo evoluciona. Con esos datos la app construye un historial ordenado y un informe que el paciente puede enviar a su médico.

La app no diagnostica ni decide un tratamiento. Su objetivo es resolver un problema muy concreto: que el paciente recuerde y pueda contar con precisión lo que ha pasado entre una consulta y la siguiente.

### Principios de diseño

- **Todo local.** Los datos de salud se guardan solo en el teléfono. No hay servidor, no hay cuenta y la app funciona sin conexión.
- **La enfermedad es un dato, no una suposición.** El diagnóstico elegido decide qué zonas se preguntan, qué síntomas se ofrecen y cómo se nombra el control de la enfermedad.
- **Registrar rápido.** Los cuestionarios piden lo mínimo accionable y las comidas anteriores se pueden repetir con un toque.
- **Avisar sin prometer milagros.** Si el paciente no concede el permiso de alarma exacta, la app degrada a una alarma inexacta en lugar de dejar de avisar.
- **Explicar las alertas.** Cada señal clínica dice qué ha detectado y por qué conviene consultar. Una alerta sin motivo se acaba ignorando.

---

## 2. Para quién es

Digest cubre cuatro diagnósticos:

- **Colitis ulcerosa**
- **Enfermedad de Crohn**
- **Colitis indeterminada**
- **Colon irritable** (síndrome del intestino irritable)

Los tres primeros son enfermedades inflamatorias intestinales y comparten buena parte del seguimiento. El colon irritable no es una enfermedad inflamatoria, así que la app adapta el alta, el cuestionario de síntomas y el lenguaje del control diario.

---

## 3. Privacidad y funcionamiento

- Los datos se guardan en la base de datos privada de la app, en el propio dispositivo.
- No se envía información a ningún servidor.
- No hay registro, cuenta de usuario ni sincronización en la nube.
- La app funciona sin conexión a internet.
- Los informes y documentos solo salen del teléfono cuando el paciente decide compartirlos.
- Si se desinstala la app o se cambia de móvil, los datos se pierden: todavía no existe copia de seguridad.

---

## 4. Primer arranque

La primera vez que se abre la app aparece una pantalla de alta con estos pasos:

1. **Elegir el diagnóstico** entre colitis ulcerosa, enfermedad de Crohn, colitis indeterminada o colon irritable.
2. **Añadir datos opcionales:** año del diagnóstico, peso y altura.
3. **Completar el perfil según el diagnóstico:**
   - En las enfermedades inflamatorias: zonas afectadas y antecedentes quirúrgicos (ostomía o resección intestinal).
   - En colon irritable: estos apartados no aparecen, porque no forman parte de la enfermedad.
4. **Pulsar “Empezar”**. El perfil se guarda y la app abre la pantalla de Hoy.

El diagnóstico determina desde ese momento:

- Qué zonas del tubo digestivo se ofrecen.
- Qué síntomas se preguntan a diario.
- Si el bloque de control se llama “Estado del brote” o “Control de los síntomas”.
- Cómo se redactan algunas alertas y el informe médico.

---

## 5. Pantallas de la app

### 5.1 Hoy

Es la pantalla de entrada. Reúne lo que el paciente necesita en el día:

- **Fecha y diagnóstico** del perfil.
- **Aviso de permisos** si faltan notificaciones o alarmas exactas.
- **Tomas de hoy:** medicación pendiente, tomada u omitida. Si no hay medicación, explica cómo añadirla.
- **Comidas de hoy:** resumen de lo registrado y acceso para añadir.
- **¿Cómo estás hoy?:** cuestionario de síntomas adaptado al diagnóstico. Cada síntoma se marca con una intensidad de 1 a 5; al volver a pulsarlo sube la intensidad y, en el máximo, se quita.
- **Control de la comida:** si han pasado dos horas desde una comida, pregunta si ha notado algún cambio.
- **Compartir mi historia con el médico:** genera el resumen y lo envía junto con los informes adjuntos.

Los síntomas que se muestran cambian según el diagnóstico:

- **Colitis ulcerosa:** diarrea, dolor abdominal, tenesmo, sangre en heces, mucosidad, distensión, fatiga, fiebre, náuseas, pérdida de apetito, pérdida de peso, desnutrición, aftas, dolor articular, manchas en la piel y ojo rojo.
- **Crohn y colitis indeterminada:** los mismos síntomas y, además, manifestaciones perianales (fisura, fístula, absceso, dolor o supuración, inflamación externa).
- **Colon irritable:** diarrea, dolor abdominal, mucosidad, distensión, fatiga y náuseas. Se excluyen de forma deliberada la sangre, la fiebre, la pérdida de peso y las manifestaciones extraintestinales, porque no forman parte del cuadro y obligarían a descartar otra causa.

### 5.2 Medicación

Permite crear y mantener la agenda de fármacos y suplementos:

- Nombre, dosis, presentación e instrucciones de uso (por ejemplo, “con comida” o “en ayunas”).
- Pauta con varios horarios al día y días concretos de la semana.
- Cada medicación se muestra con sus horarios para ver de un vistazo qué toca.

Los avisos de toma funcionan con `AlarmManager`:

- A la hora programada salta una notificación con acciones directas **Tomada** y **Omitir**.
- Si la toma sigue pendiente, 20 minutos después llega un aviso de refuerzo.
- En Android 12+ se usa alarma exacta si el paciente concede el permiso; si no, se programa una alarma inexacta para que el aviso llegue igualmente, aunque con unos minutos de retraso.
- Las alarmas se reprograman al abrir la app y después de reiniciar el teléfono, porque Android las borra al apagar.

### 5.3 Comidas

- Registro del tipo de comida y la hora.
- Anotación de lo que se ha comido, no solo la categoría.
- Las comidas anteriores se ofrecen para repetirlas con un toque.
- Dos horas después, la app pregunta si ha habido algún cambio. La respuesta se cruza con la comida para construir la relación entre alimentos y síntomas.

### 5.4 Diario

El diario combina el registro del cuerpo con el estado general del día:

- **Registro del cuerpo:**
  - Deposiciones con escala de Bristol y cantidad de sangre.
  - Episodios de dolor abdominal con intensidad, tipo y localización.
  - Volumen de nutrición enteral.
- **Resumen de hoy:** lista de síntomas marcados con su intensidad; se puede ajustar desde aquí.
- **¿La medicación te está mejorando?** Sin mejoras, poca mejora, mejora clara, gran mejora, empeora o no aplica.
- **¿Cómo te sientes en general?** Muy bien, bien, regular, mal o muy mal.
- **Control de la enfermedad:**
  - En colitis, Crohn e indeterminada: **“Estado del brote”** con En remisión, Brote leve, Brote moderado, Brote severo o No me aplica.
  - En colon irritable: **“Control de los síntomas”** con Sin molestias, Molestias leves, Molestias moderadas, Molestias intensas o No me aplica.
- **Hidratación:** normal, insuficiente o excesiva.
- **Notas del día.**
- **Generación del informe** en texto o CSV para periodos de 7, 15 o 30 días.

### 5.5 Perfil

Desde el perfil se consulta y completa la ficha del paciente:

- Diagnóstico y zonas afectadas.
- Peso, altura e IMC.
- Intervalo habitual entre consultas (por defecto, 6 meses).
- Antecedentes quirúrgicos y ostomías.
- Historial de cirugías.
- Documentos e informes médicos.
- Directorio de profesionales.

### 5.6 Documentos e informes médicos

El paciente puede adjuntar análisis, colonoscopias, biopsias, pruebas de imagen y otros informes:

- El archivo se **copia dentro de la carpeta privada de la app**, no se guarda como una simple referencia. Así el informe sigue disponible aunque el paciente borre el original de Descargas.
- Cada documento guarda nombre, tipo, fecha, notas, tamaño y formato.
- Se puede abrir, compartir o borrar.
- Se pueden compartir solo los archivos seleccionados o todos a la vez.
- La fecha de la última prueba de cada tipo se muestra en el perfil.

### 5.7 Directorio de profesionales

Incluye gastroenterología y nutrición, y prioriza a los profesionales especializados en enfermedad inflamatoria intestinal. Los datos del directorio son ficticios y están incluidos a modo de ejemplo: no corresponden a personas ni centros reales.

---

## 6. Alertas clínicas

Las alertas reconocen patrones que en la práctica aconsejan consultar. **No son un diagnóstico.** Cada una explica qué se ha detectado y por qué.

Hay tres niveles:

- **Normal:** no hace falta adelantar la consulta.
- **Pronto:** conviene pedir cita en los próximos días.
- **Urgente:** hay que contactar hoy con el centro de salud.

Reglas actuales:

| Señal | Cuándo salta | Nivel |
|---|---|---|
| Sangrado abundante | Sangre abundante en heces registrada hoy | Urgente |
| Fiebre alta | Fiebre marcada con intensidad 4 o más sobre 5 | Urgente |
| Dolor abdominal intenso | Dolor marcado con intensidad 4 o más sobre 5 | Urgente |
| Posible absceso o fístula perianal | Manifestación perianal nueva o absceso moderado en Crohn o indeterminada | Urgente |
| Muchas deposiciones diarreicas | 8 o más deposiciones diarreicas en el día | Urgente |
| Síntoma que se mantiene | Un síntoma lleva 14 días seguidos o más | Pronto |
| Pérdida de peso relevante | Más del 5 % del peso en tres meses | Pronto |
| Varios síntomas a la vez | 3 o más síntomas con uno moderado, 5 o más, o sensación general mala | Pronto |
| Toca revisión | Ha pasado el 75 % del intervalo habitual entre consultas | Pronto |

En colon irritable se mantienen las alertas que siguen teniendo sentido, pero se adapta su explicación: la sangre, la fiebre y la pérdida de peso no son habituales en esta enfermedad y por eso obligan a descartar otra causa.

---

## 7. Informe para el médico

El informe se genera desde el Diario y puede compartirse en dos formatos:

### Informe de texto

Incluye, cuando hay datos:

- **Paciente:** diagnóstico, zonas afectadas, ostomía, resección ileocecal y última consulta.
- **Medicación:** tomas programadas, tomadas, omitidas, pendientes y adherencia por fármaco.
- **Síntomas:** frecuencia, media de intensidad y detalle día a día.
- **Deposiciones:** número, media de Bristol, sangre y días con diarrea.
- **Dolor abdominal:** episodios, intensidad, tipo y localización.
- **Nutrición enteral:** producto, volumen y tipo.
- **Comidas y reacción:** qué se comió y si hubo cambios después.
- **Peso:** evolución y variación en el periodo.
- **Diario:** mejora con la medicación, sensación general, control de la enfermedad e hidratación.
- **Informes médicos adjuntos:** archivos incluidos.
- **Señales de alerta registradas** con su nivel y motivo.

El informe termina con una advertencia: lo genera el paciente a partir de sus anotaciones y no es una valoración médica.

### CSV de síntomas

Una fila por día y síntoma, con columnas:

`fecha; síntoma; intensidad; deposiciones; sangre; bristol`

Sirve para abrir los datos en una hoja de cálculo y revisar tendencias.

### Compartir mi historia

El botón **“Compartir mi historia con el médico”** prepara en un solo envío:

- El resumen escrito del periodo elegido (7, 15 o 30 días).
- Todos los informes adjuntos.

Si no hay documentos adjuntos, se envía solo el resumen y la app lo indica antes de compartir.

---

## 8. Diferencias según el diagnóstico

| | Colitis ulcerosa | Enfermedad de Crohn | Colitis indeterminada | Colon irritable |
|---|---|---|---|---|
| Tipo | Inflamatoria | Inflamatoria | Inflamatoria | No inflamatoria |
| Localización | Colon y recto | Cualquier parte del tubo digestivo | No está claro si es colitis o Crohn | Intestino, sin inflamación |
| Zonas en el alta | Proctitis, colitis izquierda, colitis extensa, pancolitis | Ileítis, ileocolitis, colitis de Crohn, gastroduodenal, perianal | Todas las anteriores | No se preguntan |
| Síntomas perianales | No | Sí | Sí | No |
| Sangre, fiebre y pérdida de peso en el cuestionario | Sí | Sí | Sí | No |
| Bloque de control | Estado del brote | Estado del brote | Estado del brote | Control de los síntomas |

---

## 9. Novedades de la versión 1.7

- **Colon irritable en el alta inicial**, con su propia descripción.
- **Alta adaptada:** al elegir colon irritable no se preguntan zonas afectadas ni antecedentes quirúrgicos.
- **Cuestionario de síntomas propio:** se excluyen los signos de alarma que no pertenecen al cuadro y se mantienen los síntomas habituales.
- **Control de los síntomas:** el Diario y el informe usan “Sin molestias / Molestias leves / Moderadas / Intensas” en lugar de “brote” y “remisión”.
- **Alertas redactadas para colon irritable** cuando la señal implica descartar otra causa.
- **Corrección de estado:** al cambiar de diagnóstico ya no se arrastra la marca de ostomía de una selección anterior.
- **Informe más completo:** las anotaciones que solo contienen estado del brote o hidratación ya no se pierden; antes podían aparecer como “Sin anotaciones”.

---

## 10. Ficha técnica

| | |
|---|---|
| Nombre | Digest |
| Paquete | `com.digest.app` |
| Versión | 1.7 (`versionCode 7`) |
| Plataforma | Android 8.0 o superior (`minSdk 26`) |
| SDK de compilación y objetivo | 36 |
| Lenguaje | Kotlin 2.3.20 |
| Interfaz | Jetpack Compose + Material 3 |
| Arquitectura | MVVM con `StateFlow` |
| Persistencia | Room 2.8.5, 15 tablas, base de datos versión 4 |
| Avisos | `AlarmManager` + `NotificationManager` |
| Inyección de dependencias | Manual, sin Hilt |
| Sistema de compilación | AGP 9.0.1 |
| Tamaño del APK | 1,8 MB |
| Firma | `CN=Digest, OU=Salud Digestiva, O=Digest, L=Madrid, C=ES` |
| SHA-256 del APK 1.7 | `46066b32485859e40ea10dbb652d3d7d18b2a4036f880e973cca8f063849636c` |

---

## 11. Instalación

El APK firmado está en:

`Digest-v1.7.apk`

Pasos para instalarlo en un móvil:

1. Pasar el archivo al teléfono (cable, tarjeta SD, Drive, correo…).
2. Abrirlo desde el explorador de archivos.
3. Conceder el permiso de **“instalar apps de orígenes desconocidos”** si Android lo pide.
4. Confirmar en **Play Protect** que se confía en el archivo. Es habitual en cualquier APK que no venga de la tienda.

El APK está firmado con la misma clave que las versiones anteriores, así que se puede instalar encima de la 1.6 sin desinstalar y conservando los datos.

---

## 12. Límites y seguridad

- **No es un dispositivo médico ni sustituye al médico.** No diagnostica, no ajusta tratamientos y no interpreta los datos.
- **Las alertas son orientativas.** Reconocen patrones, pero no conocen el contexto completo del paciente.
- **Los avisos dependen de Android.** Si se deniegan permisos, se activa el ahorro extremo de batería o el fabricante restringe las alarmas, un aviso puede retrasarse o no llegar. La app degrada a alarmas inexactas cuando no puede usar exactas.
- **Todo está en el teléfono.** No hay copia de seguridad en la nube. Si se pierde o se cambia el dispositivo, o se desinstala la app, los datos no se pueden recuperar.
- **El informe lo genera el paciente.** Es un resumen de lo anotado, no una historia clínica oficial.

---

## 13. Estado del proyecto

Funciona de extremo a extremo: alta del paciente, medicación con horarios, avisos, registro de tomas desde la notificación, comidas con control posterior, síntomas, deposiciones, dolor, nutrición enteral, documentos, señales de alarma e informe exportable.

Pendiente:

- Publicación en Google Play Store (requiere cuenta de desarrollador y política de privacidad).
- Copia de seguridad y restauración de datos.
- Cuestionario de calidad de vida validado: aparcado por la licencia del SIBDQ.

---

## 14. Glosario

- **EII:** enfermedad inflamatoria intestinal. Agrupa la colitis ulcerosa, la enfermedad de Crohn y la colitis indeterminada.
- **Colitis ulcerosa:** inflamación continua del colon y el recto.
- **Enfermedad de Crohn:** inflamación que puede afectar a cualquier parte del tubo digestivo, con frecuencia de forma discontinua.
- **Colitis indeterminada:** inflamación del colon que no encaja con claridad en colitis ulcerosa ni en Crohn.
- **Colon irritable:** trastorno funcional del intestino, sin inflamación demostrable, con dolor o molestia y cambios en el tránsito.
- **Brote:** periodo de actividad de una enfermedad inflamatoria intestinal.
- **Remisión:** periodo sin actividad inflamatoria relevante.
- **Escala de Bristol:** clasificación de la consistencia de las heces en siete tipos.
- **Tenesmo:** sensación de necesitar ir al baño sin conseguir evacuar.
- **Nutrición enteral:** alimentación administrada por sonda o por vía digestiva.
- **Ostomía:** salida artificial del intestino a través de la pared abdominal.
- **Resección ileocecal:** extirpación quirúrgica de la parte final del íleon y del inicio del colon.
- **FODMAP:** conjunto de hidratos de carbono fermentables que algunas personas con colon irritable toleran peor.

---

*Documento funcional de Digest · Versión 1.7 · Octubre de 2026*
