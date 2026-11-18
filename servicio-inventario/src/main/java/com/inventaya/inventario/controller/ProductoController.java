package com.inventaya.inventario.controller;

import com.inventaya.inventario.config.RolRequerido;
import com.inventaya.inventario.config.RolUsuario;
import com.inventaya.inventario.dto.ErrorResponse;
import com.inventaya.inventario.dto.PaginaResponse;
import com.inventaya.inventario.dto.ProductoInput;
import com.inventaya.inventario.dto.ProductoResponse;
import com.inventaya.inventario.dto.ProductoUpdateInput;
import com.inventaya.inventario.dto.StockUpdateInput;
import com.inventaya.inventario.service.ProductoService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/productos", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Productos", description = "Catalogo de productos y control de stock")
@SecurityRequirement(name = "bearerAuth")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    @Operation(
            summary = "Listar productos",
            description = """
                    Devuelve el catalogo de productos de forma paginada. La respuesta incluye el
                    arreglo de elementos en 'contenido' y los parametros de navegacion de pagina
                    posterior y anterior ('tienePaginaSiguiente', 'tienePaginaAnterior', etc.),
                    ademas de los encabezados HTTP correspondientes.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "200 OK - Pagina del catalogo de productos obtenida correctamente.",
                    content = @Content(schema = @Schema(implementation = PaginaResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "400 Bad Request - Parametros de paginacion invalidos.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - No se envio un token JWT valido.", content = @Content),
            @ApiResponse(responseCode = "500",
                    description = "500 Internal Server Error - Error inesperado al consultar el catalogo.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PaginaResponse<ProductoResponse>> listar(
            @Parameter(description = "Numero de pagina (base cero).")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "El parametro 'page' no puede ser negativo.") int page,
            @Parameter(description = "Cantidad de elementos por pagina.")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "El parametro 'size' debe ser al menos 1.")
            @Max(value = 200, message = "El parametro 'size' no puede exceder 200.") int size) {

        Page<ProductoResponse> pagina = productoService.listar(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "creadoEn")));
        return RespuestasPaginadas.conEncabezados(pagina);
    }

    @PostMapping
    @RolRequerido(RolUsuario.ADMIN)
    @Operation(summary = "Registrar un producto nuevo")
    @ApiResponses({
            @ApiResponse(responseCode = "201",
                    description = "201 Created - El producto se registro exitosamente en el catalogo. Se devuelve el recurso creado con su id.",
                    content = @Content(schema = @Schema(implementation = ProductoResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "400 Bad Request - Datos invalidos: nombre vacio, precio negativo o umbral minimo negativo.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - No se envio un token JWT valido.", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "403 Forbidden - El usuario no tiene rol de administrador.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500",
                    description = "500 Internal Server Error - Error inesperado al registrar el producto.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(input));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un producto por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "200 OK - El producto fue encontrado y se devuelve completo.",
                    content = @Content(schema = @Schema(implementation = ProductoResponse.class))),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - No se envio un token JWT valido.", content = @Content),
            @ApiResponse(responseCode = "404",
                    description = "404 Not Found - No existe un producto con el id indicado.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500",
                    description = "500 Internal Server Error - Error inesperado al consultar el producto.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ProductoResponse obtener(@PathVariable UUID id) {
        return productoService.obtener(id);
    }

    @PutMapping("/{id}")
    @RolRequerido(RolUsuario.ADMIN)
    @Operation(
            summary = "Actualizar los datos de un producto",
            description = """
                    Actualiza nombre, precio y umbral minimo de un producto existente. Extension de
                    esta entrega para completar el CRUD de RF1. El stock no se modifica aqui: se
                    ajusta con PUT /api/productos/{id}/stock o por consumo de eventos de venta.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "200 OK - El producto se actualizo correctamente.",
                    content = @Content(schema = @Schema(implementation = ProductoResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "400 Bad Request - Datos invalidos: nombre vacio, precio negativo o umbral minimo negativo.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - No se envio un token JWT valido.", content = @Content),
            @ApiResponse(responseCode = "403", description = "403 Forbidden - El usuario no tiene rol de administrador.", content = @Content),
            @ApiResponse(responseCode = "404",
                    description = "404 Not Found - No existe un producto con el id indicado.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500",
                    description = "500 Internal Server Error - Error inesperado al actualizar el producto.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ProductoResponse actualizar(@PathVariable UUID id, @Valid @RequestBody ProductoUpdateInput input) {
        return productoService.actualizar(id, input);
    }

    @PutMapping("/{id}/stock")
    @RolRequerido(RolUsuario.ADMIN)
    @Operation(
            summary = "Ajustar el stock de un producto",
            description = """
                    Actualiza manualmente el stock de un producto (por ejemplo, al recibir mercancia
                    de un proveedor). El descuento de stock por ventas normales ocurre automaticamente
                    al consumir eventos de Kafka, no a traves de este endpoint.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "200 OK - El stock se actualizo correctamente.",
                    content = @Content(schema = @Schema(implementation = ProductoResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "400 Bad Request - La cantidad enviada es invalida (cero o negativa).",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - No se envio un token JWT valido.", content = @Content),
            @ApiResponse(responseCode = "403", description = "403 Forbidden - El usuario no tiene rol de administrador.", content = @Content),
            @ApiResponse(responseCode = "404",
                    description = "404 Not Found - No existe un producto con el id indicado.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409",
                    description = "409 Conflict - Se intenta descontar mas stock del disponible (el resultado seria negativo).",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500",
                    description = "500 Internal Server Error - Error inesperado al actualizar el stock.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ProductoResponse ajustarStock(@PathVariable UUID id, @Valid @RequestBody StockUpdateInput input) {
        return productoService.ajustarStock(id, input);
    }
}
