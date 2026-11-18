package com.inventaya.ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "VentaItemInput", description = "Linea de venta: un producto con su cantidad y precio")
public record VentaItemInput(

        @Schema(description = "Producto vendido.", example = "b3f1c2a0-1234-4a56-9abc-1234567890ab", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Cada item debe indicar el productoId.")
        UUID productoId,

        @Schema(description = "Unidades vendidas.", example = "3", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Cada item debe indicar la cantidad.")
        @Min(value = 1, message = "La cantidad de cada item debe ser mayor que cero.")
        Integer cantidad,

        @Schema(description = "Precio unitario al momento de la venta.", example = "4500", format = "double", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Cada item debe indicar el precioUnitario.")
        @DecimalMin(value = "0.0", message = "El precio unitario no puede ser negativo.")
        BigDecimal precioUnitario
) {
}
