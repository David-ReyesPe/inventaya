package com.inventaya.ventas.controller;

import com.inventaya.ventas.dto.PaginaResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

/**
 * Encapsula la respuesta paginada con sus metadatos tanto en el cuerpo JSON
 * (PaginaResponse con tienePaginaSiguiente, tienePaginaAnterior, totalElementos, etc.)
 * como en los encabezados HTTP.
 */
final class RespuestasPaginadas {

    static final String TOTAL_ELEMENTOS = "X-Total-Count";
    static final String TOTAL_PAGINAS = "X-Total-Pages";
    static final String PAGINA_ACTUAL = "X-Page";
    static final String TAMANO_PAGINA = "X-Page-Size";
    static final String TIENE_SIGUIENTE = "X-Has-Next";
    static final String TIENE_ANTERIOR = "X-Has-Previous";

    private RespuestasPaginadas() {
    }

    static <T> ResponseEntity<PaginaResponse<T>> conEncabezados(Page<T> pagina) {
        return ResponseEntity.ok()
                .header(TOTAL_ELEMENTOS, String.valueOf(pagina.getTotalElements()))
                .header(TOTAL_PAGINAS, String.valueOf(pagina.getTotalPages()))
                .header(PAGINA_ACTUAL, String.valueOf(pagina.getNumber()))
                .header(TAMANO_PAGINA, String.valueOf(pagina.getSize()))
                .header(TIENE_SIGUIENTE, String.valueOf(pagina.hasNext()))
                .header(TIENE_ANTERIOR, String.valueOf(pagina.hasPrevious()))
                .header("X-Tiene-Siguiente", String.valueOf(pagina.hasNext()))
                .header("X-Tiene-Anterior", String.valueOf(pagina.hasPrevious()))
                .body(PaginaResponse.de(pagina));
    }
}
