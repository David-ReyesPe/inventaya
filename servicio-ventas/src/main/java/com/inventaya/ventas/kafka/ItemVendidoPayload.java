package com.inventaya.ventas.kafka;

import java.math.BigDecimal;
import java.util.UUID;

/** Linea completa que viaja en "venta-realizada". */
public record ItemVendidoPayload(
        UUID productoId,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
}
