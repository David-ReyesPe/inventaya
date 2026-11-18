package com.inventaya.inventario.controller;

import com.inventaya.inventario.dto.AlertaStockResponse;
import com.inventaya.inventario.dto.ErrorResponse;
import com.inventaya.inventario.dto.PaginaResponse;
import com.inventaya.inventario.service.AlertaStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * RF6: consulta de las alertas de stock bajo registradas por el servicio. Endpoint agregado
 * en esta entrega (no estaba en el contrato original de Inventario).
 */
@RestController
@RequestMapping(value = "/api/alertas", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Alertas", description = "Consulta de alertas de stock bajo")
@SecurityRequirement(name = "bearerAuth")
public class AlertaController {

    private final AlertaStockService alertaStockService;

    public AlertaController(AlertaStockService alertaStockService) {
        this.alertaStockService = alertaStockService;
    }

    @GetMapping
    @Operation(
            summary = "Listar alertas de stock bajo",
            description = """
                    Devuelve las alertas generadas cuando el stock de un producto quedo por debajo de
                    su umbral minimo, ordenadas de la mas reciente a la mas antigua. Cada alerta de
                    esta lista corresponde a un evento "stock-bajo" publicado en alertas-topic.
                    La respuesta incluye los metadatos de navegacion con 'tienePaginaSiguiente' y
                    'tienePaginaAnterior' en el cuerpo JSON y en encabezados HTTP.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "200 OK - Listado de alertas obtenido correctamente (puede ser vacio).",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AlertaStockResponse.class)))),
            @ApiResponse(responseCode = "400",
                    description = "400 Bad Request - Parametros de consulta invalidos.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - No se envio un token JWT valido.", content = @Content),
            @ApiResponse(responseCode = "500",
                    description = "500 Internal Server Error - Error inesperado al consultar las alertas.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PaginaResponse<AlertaStockResponse>> listar(
            @Parameter(description = "Filtra por alertas atendidas (true) o pendientes (false). Si se omite, devuelve todas.")
            @RequestParam(required = false) Boolean atendida,
            @Parameter(description = "Numero de pagina (base cero).")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "El parametro 'page' no puede ser negativo.") int page,
            @Parameter(description = "Cantidad de elementos por pagina.")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "El parametro 'size' debe ser al menos 1.")
            @Max(value = 200, message = "El parametro 'size' no puede exceder 200.") int size) {

        Page<AlertaStockResponse> pagina = alertaStockService.listar(
                atendida, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fecha")));
        return RespuestasPaginadas.conEncabezados(pagina);
    }
}
