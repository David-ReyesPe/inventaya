package com.inventaya.inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(name = "PaginaResponse", description = "Respuesta paginada con elementos y control de navegacion")
public record PaginaResponse<T>(

        @Schema(description = "Listado de elementos en la pagina actual.")
        List<T> contenido,

        @Schema(description = "Numero de la pagina actual (base cero).", example = "0")
        int paginaActual,

        @Schema(description = "Cantidad de elementos por pagina.", example = "10")
        int tamanoPagina,

        @Schema(description = "Cantidad total de elementos en todas las paginas.", example = "25")
        long totalElementos,

        @Schema(description = "Cantidad total de paginas disponibles.", example = "3")
        int totalPaginas,

        @Schema(description = "Indica si existe una pagina posterior disponible.", example = "true")
        boolean tienePaginaSiguiente,

        @Schema(description = "Indica si existe una pagina anterior disponible.", example = "false")
        boolean tienePaginaAnterior
) {

    public static <T> PaginaResponse<T> de(Page<T> pagina) {
        return new PaginaResponse<>(
                pagina.getContent(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.hasNext(),
                pagina.hasPrevious()
        );
    }
}
