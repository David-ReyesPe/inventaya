package com.inventaya.inventario.kafka;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Linea de venta tal como llega en los eventos de ventas-topic.
 *
 * En "venta-realizada" vienen los cuatro campos; en "venta-anulada" solo
 * productoId y cantidad, que es lo unico que Inventario necesita para revertir.
 */
public record ItemEventoPayload(
        UUID productoId,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
}
