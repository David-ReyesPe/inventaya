package com.inventaya.ventas.controller;

import com.inventaya.ventas.config.JwtService;
import com.inventaya.ventas.config.RolUsuario;
import com.inventaya.ventas.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ayuda de desarrollo para obtener un JWT valido sin montar login ni usuarios.
 * Solo existe con el perfil "dev" activo; en cualquier otro perfil la ruta devuelve 404.
 * Como todos los servicios comparten la misma clave simetrica, el token que emite este
 * endpoint tambien sirve contra servicio-inventario.
 */
@RestController
@RequestMapping("/api/dev")
@Profile("dev")
@Tag(name = "Desarrollo", description = "Utilidades disponibles solo con el perfil dev")
public class DevTokenController {

    private final JwtService jwtService;

    public DevTokenController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/token")
    @Operation(summary = "Emitir un token JWT de prueba (solo perfil dev)")
    public TokenResponse emitir(
            @Parameter(description = "Rol que llevara el token en el claim 'role'.")
            @RequestParam(defaultValue = "ADMIN") RolUsuario role,
            @Parameter(description = "Identificador del usuario que se escribe en el subject.")
            @RequestParam(defaultValue = "demo") String subject) {

        return new TokenResponse(
                jwtService.generarToken(subject, role),
                "Bearer",
                role.name(),
                jwtService.segundosDeVigencia());
    }
}
