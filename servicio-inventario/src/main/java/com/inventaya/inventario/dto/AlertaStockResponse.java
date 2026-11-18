package com.inventaya.inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Los nombres de los campos siguen el payload del evento "stock-bajo" y el tipo Alerta del
 * schema.graphqls ya aprobado (stockActual / umbralMinimo), para que el Gateway GraphQL y el
 * Servicio de Notificaciones puedan proyectar esta respuesta sin traducir nombres. Las columnas
 * de la tabla alertas_stock conservan los nombres del modelo de datos (stock_al_momento, umbral).
 */
@Schema(name = "AlertaStock", description = "Registro de un producto que quedo por debajo de su umbral minimo")
public record AlertaStockResponse(

        @Schema(description = "Identificador de la alerta.", example = "7f2b91c4-4a10-4c9e-9d55-9911aabbccdd")
        UUID id,

        @Schema(description = "Producto afectado.", example = "b3f1c2a0-1234-4a56-9abc-1234567890ab")
        UUID productoId,

        @Schema(description = "Nombre del producto al momento de consultar la alerta.", example = "Arroz 500g")
        String productoNombre,

        @Schema(description = "Stock que tenia el producto cuando se genero la alerta.", example = "4")
        int stockActual,

        @Schema(description = "Umbral minimo vigente cuando se genero la alerta.", example = "10")
        int umbralMinimo,

        @Schema(description = "Momento en que se genero la alerta.", example = "2026-08-19T15:32:05Z")
        Instant fecha,

        @Schema(description = "Indica si la alerta ya fue gestionada (por ejemplo, reponiendo stock).", example = "false")
        boolean atendida
) {
}
