package com.inventaya.inventario.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_BEARER = "bearerAuth";

    @Bean
    public OpenAPI openApiInventario() {
        return new OpenAPI()
                .openapi("3.0.3")
                .info(new Info()
                        .title("InventaYa - Servicio de Inventario")
                        .version("1.0.0")
                        .description("""
                                API REST para el catalogo de productos y el control de stock.
                                Este servicio tambien consume los eventos "venta-realizada" y
                                "venta-anulada" desde Kafka para mantener el stock actualizado,
                                y publica el evento "stock-bajo" cuando corresponde.

                                ### Codigos de respuesta HTTP usados en esta API

                                | Codigo | Significado | Cuando se usa |
                                |---|---|---|
                                | 200 OK | La solicitud se proceso correctamente y se devuelve el recurso. | Consultas (GET) y actualizacion de stock exitosa. |
                                | 201 Created | El recurso se creo correctamente. | Al registrar un producto nuevo en el catalogo. |
                                | 400 Bad Request | Los datos enviados no son validos. | Body invalido en POST/PUT (ej. precio negativo, stock negativo). |
                                | 401 Unauthorized | No se envio un token JWT o el token no es valido. | Cualquier endpoint que requiera autenticacion. |
                                | 403 Forbidden | El token es valido pero el usuario no tiene permisos para esta accion. | Crear productos o ajustar stock sin rol de administrador. |
                                | 404 Not Found | El recurso solicitado no existe. | Consultar o actualizar un producto con un id que no existe. |
                                | 409 Conflict | La solicitud es valida pero entra en conflicto con el estado actual del recurso. | Intentar descontar mas stock del disponible. |
                                | 500 Internal Server Error | Ocurrio un error inesperado en el servidor. | Fallas no controladas (ej. no se pudo publicar el evento de stock bajo). |
                                """))
                .servers(List.of(new Server()
                        .url("http://localhost:8082")
                        .description("Entorno local (Docker Compose)")))
                .components(new Components().addSecuritySchemes(ESQUEMA_BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER));
    }
}
