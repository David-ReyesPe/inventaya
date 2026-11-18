package com.inventaya.ventas.controller;

import com.inventaya.ventas.config.JwtService;
import com.inventaya.ventas.config.RolUsuario;
import com.inventaya.ventas.entity.Venta;
import com.inventaya.ventas.kafka.VentaEventProducer;
import com.inventaya.ventas.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VentaControllerIT {

    private static final UUID ARROZ = UUID.fromString("b3f1c2a0-1234-4a56-9abc-1234567890ab");
    private static final UUID ACEITE = UUID.fromString("a9d8e7f6-4321-4b65-8fed-0987654321cd");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private VentaEventProducer ventaEventProducer;

    private String tokenAdmin;
    private String tokenUsuario;

    @BeforeEach
    void preparar() {
        ventaRepository.deleteAll();
        jdbcTemplate.update("delete from productos");
        registrarProductoEnCatalogo(ARROZ, "Arroz 500g", 50);
        registrarProductoEnCatalogo(ACEITE, "Aceite 1L", 20);
        tokenAdmin = "Bearer " + jwtService.generarToken("admin", RolUsuario.ADMIN);
        tokenUsuario = "Bearer " + jwtService.generarToken("cajero", RolUsuario.USER);
    }

    @Test
    @DisplayName("POST /api/ventas con varios items devuelve 201, calcula el total y publica el evento")
    void registrarVentaDevuelve201() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoVentaDeDosItems()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].subtotal").value(13500.00))
                .andExpect(jsonPath("$.items[1].subtotal").value(8000.00))
                .andExpect(jsonPath("$.total").value(21500.00))
                .andExpect(jsonPath("$.estado").value("REGISTRADA"))
                .andExpect(jsonPath("$.fecha").exists());

        verify(ventaEventProducer).publicarVentaRealizada(any(Venta.class));
    }

    @Test
    @DisplayName("POST /api/ventas sin items devuelve 400 con el mensaje del contrato")
    void registrarVentaSinItemsDevuelve400() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"items\": [] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje").value("La venta debe incluir al menos un producto."))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(ventaEventProducer, never()).publicarVentaRealizada(any());
    }

    @Test
    @DisplayName("POST /api/ventas con cantidad cero devuelve 400")
    void registrarVentaConCantidadCeroDevuelve400() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    { "productoId": "%s", "cantidad": 0, "precioUnitario": 4500 }
                                  ]
                                }
                                """.formatted(ARROZ)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La cantidad de cada item debe ser mayor que cero."));
    }

    @Test
    @DisplayName("POST /api/ventas sin stock suficiente devuelve 409 y no registra la venta")
    void registrarVentaSinStockDevuelve409() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    { "productoId": "%s", "cantidad": 999, "precioUnitario": 4500 }
                                  ]
                                }
                                """.formatted(ARROZ)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value(409))
                .andExpect(jsonPath("$.mensaje").value("Stock insuficiente para el producto " + ARROZ + "."));

        org.assertj.core.api.Assertions.assertThat(ventaRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /api/ventas sin token devuelve 401")
    void registrarVentaSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoVentaDeDosItems()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401));
    }

    @Test
    @DisplayName("GET /api/ventas devuelve el arreglo de ventas, los encabezados de paginacion y filtra por fecha")
    void listarVentasFiltraPorFecha() throws Exception {
        ventaRepository.save(Venta.registrar(List.of(
                new Venta.LineaNueva(ARROZ, 1, new BigDecimal("4500")))));

        mockMvc.perform(get("/api/ventas").header(HttpHeaders.AUTHORIZATION, tokenUsuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.tienePaginaSiguiente").value(false))
                .andExpect(jsonPath("$.tienePaginaAnterior").value(false))
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(header().string("X-Page-Size", "20"));

        mockMvc.perform(get("/api/ventas")
                        .param("desde", "2020-01-01T00:00:00Z")
                        .param("hasta", "2020-12-31T23:59:59Z")
                        .header(HttpHeaders.AUTHORIZATION, tokenUsuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(0)))
                .andExpect(header().string("X-Total-Count", "0"));
    }

    @Test
    @DisplayName("GET /api/ventas con 'desde' posterior a 'hasta' devuelve 400")
    void listarVentasConRangoInvertidoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/ventas")
                        .param("desde", "2026-08-31T00:00:00Z")
                        .param("hasta", "2026-08-01T00:00:00Z")
                        .header(HttpHeaders.AUTHORIZATION, tokenUsuario))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400));
    }

    @Test
    @DisplayName("GET /api/ventas/{id} inexistente devuelve 404")
    void consultarVentaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/ventas/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, tokenUsuario))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value(404))
                .andExpect(jsonPath("$.mensaje").value("No se encontro la venta solicitada."));
    }

    @Test
    @DisplayName("PATCH /api/ventas/{id}/anular devuelve 202 y deja la venta en ANULADA")
    void anularVentaDevuelve202() throws Exception {
        UUID ventaId = ventaRegistradaEnBase();

        mockMvc.perform(patch("/api/ventas/{id}/anular", ventaId)
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAnulacion()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(ventaId.toString()))
                .andExpect(jsonPath("$.estado").value("ANULADA"))
                .andExpect(jsonPath("$.items", hasSize(1)));

        verify(ventaEventProducer).publicarVentaAnulada(any(Venta.class));
    }

    @Test
    @DisplayName("PATCH /api/ventas/{id}/anular sobre una venta ya anulada devuelve 409")
    void anularVentaYaAnuladaDevuelve409() throws Exception {
        UUID ventaId = ventaRegistradaEnBase();

        mockMvc.perform(patch("/api/ventas/{id}/anular", ventaId)
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAnulacion()))
                .andExpect(status().isAccepted());

        mockMvc.perform(patch("/api/ventas/{id}/anular", ventaId)
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAnulacion()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value(409))
                .andExpect(jsonPath("$.mensaje").value("La venta ya fue anulada previamente."));
    }

    @Test
    @DisplayName("PATCH /api/ventas/{id}/anular con rol USER devuelve 403")
    void anularVentaConRolUsuarioDevuelve403() throws Exception {
        UUID ventaId = ventaRegistradaEnBase();

        mockMvc.perform(patch("/api/ventas/{id}/anular", ventaId)
                        .header(HttpHeaders.AUTHORIZATION, tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAnulacion()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(403));

        verify(ventaEventProducer, never()).publicarVentaAnulada(any());
    }

    @Test
    @DisplayName("PATCH /api/ventas/{id}/anular sin motivo valido devuelve 400")
    void anularVentaSinMotivoDevuelve400() throws Exception {
        UUID ventaId = ventaRegistradaEnBase();

        mockMvc.perform(patch("/api/ventas/{id}/anular", ventaId)
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"motivo\": \"x\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400));
    }

    @Test
    @DisplayName("PATCH /api/ventas/{id}/anular sobre un id inexistente devuelve 404")
    void anularVentaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(patch("/api/ventas/{id}/anular", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAnulacion()))
                .andExpect(status().isNotFound());
    }

    private UUID ventaRegistradaEnBase() {
        return ventaRepository.save(Venta.registrar(List.of(
                new Venta.LineaNueva(ARROZ, 3, new BigDecimal("4500"))))).getId();
    }

    private void registrarProductoEnCatalogo(UUID id, String nombre, int stock) {
        jdbcTemplate.update("insert into productos (id, nombre, stock) values (?, ?, ?)", id, nombre, stock);
    }

    private String cuerpoVentaDeDosItems() {
        return """
                {
                  "items": [
                    { "productoId": "%s", "cantidad": 3, "precioUnitario": 4500 },
                    { "productoId": "%s", "cantidad": 1, "precioUnitario": 8000 }
                  ]
                }
                """.formatted(ARROZ, ACEITE);
    }

    private String cuerpoAnulacion() {
        return "{ \"motivo\": \"Cantidad registrada por error, se corrige con una nueva venta.\" }";
    }
}
