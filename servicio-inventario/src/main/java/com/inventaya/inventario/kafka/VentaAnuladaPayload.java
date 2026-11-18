package com.inventaya.inventario.kafka;

import java.util.List;
import java.util.UUID;

public record VentaAnuladaPayload(
        UUID ventaId,
        String motivo,
        List<ItemEventoPayload> items
) {
}
