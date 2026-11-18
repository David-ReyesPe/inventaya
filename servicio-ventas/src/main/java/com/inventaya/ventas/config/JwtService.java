package com.inventaya.ventas.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Emision y validacion de JWT firmados con la clave simetrica de configuracion.
 * Para esta entrega no hay login ni usuarios persistidos: el token solo transporta
 * un subject informativo y el claim {@code role} (ADMIN | USER).
 */
@Component
public class JwtService {

    private static final String CLAIM_ROL = "role";

    private final SecretKey clave;
    private final String issuer;
    private final Duration expiracion;

    public JwtService(JwtProperties propiedades) {
        if (propiedades.getSecret() == null || propiedades.getSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "inventaya.jwt.secret debe tener al menos 32 caracteres para firmar con HMAC-SHA.");
        }
        this.clave = Keys.hmacShaKeyFor(propiedades.getSecret().getBytes(StandardCharsets.UTF_8));
        this.issuer = propiedades.getIssuer();
        this.expiracion = Duration.ofMinutes(propiedades.getExpiracionMinutos());
    }

    public String generarToken(String subject, RolUsuario rol) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .claim(CLAIM_ROL, rol.name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracion)))
                .signWith(clave)
                .compact();
    }

    /**
     * @throws JwtException si la firma no coincide, el token expiro o el claim role no es valido.
     */
    public UsuarioAutenticado validarToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String rolBruto = claims.get(CLAIM_ROL, String.class);
        if (rolBruto == null) {
            throw new JwtException("El token no incluye el claim 'role'.");
        }
        try {
            return new UsuarioAutenticado(claims.getSubject(), RolUsuario.valueOf(rolBruto.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new JwtException("El claim 'role' no corresponde a un rol conocido: " + rolBruto);
        }
    }

    public long segundosDeVigencia() {
        return expiracion.toSeconds();
    }
}
