# gateway-graphql

Pendiente de la siguiente entrega. Implementara el contrato `contratos/schema.graphqls`
como fachada de agregacion para el Dashboard Administrativo.

Los endpoints REST que necesita ya estan disponibles en esta entrega:

| Operacion GraphQL | Endpoint REST que la respalda |
|---|---|
| `inventario(productoId)` | `GET /api/productos/{id}` (:8082) |
| `productos` | `GET /api/productos` (:8082) |
| `ventasPorRango(desde, hasta)` | `GET /api/ventas?desde=&hasta=` (:8081) |
| `alertasActivas` | `GET /api/alertas?atendida=false` (:8082) |
| `crearProducto(input)` | `POST /api/productos` (:8082) |
| `anularVenta(ventaId, motivo)` | `PATCH /api/ventas/{id}/anular` (:8081) |

Segun la decision ya documentada, el gateway no expone `registrarVenta`: el POS llama
directamente a `POST /api/ventas` por REST. Su bloque en `docker-compose.yml` queda
comentado.
