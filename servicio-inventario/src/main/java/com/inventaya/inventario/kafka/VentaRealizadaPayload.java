package com.inventaya.inventario.kafka;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record VentaRealizadaPayload(
        UUID ventaId,
        List<ItemEventoPayload> items,
        BigDecimal total
) {
}
