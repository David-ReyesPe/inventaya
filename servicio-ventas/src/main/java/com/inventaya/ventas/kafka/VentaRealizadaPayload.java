package com.inventaya.ventas.kafka;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record VentaRealizadaPayload(
        UUID ventaId,
        List<ItemVendidoPayload> items,
        BigDecimal total
) {
}
