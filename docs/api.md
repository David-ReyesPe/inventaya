# Documentación de la API

La especificación formal (OpenAPI 3) se **genera desde el código** y queda versionada en `contratos/`:

- `contratos/openapi-servicio-ventas.yaml`
- `contratos/openapi-servicio-inventario.yaml`
- Swagger UI con el stack arriba: http://localhost:8081/swagger-ui.html y http://localhost:8082/swagger-ui.html
- Contratos tal como se aprobaron en la Entrega 2 P1: `contratos/entrega-2-p1-aprobados/`

GraphQL: `contratos/schema.graphqls` es **solo un contrato**; no hay gateway implementado (ver `docs/arquitectura.md`).

## Autenticación

Todos los endpoints de negocio exigen `Authorization: Bearer <JWT>`. Con el perfil `dev`:

`GET /api/dev/token?role=ADMIN|USER[&subject=nombre]` (en cualquiera de los dos servicios; clave compartida).

## servicio-ventas (:8081)

| Método y ruta | Rol | Respuestas |
|---|---|---|
| `POST /api/ventas` | ADMIN, USER | 201, 400 (sin items, cantidad 0, producto inexistente), 401, 409 (stock insuficiente) |
| `GET /api/ventas?desde&hasta&page&size` | ADMIN, USER | 200 (paginado), 400 (rango invertido), 401 |
| `GET /api/ventas/{id}` | ADMIN, USER | 200, 401, 404 |
| `PATCH /api/ventas/{id}/anular` (`{"motivo": "..."}`) | ADMIN | 202, 400, 401, 403, 404, 409 (ya anulada) |

## servicio-inventario (:8082)

| Método y ruta | Rol | Respuestas |
|---|---|---|
| `POST /api/productos` | ADMIN | 201, 400, 401, 403 |
| `GET /api/productos?page&size` | ADMIN, USER | 200 (paginado), 400 (size inválido), 401 |
| `GET /api/productos/{id}` | ADMIN, USER | 200, 401, 404 |
| `PUT /api/productos/{id}` | ADMIN | 200, 400, 401, 403, 404 |
| `PUT /api/productos/{id}/stock` (`{"operacion":"INCREMENTO\|DECREMENTO","cantidad":n}`) | ADMIN | 200, 400, 401, 403, 404, 409 |
| `GET /api/alertas?atendida&page&size` | ADMIN, USER | 200 (paginado), 400, 401 |

Errores: siempre `{"codigo": 409, "mensaje": "...", "timestamp": "..."}`.
Paginación: cuerpo `PaginaResponse` + cabeceras `X-Total-Count`, `X-Total-Pages`, `X-Page`, `X-Page-Size`, `X-Has-Next`, `X-Has-Previous`.

Ejemplos `curl` completos: ver `README.md` (sección “Ejemplos curl”).

## Eventos Kafka

Definición completa en `kafka/kafka-eventos.md`.
