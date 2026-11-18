package com.inventaya.ventas.kafka;

import java.util.UUID;

/**
 * Linea que viaja en "venta-anulada": para revertir el stock Inventario solo necesita
 * saber que producto y cuantas unidades devolver.
 */
public record ItemRevertidoPayload(
        UUID productoId,
        int cantidad
) {
}
