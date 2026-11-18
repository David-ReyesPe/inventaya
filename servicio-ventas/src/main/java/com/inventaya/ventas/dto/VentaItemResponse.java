package com.inventaya.ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "VentaItem", description = "Linea de una venta registrada")
public record VentaItemResponse(

        @Schema(description = "Producto vendido.", example = "b3f1c2a0-1234-4a56-9abc-1234567890ab")
        UUID productoId,

        @Schema(description = "Unidades vendidas.", example = "3")
        int cantidad,

        @Schema(description = "Precio unitario aplicado.", example = "4500", format = "double")
        BigDecimal precioUnitario,

        @Schema(description = "cantidad x precioUnitario.", example = "13500", format = "double")
        BigDecimal subtotal
) {
}
