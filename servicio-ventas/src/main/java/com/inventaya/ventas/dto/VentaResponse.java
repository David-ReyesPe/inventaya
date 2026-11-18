package com.inventaya.ventas.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(name = "Venta", description = "Venta registrada con todas sus lineas")
public record VentaResponse(

        @Schema(description = "Identificador de la venta.", example = "e7a1f9d0-9c2b-4a11-8f3e-abc123456789")
        UUID id,

        @ArraySchema(schema = @Schema(implementation = VentaItemResponse.class))
        List<VentaItemResponse> items,

        @Schema(description = "Suma de los subtotales de todos los items de la venta.", example = "21500", format = "double")
        BigDecimal total,

        @Schema(description = "Estado de la venta.", example = "REGISTRADA", allowableValues = {"REGISTRADA", "ANULADA"})
        String estado,

        @Schema(description = "Momento del registro de la venta.", example = "2026-08-19T15:32:00Z")
        Instant fecha
) {
}
