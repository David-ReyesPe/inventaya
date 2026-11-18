#!/usr/bin/env bash
# Equivalente en bash de insertar_datos_prueba.ps1: crea productos y ventas ficticias por la API,
# de modo que se generen los eventos Kafka y se descuente el stock (permite probar paginacion).
# Requisitos: stack arriba, curl y python3. Uso: ./scripts/insertar_datos_prueba.sh
set -euo pipefail
V=${VENTAS_URL:-http://localhost:8081}
I=${INVENTARIO_URL:-http://localhost:8082}
json() { python3 -c "import sys,json;print(json.load(sys.stdin)$1)"; }

TOKEN=$(curl -fsS "$V/api/dev/token?role=ADMIN" | json "['token']")
H=(-H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json")

NOMBRES=("Aceite" "Leche" "Arroz" "Azucar" "Cafe" "Sal" "Harina" "Pasta" "Galletas" "Jabon" \
         "Atun" "Lentejas" "Frijol" "Panela" "Chocolate" "Avena" "Jugo" "Agua" "Cereal" "Mantequilla")
IDS=(); PRECIOS=()
for n in "${!NOMBRES[@]}"; do
  precio=$(( 2000 + (n * 750) ))
  r=$(curl -fsS -X POST "$I/api/productos" "${H[@]}" \
      -d "{\"nombre\":\"${NOMBRES[$n]} (prueba)\",\"precio\":$precio,\"stockInicial\":$(( 40 + n * 5 )),\"umbralMinimo\":10}")
  IDS+=("$(echo "$r" | json "['id']")"); PRECIOS+=("$precio")
done
echo "Productos creados: ${#IDS[@]}"

for k in $(seq 0 19); do
  a=$(( k % ${#IDS[@]} )); b=$(( (k + 3) % ${#IDS[@]} ))
  curl -fsS -o /dev/null -X POST "$V/api/ventas" "${H[@]}" -d "{\"items\":[
    {\"productoId\":\"${IDS[$a]}\",\"cantidad\":$(( 1 + k % 4 )),\"precioUnitario\":${PRECIOS[$a]}},
    {\"productoId\":\"${IDS[$b]}\",\"cantidad\":1,\"precioUnitario\":${PRECIOS[$b]}}]}"
done
echo "Ventas creadas: 20. Revise http://localhost:8083 (paginacion en Ventas e Inventario)."
