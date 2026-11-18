package com.inventaya.ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "AnulacionInput", description = "Motivo por el cual se anula la venta")
public record AnulacionInput(

        @Schema(description = "Explicacion de por que se anula la venta.",
                example = "Cantidad registrada por error, se corrige con una nueva venta.",
                minLength = 5,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Debe indicar el motivo de la anulacion.")
        @Size(min = 5, message = "El motivo de anulacion debe tener al menos 5 caracteres.")
        String motivo
) {
}
