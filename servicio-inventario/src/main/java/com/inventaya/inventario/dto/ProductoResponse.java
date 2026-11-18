package com.inventaya.inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "Producto", description = "Producto del catalogo")
public record ProductoResponse(

        @Schema(description = "Identificador del producto.", example = "c1d2e3f4-5678-90ab-cdef-1234567890ab")
        UUID id,

        @Schema(description = "Nombre del producto.", example = "Arroz 500g")
        String nombre,

        @Schema(description = "Precio unitario de venta.", example = "3200", format = "double")
        BigDecimal precio,

        @Schema(description = "Unidades disponibles.", example = "50")
        int stock,

        @Schema(description = "Stock por debajo del cual se genera una alerta.", example = "10")
        int umbralMinimo
) {
}
