# servicio-notificaciones

**Componente pendiente: no tiene codigo ni contenedor en esta version.** Consumira el evento `stock-bajo` de `alertas-topic`
(grupo `notificaciones-service-group`) y expondra las alertas para su gestion.

En esta entrega las alertas ya se generan y se persisten: `servicio-inventario` publica
`stock-bajo`, deja el registro en la tabla `alertas_stock` y las expone en
`GET /api/alertas`. Su bloque en `docker-compose.yml` queda comentado.
