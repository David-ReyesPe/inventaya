#!/usr/bin/env bash
# Genera el ZIP de entrega DESDE EL COMMIT ETIQUETADO, para que ZIP y repositorio sean la misma version.
# Uso: ./scripts/generar_zip.sh v1.0.0-entrega-final
set -euo pipefail
TAG=${1:?indique la etiqueta, p. ej. v1.0.0-entrega-final}
git rev-parse --verify "$TAG^{commit}" >/dev/null
git archive --format=zip --prefix=inventaya/ -o "PA_G3_Equipo3_EntregaFinal_${TAG}.zip" "$TAG"
echo "Creado PA_G3_Equipo3_EntregaFinal_${TAG}.zip (commit $(git rev-parse --short "$TAG^{commit}"))"
