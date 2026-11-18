package com.inventaya.inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(name = "StockUpdateInput", description = "Ajuste manual de stock de un producto")
public record StockUpdateInput(

        @Schema(description = "Tipo de ajuste a aplicar.", example = "INCREMENTO", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "La operacion es obligatoria y debe ser INCREMENTO o DECREMENTO.")
        OperacionStock operacion,

        @Schema(description = "Unidades a sumar o restar.", example = "30", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "La cantidad es obligatoria.")
        @Min(value = 1, message = "La cantidad debe ser mayor que cero.")
        Integer cantidad
) {
}
