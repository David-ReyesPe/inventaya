package com.inventaya.ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TokenResponse", description = "Token JWT de prueba emitido por el perfil dev")
public record TokenResponse(

        @Schema(description = "JWT firmado con la clave simetrica de configuracion.")
        String token,

        @Schema(description = "Esquema a usar en el encabezado Authorization.", example = "Bearer")
        String tipo,

        @Schema(description = "Rol embebido en el claim 'role'.", example = "ADMIN")
        String rol,

        @Schema(description = "Vigencia del token en segundos.", example = "7200")
        long expiraEnSegundos
) {
}
