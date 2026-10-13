#!/usr/bin/env bash
# Revisa que no se colen caracteres de otros alfabetos en el código.
# Han aparecido varias veces al escribir en bloque, y rompen la compilación
# o, peor, llegan a la interfaz sin que se note.

set -u
RAIZ="$(cd "$(dirname "$0")" && pwd)"
FALLOS=0

buscar() {
  local rango="$1" descripcion="$2" enunciado="$3"
  local hallazgos
  hallazgos=$(grep -rnP "$rango" "$RAIZ/app/src" 2>/dev/null)
  if [ -n "$hallazgos" ]; then
    echo "✗ $descripcion"
    echo "$hallazgos" | head -10
    FALLOS=1
  fi
}

buscar '[\x{4e00}-\x{9fff}\x{3040}-\x{30ff}\x{ac00}-\x{d7af}]' \
  "caracteres chinos/japoneses/coreanos en el código" \
  "C"
buscar '\bfn\s+[a-zA-Z]' "palabras clave mal escritas (fn en vez de fun)" "fn"
buscar '\bfunc\s' "palabras clave mal escritas (func en vez de fun)" "func"
buscar '[一-鿿]' "bloques CJK literales" "CJK"

if [ "$FALLOS" -eq 0 ]; then
  echo "✓ Código limpio: sin caracteres extraños ni errores de tecleo"
fi

exit "$FALLOS"