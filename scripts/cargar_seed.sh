#!/usr/bin/env bash
# Carga db/seed/datos_prueba.sql en la base del contenedor inventaya-postgres.
# Uso (desde la raiz del proyecto, con el stack arriba): ./scripts/cargar_seed.sh
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f .env ] && set -a && . ./.env && set +a
docker exec -i inventaya-postgres psql -U "${POSTGRES_USER:-inventaya}" -d "${POSTGRES_DB:-inventaya_db}" < db/seed/datos_prueba.sql
echo "Seed cargado. Productos en catalogo:"
docker exec inventaya-postgres psql -U "${POSTGRES_USER:-inventaya}" -d "${POSTGRES_DB:-inventaya_db}" -tAc "select count(*) from productos;"
