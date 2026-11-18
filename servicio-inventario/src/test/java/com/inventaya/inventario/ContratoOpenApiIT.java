package com.inventaya.inventario;

import com.inventaya.inventario.kafka.AlertaEventProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Genera contratos/openapi-servicio-inventario.yaml a partir del codigo y verifica que el
 * contrato expuesto siga conteniendo todo lo acordado, incluida la extension GET /api/alertas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContratoOpenApiIT {

    private static final Path DESTINO = Path.of("..", "contratos", "openapi-servicio-inventario.yaml");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlertaEventProducer alertaEventProducer;

    @Test
    @DisplayName("El contrato OpenAPI expuesto cubre los endpoints acordados y se guarda en contratos/")
    void generaYValidaElContrato() throws Exception {
        String contrato = mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(contrato)
                .contains("title: InventaYa - Servicio de Inventario")
                .contains("/api/productos:")
                .contains("/api/productos/{id}:")
                .contains("/api/productos/{id}/stock:")
                .contains("/api/alertas:")
                .contains("bearerAuth")
                .contains("Producto")
                .contains("ProductoInput")
                .contains("StockUpdateInput")
                .contains("AlertaStock")
                .contains("ErrorResponse")
                .contains("\"201\"")
                .contains("\"409\"");

        Files.createDirectories(DESTINO.getParent());
        Files.writeString(DESTINO, contrato, StandardCharsets.UTF_8);
    }
}
