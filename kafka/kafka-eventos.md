# InventaYa — Definición de Tópicos y Eventos Kafka

## 1. Tópicos

| Tópico | Particiones (inicial) | Key | Productor(es) | Consumidor(es) |
|---|---|---|---|---|
| `ventas-topic` | 3 | `ventaId` | Servicio de Ventas | Servicio de Inventario (grupo `inventario-service-group`) |
| `alertas-topic` | 1 | `productoId` | Servicio de Inventario | Servicio de Notificaciones (grupo `notificaciones-service-group`) |

Se usa `ventaId` como key en `ventas-topic` para garantizar que todos los
eventos de una misma venta (registro y eventual anulación) queden en la
misma partición y se procesen en orden.

## 2. Estructura de eventos

Todos los eventos comparten un sobre común (envelope) con metadatos, y un
campo `payload` específico según el tipo de evento.

```json
{
  "eventoId": "uuid",
  "tipo": "venta-realizada | venta-anulada | stock-bajo",
  "fecha": "2026-08-19T15:32:00Z",
  "payload": { }
}
```

### 2.1 Evento `venta-realizada`

Publicado por el Servicio de Ventas al registrar una venta nueva (uno o
varios productos) recibida por REST desde el POS. Consumido por el
Servicio de Inventario, que descuenta el stock de cada item.

```json
{
  "eventoId": "8f3a1b2c-0000-4a11-9c22-abc123456789",
  "tipo": "venta-realizada",
  "fecha": "2026-08-19T15:32:00Z",
  "payload": {
    "ventaId": "e7a1f9d0-9c2b-4a11-8f3e-abc123456789",
    "items": [
      { "productoId": "b3f1c2a0-1234-4a56-9abc-1234567890ab", "cantidad": 3, "precioUnitario": 4500, "subtotal": 13500 },
      { "productoId": "a9d8e7f6-4321-4b65-8fed-0987654321cd", "cantidad": 1, "precioUnitario": 8000, "subtotal": 8000 }
    ],
    "total": 21500
  }
}
```

### 2.2 Evento `venta-anulada`

Publicado por el Servicio de Ventas al anular una venta existente (vía
REST, PATCH /api/ventas/{id}/anular, o vía la mutación GraphQL
anularVenta desde el Dashboard). Consumido por el Servicio de
Inventario, que revierte el stock de cada item de la venta anulada.

```json
{
  "eventoId": "1c2d3e4f-1111-4a22-9b33-def987654321",
  "tipo": "venta-anulada",
  "fecha": "2026-08-19T16:05:00Z",
  "payload": {
    "ventaId": "e7a1f9d0-9c2b-4a11-8f3e-abc123456789",
    "motivo": "Cantidad registrada por error, se corrige con una nueva venta.",
    "items": [
      { "productoId": "b3f1c2a0-1234-4a56-9abc-1234567890ab", "cantidad": 3 },
      { "productoId": "a9d8e7f6-4321-4b65-8fed-0987654321cd", "cantidad": 1 }
    ]
  }
}
```

### 2.3 Evento `stock-bajo`

Publicado por el Servicio de Inventario cuando, tras procesar una venta o
un ajuste de stock, el stock resultante de un producto queda por debajo
de su umbral mínimo. Consumido por el Servicio de Notificaciones.

```json
{
  "eventoId": "9a8b7c6d-2222-4a33-9c44-1122334455aa",
  "tipo": "stock-bajo",
  "fecha": "2026-08-19T15:32:05Z",
  "payload": {
    "productoId": "b3f1c2a0-1234-4a56-9abc-1234567890ab",
    "stockActual": 4,
    "umbralMinimo": 10
  }
}
```

## 3. Decisión de diseño: canal de registro de ventas

El registro de una venta (POST /api/ventas) llega directamente al
Servicio de Ventas por REST desde el Punto de Venta (POS), sin pasar por
el Gateway GraphQL. El Gateway GraphQL se usa para el Dashboard
Administrativo (consultas agregadas de inventario, ventas y alertas) y
para las mutaciones administrativas (crearProducto, anularVenta). Esta
decisión se toma porque el POS necesita baja latencia y una integración
simple contra un único servicio, mientras que el valor de GraphQL está
en agregar datos de varios servicios para el dashboard.

## 4. Otras consideraciones de diseño

- **Idempotencia:** cada evento lleva un `eventoId` único. Los consumidores
  deben validar que no hayan procesado ya ese `eventoId` antes de aplicar
  el efecto (descuento/reversión de stock), para evitar duplicados en caso
  de reintentos.
- **Orden de eventos:** al usar `ventaId` como key en `ventas-topic`, Kafka
  garantiza que el evento `venta-realizada` de una venta siempre se procese
  antes que su eventual `venta-anulada`, ya que ambos caen en la misma
  partición.
- **Manejo de fallos:** queda pendiente de definir en la siguiente
  iteración si se implementará una dead-letter queue (ej. `ventas-topic-dlq`)
  para eventos que el consumidor no logre procesar tras varios reintentos.
