package com.inventaya.ventas.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(name = "VentaInput", description = "Venta a registrar, con una o varias lineas de producto")
public record VentaInput(

        @ArraySchema(minItems = 1, schema = @Schema(implementation = VentaItemInput.class))
        @NotEmpty(message = "La venta debe incluir al menos un producto.")
        @Valid
        List<VentaItemInput> items
) {
}
