package com.inventaya.ventas.controller;

import com.inventaya.ventas.dto.PaginaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RespuestasPaginadasTest {

    @Test
    @DisplayName("Primera pagina con paginas siguientes debe tener tienePaginaSiguiente=true y tienePaginaAnterior=false")
    void primeraPaginaConPosterior() {
        Page<String> pagina = new PageImpl<>(List.of("A"), PageRequest.of(0, 1), 3);
        ResponseEntity<PaginaResponse<String>> respuesta = RespuestasPaginadas.conEncabezados(pagina);

        // Validacion de encabezados HTTP
        assertEquals("true", respuesta.getHeaders().getFirst("X-Has-Next"));
        assertEquals("false", respuesta.getHeaders().getFirst("X-Has-Previous"));
        assertEquals("true", respuesta.getHeaders().getFirst("X-Tiene-Siguiente"));
        assertEquals("false", respuesta.getHeaders().getFirst("X-Tiene-Anterior"));
        assertEquals("3", respuesta.getHeaders().getFirst("X-Total-Count"));
        assertEquals("3", respuesta.getHeaders().getFirst("X-Total-Pages"));
        assertEquals("0", respuesta.getHeaders().getFirst("X-Page"));

        // Validacion de parametros en el cuerpo JSON
        PaginaResponse<String> cuerpo = respuesta.getBody();
        assertNotNull(cuerpo);
        assertEquals(List.of("A"), cuerpo.contenido());
        assertTrue(cuerpo.tienePaginaSiguiente());
        assertFalse(cuerpo.tienePaginaAnterior());
        assertEquals(0, cuerpo.paginaActual());
        assertEquals(3, cuerpo.totalElementos());
        assertEquals(3, cuerpo.totalPaginas());
    }

    @Test
    @DisplayName("Pagina intermedia debe tener tienePaginaSiguiente=true y tienePaginaAnterior=true")
    void paginaIntermedia() {
        Page<String> pagina = new PageImpl<>(List.of("B"), PageRequest.of(1, 1), 3);
        ResponseEntity<PaginaResponse<String>> respuesta = RespuestasPaginadas.conEncabezados(pagina);

        assertEquals("true", respuesta.getHeaders().getFirst("X-Has-Next"));
        assertEquals("true", respuesta.getHeaders().getFirst("X-Has-Previous"));

        PaginaResponse<String> cuerpo = respuesta.getBody();
        assertNotNull(cuerpo);
        assertTrue(cuerpo.tienePaginaSiguiente());
        assertTrue(cuerpo.tienePaginaAnterior());
    }

    @Test
    @DisplayName("Ultima pagina debe tener tienePaginaSiguiente=false y tienePaginaAnterior=true")
    void ultimaPagina() {
        Page<String> pagina = new PageImpl<>(List.of("C"), PageRequest.of(2, 1), 3);
        ResponseEntity<PaginaResponse<String>> respuesta = RespuestasPaginadas.conEncabezados(pagina);

        assertEquals("false", respuesta.getHeaders().getFirst("X-Has-Next"));
        assertEquals("true", respuesta.getHeaders().getFirst("X-Has-Previous"));

        PaginaResponse<String> cuerpo = respuesta.getBody();
        assertNotNull(cuerpo);
        assertFalse(cuerpo.tienePaginaSiguiente());
        assertTrue(cuerpo.tienePaginaAnterior());
    }

    @Test
    @DisplayName("Pagina unica debe tener tienePaginaSiguiente=false y tienePaginaAnterior=false")
    void paginaUnica() {
        Page<String> pagina = new PageImpl<>(List.of("A", "B"), PageRequest.of(0, 10), 2);
        ResponseEntity<PaginaResponse<String>> respuesta = RespuestasPaginadas.conEncabezados(pagina);

        assertEquals("false", respuesta.getHeaders().getFirst("X-Has-Next"));
        assertEquals("false", respuesta.getHeaders().getFirst("X-Has-Previous"));

        PaginaResponse<String> cuerpo = respuesta.getBody();
        assertNotNull(cuerpo);
        assertFalse(cuerpo.tienePaginaSiguiente());
        assertFalse(cuerpo.tienePaginaAnterior());
    }
}
