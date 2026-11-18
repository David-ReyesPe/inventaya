package com.inventaya.inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Datos editables del producto. El stock queda deliberadamente fuera: se ajusta por
 * PUT /api/productos/{id}/stock o por consumo de eventos de venta, nunca sobreescribiendolo.
 */
@Schema(name = "ProductoUpdateInput", description = "Datos editables de un producto (no incluye stock)")
public record ProductoUpdateInput(

        @Schema(description = "Nombre del producto.", example = "Arroz premium 500g", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre del producto es obligatorio.")
        String nombre,

        @Schema(description = "Precio unitario de venta.", example = "3500", format = "double", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El precio es obligatorio.")
        @DecimalMin(value = "0.0", message = "El precio no puede ser negativo.")
        BigDecimal precio,

        @Schema(description = "Stock por debajo del cual se genera una alerta de stock bajo.", example = "15", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El umbral minimo es obligatorio.")
        @Min(value = 0, message = "El umbral minimo no puede ser negativo.")
        Integer umbralMinimo
) {
}
