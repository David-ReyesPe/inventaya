package com.inventaya.inventario.controller;

import com.inventaya.inventario.config.JwtService;
import com.inventaya.inventario.config.RolUsuario;
import com.inventaya.inventario.entity.AlertaStock;
import com.inventaya.inventario.entity.Producto;
import com.inventaya.inventario.kafka.AlertaEventProducer;
import com.inventaya.inventario.repository.AlertaStockRepository;
import com.inventaya.inventario.repository.EventoProcesadoRepository;
import com.inventaya.inventario.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductoControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private AlertaStockRepository alertaStockRepository;

    @Autowired
    private EventoProcesadoRepository eventoProcesadoRepository;

    @MockBean
    private AlertaEventProducer alertaEventProducer;

    private String tokenAdmin;
    private String tokenUsuario;

    @BeforeEach
    void preparar() {
        alertaStockRepository.deleteAll();
        eventoProcesadoRepository.deleteAll();
        productoRepository.deleteAll();
        tokenAdmin = "Bearer " + jwtService.generarToken("admin", RolUsuario.ADMIN);
        tokenUsuario = "Bearer " + jwtService.generarToken("cajero", RolUsuario.USER);
    }

    @Test
    @DisplayName("POST /api/productos devuelve 201 con el producto creado")
    void crearProductoDevuelve201() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Arroz 500g",
                                  "precio": 3200,
                                  "stockInicial": 50,
                                  "umbralMinimo": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value("Arroz 500g"))
                .andExpect(jsonPath("$.stock").value(50))
                .andExpect(jsonPath("$.umbralMinimo").value(10));
    }

    @Test
    @DisplayName("POST /api/productos con precio negativo devuelve 400 con el schema ErrorResponse")
    void crearProductoConPrecioNegativoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Arroz 500g",
                                  "precio": -1,
                                  "stockInicial": 50,
                                  "umbralMinimo": 10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje").value("El precio no puede ser negativo."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("POST /api/productos sin token devuelve 401")
    void crearProductoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401))
                .andExpect(jsonPath("$.mensaje").value("No se envio un token JWT valido."));
    }

    @Test
    @DisplayName("POST /api/productos con rol USER devuelve 403")
    void crearProductoConRolUsuarioDevuelve403() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Arroz 500g",
                                  "precio": 3200,
                                  "stockInicial": 50,
                                  "umbralMinimo": 10
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(403));
    }

    @Test
    @DisplayName("GET /api/productos devuelve el arreglo de productos y los encabezados de paginacion")
    void listarProductosDevuelveArregloYEncabezados() throws Exception {
        productoRepository.save(Producto.crear("Arroz 500g", new BigDecimal("3200.00"), 50, 10));
        productoRepository.save(Producto.crear("Aceite 1L", new BigDecimal("8000.00"), 20, 5));

        mockMvc.perform(get("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, tokenUsuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(2)))
                .andExpect(jsonPath("$.tienePaginaSiguiente").value(false))
                .andExpect(jsonPath("$.tienePaginaAnterior").value(false))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(1))
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(header().string("X-Total-Pages", "1"));
    }

    @Test
    @DisplayName("GET /api/productos con size invalido devuelve 400")
    void listarProductosConSizeInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/productos")
                        .param("size", "0")
                        .header(HttpHeaders.AUTHORIZATION, tokenUsuario))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400));
    }

    @Test
    @DisplayName("GET /api/productos/{id} inexistente devuelve 404")
    void consultarProductoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/productos/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, tokenUsuario))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value(404))
                .andExpect(jsonPath("$.mensaje").value("No se encontro el producto solicitado."));
    }

    @Test
    @DisplayName("PUT /api/productos/{id}/stock con INCREMENTO devuelve 200 y el stock actualizado")
    void ajustarStockDevuelve200() throws Exception {
        Producto producto = productoRepository.save(Producto.crear("Arroz 500g", new BigDecimal("3200.00"), 50, 10));

        mockMvc.perform(put("/api/productos/{id}/stock", producto.getId())
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operacion": "INCREMENTO",
                                  "cantidad": 30
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(80));
    }

    @Test
    @DisplayName("PUT /api/productos/{id}/stock descontando mas de lo disponible devuelve 409")
    void ajustarStockSinDisponibilidadDevuelve409() throws Exception {
        Producto producto = productoRepository.save(Producto.crear("Arroz 500g", new BigDecimal("3200.00"), 5, 10));

        mockMvc.perform(put("/api/productos/{id}/stock", producto.getId())
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operacion": "DECREMENTO",
                                  "cantidad": 6
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value(409))
                .andExpect(jsonPath("$.mensaje").value("No hay stock suficiente para realizar este ajuste."));
    }

    @Test
    @DisplayName("PUT /api/productos/{id}/stock con cantidad cero devuelve 400")
    void ajustarStockConCantidadCeroDevuelve400() throws Exception {
        Producto producto = productoRepository.save(Producto.crear("Arroz 500g", new BigDecimal("3200.00"), 5, 10));

        mockMvc.perform(put("/api/productos/{id}/stock", producto.getId())
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operacion": "DECREMENTO",
                                  "cantidad": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La cantidad debe ser mayor que cero."));
    }

    @Test
    @DisplayName("PUT /api/productos/{id} actualiza nombre, precio y umbral minimo")
    void actualizarProductoDevuelve200() throws Exception {
        Producto producto = productoRepository.save(Producto.crear("Arroz 500g", new BigDecimal("3200.00"), 50, 10));

        mockMvc.perform(put("/api/productos/{id}", producto.getId())
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Arroz premium 500g",
                                  "precio": 3500,
                                  "umbralMinimo": 15
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Arroz premium 500g"))
                .andExpect(jsonPath("$.umbralMinimo").value(15))
                .andExpect(jsonPath("$.stock").value(50));
    }

    @Test
    @DisplayName("GET /api/alertas devuelve las alertas registradas y respeta el filtro atendida")
    void listarAlertasFiltraPorAtendida() throws Exception {
        Producto producto = productoRepository.save(Producto.crear("Arroz 500g", new BigDecimal("3200.00"), 4, 10));
        alertaStockRepository.save(AlertaStock.de(producto));

        mockMvc.perform(get("/api/alertas")
                        .header(HttpHeaders.AUTHORIZATION, tokenUsuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].productoId").value(producto.getId().toString()))
                .andExpect(jsonPath("$.contenido[0].productoNombre").value("Arroz 500g"))
                .andExpect(jsonPath("$.contenido[0].stockActual").value(4))
                .andExpect(jsonPath("$.contenido[0].umbralMinimo").value(10))
                .andExpect(jsonPath("$.contenido[0].atendida").value(false))
                .andExpect(jsonPath("$.tienePaginaSiguiente").value(false))
                .andExpect(jsonPath("$.tienePaginaAnterior").value(false))
                .andExpect(header().string("X-Total-Count", "1"));

        mockMvc.perform(get("/api/alertas")
                        .param("atendida", "true")
                        .header(HttpHeaders.AUTHORIZATION, tokenUsuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/alertas sin token devuelve 401")
    void listarAlertasSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/alertas"))
                .andExpect(status().isUnauthorized());
    }
}
