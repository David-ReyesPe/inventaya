package com.inventaya.inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ErrorResponse", description = "Respuesta estandar de error de la API")
public record ErrorResponse(

        @Schema(description = "Codigo HTTP del error.", example = "404")
        int codigo,

        @Schema(description = "Explicacion legible del error.", example = "No se encontro el producto solicitado.")
        String mensaje,

        @Schema(description = "Momento en que se genero el error.", example = "2026-08-19T15:32:00Z")
        Instant timestamp
) {

    public static ErrorResponse de(int codigo, String mensaje) {
        return new ErrorResponse(codigo, mensaje, Instant.now());
    }
}
