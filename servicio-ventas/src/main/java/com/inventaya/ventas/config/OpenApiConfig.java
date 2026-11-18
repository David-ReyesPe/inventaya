package com.inventaya.ventas.config;

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
    public OpenAPI openApiVentas() {
        return new OpenAPI()
                .openapi("3.0.3")
                .info(new Info()
                        .title("InventaYa - Servicio de Ventas")
                        .version("1.0.0")
                        .description("""
                                API REST para el registro, consulta y anulacion de ventas.
                                Una venta puede incluir uno o varios productos (lineas de venta).
                                Cada venta registrada publica el evento "venta-realizada" en Kafka
                                (topico ventas-topic), incluyendo todas sus lineas, para que el
                                Servicio de Inventario descuente el stock de cada producto. Las
                                ventas nunca se editan: si un registro es incorrecto, se anula
                                (evento "venta-anulada") y se crea una venta nueva con los datos
                                corregidos.

                                ### Codigos de respuesta HTTP usados en esta API

                                | Codigo | Significado | Cuando se usa |
                                |---|---|---|
                                | 200 OK | La solicitud se proceso correctamente y se devuelve el recurso. | Consultas (GET). |
                                | 201 Created | El recurso se creo correctamente. | Al registrar una venta nueva con exito. |
                                | 202 Accepted | La solicitud fue aceptada, pero su efecto secundario (compensar el inventario) se procesa de forma asincrona via Kafka. | Al anular una venta, cuando se prioriza confirmar rapido y dejar la reversion de stock al consumidor de eventos. |
                                | 400 Bad Request | Los datos enviados no son validos (campos faltantes, formato incorrecto, valores negativos, etc.). | Body invalido en POST o parametros de fecha invalidos en GET. |
                                | 401 Unauthorized | No se envio un token JWT o el token no es valido. | Cualquier endpoint que requiera autenticacion. |
                                | 403 Forbidden | El token es valido pero el usuario no tiene permisos para esta accion. | Anular una venta sin rol de administrador/supervisor. |
                                | 404 Not Found | El recurso solicitado no existe. | Consultar o anular una venta con un id que no existe. |
                                | 409 Conflict | La solicitud es valida pero entra en conflicto con el estado actual del recurso. | Intentar anular una venta que ya estaba anulada, o registrar una venta sin stock suficiente. |
                                | 500 Internal Server Error | Ocurrio un error inesperado en el servidor. | Fallas no controladas (ej. no se pudo publicar el evento en Kafka). |
                                """))
                .servers(List.of(new Server()
                        .url("http://localhost:8081")
                        .description("Entorno local (Docker Compose)")))
                .components(new Components().addSecuritySchemes(ESQUEMA_BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER));
    }
}
