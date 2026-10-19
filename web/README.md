# Digest · web

Sitio de presentación de **Digest 1.7** con una demo funcional de la app dentro de un móvil simulado. Es HTML, CSS y JavaScript sin dependencias ni paso de compilación, y **autocontenido**: todo lo que necesita está dentro de esta carpeta.

## Cómo abrirlo en local

```bash
cd proyectos/colitis/web
python3 -m http.server 8000
```

Abre `http://localhost:8000/index.html`. También funciona con doble clic sobre `index.html` (`file://`).

## Contenido

- **Sitio**: hero animado, navegación, tema claro/oscuro, pestañas de funciones, tabla de diagnósticos interactiva, nueve reglas de alertas desplegables, generador de informe, buscador de glosario, ficha técnica, instalación y novedades.
- **Demo**: las cinco pantallas de la app (Hoy, Medicación, Comidas, Diario, Perfil) con alta del paciente, registro de tomas, síntomas por toque, comidas con control posterior por aviso simulado, deposiciones con escala de Bristol, dolor, nutrición enteral, documentos y exportación de informe (texto y CSV).

## Archivos

```
web/
├── index.html            estructura de la página y marcado del móvil
├── documentacion/        PDF de la documentación (se enlaza desde la ficha)
└── assets/
    ├── styles.css        estilos (marca y componentes Material 3 del móvil)
    ├── app.js            interacciones del sitio (tema, nav, pestañas, informe, glosario)
    ├── demo-modelo.js    modelo, estado, señales y generación de informe
    ├── demo-ui.js        comunes, alta, Hoy, Medicación, Comidas
    ├── demo-ui2.js       Diario, Perfil, hojas modales, avisos y arranque
    └── logo.svg          favicon
```

## Publicar en GitHub Pages

El repositorio incluye el workflow `.github/workflows/deploy-pages.yml`, que publica el contenido de `web/` como sitio estático.

1. Sube los cambios del sitio y del workflow al repositorio (rama `master`).
2. En GitHub: **Settings → Pages → Source: «GitHub Actions»**.
3. El sitio se publica solo con cada push que toque `web/` (o lanzando el workflow a mano desde la pestaña *Actions*).

La URL queda en `https://davidbordalecu.github.io/digest/`.

## Descarga del APK

Los botones de descarga apuntan a la constante `APK_URL` de `assets/app.js`:

```js
const APK_URL = "https://github.com/DavidBordalecu/digest/releases/download/v1.7/Digest-v1.7.apk";
```

El APK se publica como **archivo adjunto de una release** de GitHub, así no engorda el repositorio (el `.gitignore` ignora los `*.apk`) y la descarga es directa, sin permisos ni cuotas.

Para publicar o actualizar la versión:

```bash
# Con la CLI de GitHub (gh), desde la raíz del proyecto:
gh release create v1.7 Digest-v1.7.apk \
  --title "Digest 1.7" \
  --notes "Colon irritable en el alta, cuestionario propio y control de los síntomas."
```

O desde la web: **Releases → Draft a new release → etiqueta `v1.7` → adjuntar el APK → Publish**.

Si cambias el número de versión, actualiza también `APK_URL` para que apunte al de la nueva release.

## API pública de la demo

Desde la consola del navegador:

```js
DigestDemo.senales()                 // señales de alerta calculadas
DigestDemo.informe("texto", 7)       // informe de los últimos 7 días ("texto" o "csv")
DigestDemo.reiniciar()               // borra el estado y vuelve al alta
```

El estado de la demo se guarda en `localStorage` con la clave `digest-demo-v1` y el tema del sitio en `digest-web-tema`.

## Verificación

Las pruebas de humo usadas durante el desarrollo están en `/tmp/opencode/digest-test/` (`test.js`, `test-web.js` con jsdom y `cdp.js` con Chrome real, incluidos clics con hit-testing). No forman parte del sitio.
