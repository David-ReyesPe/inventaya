package com.inventaya.inventario.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "inventaya.jwt")
public class JwtProperties {

    /**
     * Clave simetrica compartida por todos los servicios de InventaYa. Debe tener al menos
     * 32 caracteres (256 bits) para firmar con HMAC-SHA.
     */
    private String secret;

    /** Emisor que se escribe y se espera en el token. */
    private String issuer = "inventaya";

    /** Vigencia de los tokens emitidos por el endpoint de desarrollo. */
    private long expiracionMinutos = 120;
}
