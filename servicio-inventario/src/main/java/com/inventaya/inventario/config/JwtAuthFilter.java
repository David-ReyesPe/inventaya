package com.inventaya.inventario.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventaya.inventario.dto.ErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Valida el JWT de las rutas protegidas (todas las de negocio) y deja el usuario
 * autenticado como atributo del request para que {@link RolInterceptor} evalue el rol.
 * Responde 401 con el schema ErrorResponse si no hay token o si no es valido.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private static final List<String> RUTAS_PUBLICAS = List.of(
            "/v3/api-docs",
            "/swagger-ui",
            "/actuator",
            "/api/dev/");

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        return RUTAS_PUBLICAS.stream().anyMatch(ruta::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String encabezado = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (encabezado == null || !encabezado.startsWith(PREFIJO_BEARER)) {
            responderNoAutorizado(response, "No se envio un token JWT valido.");
            return;
        }

        try {
            UsuarioAutenticado usuario = jwtService.validarToken(encabezado.substring(PREFIJO_BEARER.length()).trim());
            request.setAttribute(UsuarioAutenticado.ATRIBUTO_REQUEST, usuario);
        } catch (JwtException | IllegalArgumentException ex) {
            responderNoAutorizado(response, "No se envio un token JWT valido.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void responderNoAutorizado(HttpServletResponse response, String mensaje) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ErrorResponse.de(HttpStatus.UNAUTHORIZED.value(), mensaje));
    }
}
