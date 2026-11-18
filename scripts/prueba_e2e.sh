#!/usr/bin/env bash
# Prueba de humo end-to-end sobre el stack levantado con Docker Compose.
# Genera la evidencia MEDIDA de los casos E2E-xx de docs/pruebas/matriz-pruebas.md.
# Uso:  ./scripts/prueba_e2e.sh | tee docs/evidencias/prueba_e2e.log
# Requisitos: stack arriba y sano (docker compose ps), curl y python3. Sale con codigo 1 si algun caso falla.
set -uo pipefail
V=${VENTAS_URL:-http://localhost:8081}; I=${INVENTARIO_URL:-http://localhost:8082}; W=${WEB_URL:-http://localhost:8083}
PASS=0; FAIL=0
ok()   { echo "[PASS] $1 - $2"; PASS=$((PASS+1)); }
bad()  { echo "[FAIL] $1 - $2 (obtenido: $3)"; FAIL=$((FAIL+1)); }
chk()  { [ "$3" = "$4" ] && ok "$1" "$2 -> $4" || bad "$1" "$2" "$3 (esperado $4)"; }
code() { curl -s -o /dev/null -w '%{http_code}' "$@"; }
jq_()  { python3 -c "import sys,json;d=json.load(sys.stdin);print($1)"; }

echo "== Prueba E2E InventaYa  $(date -u +%Y-%m-%dT%H:%M:%SZ) =="
chk E2E-01 "GET $V/actuator/health" "$(curl -s $V/actuator/health | jq_ "d['status']")" UP
chk E2E-02 "GET $I/actuator/health" "$(curl -s $I/actuator/health | jq_ "d['status']")" UP

ADMIN=$(curl -s "$V/api/dev/token?role=ADMIN" | jq_ "d['token']")
USER_=$(curl -s "$V/api/dev/token?role=USER&subject=cajero1" | jq_ "d['token']")
HA=(-H "Authorization: Bearer $ADMIN" -H "Content-Type: application/json")
HU=(-H "Authorization: Bearer $USER_" -H "Content-Type: application/json")

chk E2E-03 "POST /api/ventas sin token" "$(code -X POST $V/api/ventas -H 'Content-Type: application/json' -d '{"items":[]}')" 401
chk E2E-04 "POST /api/productos con rol USER" "$(code -X POST $I/api/productos "${HU[@]}" -d '{"nombre":"X","precio":1,"stockInicial":1,"umbralMinimo":1}')" 403

NOM="E2E $(date +%s)"
P=$(curl -s -X POST $I/api/productos "${HA[@]}" -d "{\"nombre\":\"$NOM\",\"precio\":3200,\"stockInicial\":12,\"umbralMinimo\":10}")
PID=$(echo "$P" | jq_ "d['id']")
[ -n "$PID" ] && ok E2E-05 "POST /api/productos (ADMIN) -> 201, id=$PID" || bad E2E-05 "crear producto" "$P"

R=$(curl -s -w '\n%{http_code}' -X POST $V/api/ventas "${HA[@]}" -d "{\"items\":[{\"productoId\":\"$PID\",\"cantidad\":8,\"precioUnitario\":4500}]}")
chk E2E-06 "POST /api/ventas con 8 unidades" "$(echo "$R" | tail -1)" 201
VID=$(echo "$R" | sed '$d' | jq_ "d['id']")
sleep 5
chk E2E-07 "Kafka venta-realizada: stock 12 -> 4" "$(curl -s $I/api/productos/$PID -H "Authorization: Bearer $ADMIN" | jq_ "d['stock']")" 4
chk E2E-08 "Alerta stock-bajo generada (stockActual=4)" "$(curl -s "$I/api/alertas?atendida=false&size=200" -H "Authorization: Bearer $ADMIN" | jq_ "any(a['productoId']=='$PID' and a['stockActual']==4 for a in (d.get('contenido') or d.get('items')))")" True
chk E2E-09 "Venta sobre el stock disponible" "$(code -X POST $V/api/ventas "${HA[@]}" -d "{\"items\":[{\"productoId\":\"$PID\",\"cantidad\":99999,\"precioUnitario\":4500}]}")" 409
chk E2E-10 "Venta sin items" "$(code -X POST $V/api/ventas "${HA[@]}" -d '{"items":[]}')" 400
chk E2E-11 "Anular con rol USER" "$(code -X PATCH $V/api/ventas/$VID/anular "${HU[@]}" -d '{"motivo":"prueba de permisos"}')" 403
chk E2E-12 "Anular con ADMIN" "$(code -X PATCH $V/api/ventas/$VID/anular "${HA[@]}" -d '{"motivo":"Cantidad registrada por error, se corrige con una nueva venta."}')" 202
sleep 5
chk E2E-13 "Kafka venta-anulada: stock revertido 4 -> 12" "$(curl -s $I/api/productos/$PID -H "Authorization: Bearer $ADMIN" | jq_ "d['stock']")" 12
chk E2E-14 "Anular dos veces la misma venta" "$(code -X PATCH $V/api/ventas/$VID/anular "${HA[@]}" -d '{"motivo":"segunda anulacion"}')" 409
chk E2E-15 "Interfaz web (nginx :8083)" "$(code $W/)" 200
chk E2E-16 "Proxy nginx -> inventario (/api/productos)" "$(code $W/api/productos -H "Authorization: Bearer $ADMIN")" 200
if command -v docker >/dev/null 2>&1; then
  T=$(docker exec inventaya-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list 2>/dev/null | grep -E '^(ventas-topic|alertas-topic)$' | sort | tr '\n' ' ')
  chk E2E-17 "Topicos Kafka existentes" "$(echo $T)" "alertas-topic ventas-topic"
else echo "[SKIP] E2E-17 - docker no disponible en esta terminal"; fi

echo "== Resumen: $PASS PASS / $FAIL FAIL =="
[ "$FAIL" -eq 0 ]
