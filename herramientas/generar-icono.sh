#!/usr/bin/env bash
# Genera el icono de la app a partir del diseño maestro.
#
#   logo/logo.svg  ->  logo/logo-512.png          (ficha de Play Store)
#                  ->  mipmap-*/ic_launcher.webp  (iconos de densities)
#                  ->  logo/marca-sobre-claro.png (uso en documentos)
#
# Solo hay que ejecutarlo cuando cambie el diseño. El icono adaptativo de
# Android está dibujado a mano en app/src/main/res/drawable/, porque los
# vectores de Android no admiten todo lo que sí admite SVG.

set -euo pipefail

cd "$(dirname "$0")/.."
RAIZ="$(pwd)"

if ! command -v rsvg-convert >/dev/null; then
  echo "Falta rsvg-convert (paquete: librsvg)"
  exit 1
fi

MAESTRO="$RAIZ/logo/logo.svg"
SALIDA="$RAIZ/logo/logo-512.png"

echo "Generando icono maestro..."
rsvg-convert -w 512 -h 512 "$MAESTRO" -o "$SALIDA"

# Densidades de Android para el icono cuadrado clásico.
declare -A DENSIDADES=(
  [mdpi]=48
  [hdpi]=72
  [xhdpi]=96
  [xxhdpi]=144
  [xxxhdpi]=192
)

for densidad in "${!DENSIDADES[@]}"; do
  lado="${DENSIDADES[$densidad]}"
  destino="$RAIZ/app/src/main/res/mipmap-$densidad"
  mkdir -p "$destino"
  rsvg-convert -w "$lado" -h "$lado" "$MAESTRO" -o "$destino/ic_launcher.png"
  magick "$destino/ic_launcher.png" -quality 92 "$destino/ic_launcher.webp"
  magick "$SALIDA" -resize "${lado}x${lado}" "$destino/ic_launcher_round.png"
  magick "$destino/ic_launcher_round.png" -quality 92 "$destino/ic_launcher_round.webp"
  # Los PNG intermedios sobran: el formato de destino es webp.
  rm -f "$destino/ic_launcher.png" "$destino/ic_launcher_round.png"
  echo "  $densidad  ${lado}x${lado}"
done

echo
echo "Listo:"
echo "  logo/logo-512.png  → ficha de Google Play"
echo "  mipmap-*/          → iconos del lanzador"