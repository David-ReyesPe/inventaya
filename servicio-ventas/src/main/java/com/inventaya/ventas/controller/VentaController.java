package com.inventaya.ventas.controller;

import com.inventaya.ventas.config.RolRequerido;
import com.inventaya.ventas.config.RolUsuario;
import com.inventaya.ventas.dto.AnulacionInput;
import com.inventaya.ventas.dto.ErrorResponse;
import com.inventaya.ventas.dto.PaginaResponse;
import com.inventaya.ventas.dto.VentaInput;
import com.inventaya.ventas.dto.VentaResponse;
import com.inventaya.ventas.service.VentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/ventas", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Ventas", description = "Registro, consulta y anulacion de ventas")
@SecurityRequirement(name = "bearerAuth")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @PostMapping
    @Operation(
            summary = "Registrar una venta con uno o varios productos",
            description = """
                    Registra una nueva venta con una o mas lineas de producto (items), la persiste y
                    publica el evento "venta-realizada" en el topico ventas-topic de Kafka, incluyendo
                    el detalle de todos los items para que el Servicio de Inventario descuente el stock
                    de cada producto vendido.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201",
                    description = "201 Created - La venta se registro exitosamente con todas sus lineas. Se devuelve el recurso creado, incluyendo su id, el detalle de cada item, el total y el estado.",
                    content = @Content(schema = @Schema(implementation = VentaResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "400 Bad Request - Datos invalidos: no se envio ningun item, un item tiene cantidad menor o igual a cero, precio negativo, o se referencia un producto que no existe en el catalogo.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401",
                    description = "401 Unauthorized - No se envio un token JWT valido.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409",
                    description = "409 Conflict - No hay stock suficiente de uno o mas productos de la venta para completarla. La venta completa se rechaza (no se registran ventas parciales).",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500",
                    description = "500 Internal Server Error - Error inesperado, por ejemplo al persistir la venta o publicar el evento en Kafka.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<VentaResponse> registrar(@Valid @RequestBody VentaInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.registrar(input));
    }

    @GetMapping
    @Operation(
            summary = "Listar ventas",
            description = """
                    Devuelve el listado de ventas paginado, con filtros opcionales de fecha.
                    La respuesta incluye los elementos en 'contenido' y los parametros de navegacion
                    ('tienePaginaSiguiente', 'tienePaginaAnterior', etc.) en el cuerpo JSON y
                    en los encabezados HTTP.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "200 OK - Pagina de ventas obtenida correctamente.",
                    content = @Content(schema = @Schema(implementation = PaginaResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "400 Bad Request - El rango de fechas es invalido (ej. \"desde\" posterior a \"hasta\") o los parametros de paginacion no son validos.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - No se envio un token JWT valido.", content = @Content),
            @ApiResponse(responseCode = "500",
                    description = "500 Internal Server Error - Error inesperado al consultar las ventas.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PaginaResponse<VentaResponse>> listar(
            @Parameter(description = "Fecha inicial del rango de consulta.", example = "2026-08-01T00:00:00Z")
            @RequestParam(required = false) Instant desde,
            @Parameter(description = "Fecha final del rango de consulta.", example = "2026-08-31T23:59:59Z")
            @RequestParam(required = false) Instant hasta,
            @Parameter(description = "Numero de pagina (base cero).")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "El parametro 'page' no puede ser negativo.") int page,
            @Parameter(description = "Cantidad de elementos por pagina.")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "El parametro 'size' debe ser al menos 1.")
            @Max(value = 200, message = "El parametro 'size' no puede exceder 200.") int size) {

        Page<VentaResponse> pagina = ventaService.listar(desde, hasta,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fecha")));
        return RespuestasPaginadas.conEncabezados(pagina);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una venta por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "200 OK - La venta fue encontrada y se devuelve completa.",
                    content = @Content(schema = @Schema(implementation = VentaResponse.class))),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - No se envio un token JWT valido.", content = @Content),
            @ApiResponse(responseCode = "404",
                    description = "404 Not Found - No existe una venta con el id indicado.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500",
                    description = "500 Internal Server Error - Error inesperado al consultar la venta.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public VentaResponse obtener(@PathVariable UUID id) {
        return ventaService.obtener(id);
    }

    @PatchMapping("/{id}/anular")
    @RolRequerido(RolUsuario.ADMIN)
    @Operation(
            summary = "Anular una venta",
            description = """
                    Marca la venta como ANULADA (no la elimina, para conservar el historial) y publica
                    el evento "venta-anulada" en Kafka, para que el Servicio de Inventario revierta el
                    descuento de stock. Para registrar la venta corregida se debe crear una venta nueva
                    con POST /api/ventas.""")
    @ApiResponses({
            @ApiResponse(responseCode = "202",
                    description = "202 Accepted - La solicitud de anulacion fue aceptada. La venta queda marcada como ANULADA de inmediato, pero la reversion del stock de cada producto en el Servicio de Inventario ocurre de forma asincrona al consumir el evento \"venta-anulada\".",
                    content = @Content(schema = @Schema(implementation = VentaResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "400 Bad Request - Falta el motivo de anulacion.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - No se envio un token JWT valido.", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "403 Forbidden - El usuario autenticado no tiene el rol necesario (ej. administrador/supervisor) para anular ventas.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404",
                    description = "404 Not Found - No existe una venta con el id indicado.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409",
                    description = "409 Conflict - La venta ya se encontraba anulada previamente.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500",
                    description = "500 Internal Server Error - Error inesperado al anular la venta.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<VentaResponse> anular(@PathVariable UUID id, @Valid @RequestBody AnulacionInput input) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ventaService.anular(id, input));
    }
}
