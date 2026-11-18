package com.inventaya.inventario.kafka;

import java.util.UUID;

public record StockBajoPayload(
        UUID productoId,
        int stockActual,
        int umbralMinimo
) {
}
