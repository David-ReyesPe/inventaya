package com.inventaya.inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(name = "ProductoInput", description = "Datos para registrar un producto nuevo en el catalogo")
public record ProductoInput(

        @Schema(description = "Nombre del producto.", example = "Arroz 500g", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre del producto es obligatorio.")
        String nombre,

        @Schema(description = "Precio unitario de venta.", example = "3200", format = "double", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El precio es obligatorio.")
        @DecimalMin(value = "0.0", message = "El precio no puede ser negativo.")
        BigDecimal precio,

        @Schema(description = "Stock con el que se da de alta el producto.", example = "50", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El stock inicial es obligatorio.")
        @Min(value = 0, message = "El stock inicial no puede ser negativo.")
        Integer stockInicial,

        @Schema(description = "Stock por debajo del cual se genera una alerta de stock bajo.", example = "10", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El umbral minimo es obligatorio.")
        @Min(value = 0, message = "El umbral minimo no puede ser negativo.")
        Integer umbralMinimo
) {
}
