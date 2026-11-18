package com.inventaya.ventas;

import com.inventaya.ventas.kafka.VentaEventProducer;
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
 * Genera contratos/openapi-servicio-ventas.yaml a partir del codigo y verifica que el contrato
 * expuesto siga conteniendo todo lo acordado. Al regenerarse en cada "mvn test", el YAML
 * entregado nunca queda desfasado de la implementacion.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContratoOpenApiIT {

    private static final Path DESTINO = Path.of("..", "contratos", "openapi-servicio-ventas.yaml");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VentaEventProducer ventaEventProducer;

    @Test
    @DisplayName("El contrato OpenAPI expuesto cubre los endpoints acordados y se guarda en contratos/")
    void generaYValidaElContrato() throws Exception {
        String contrato = mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(contrato)
                .contains("title: InventaYa - Servicio de Ventas")
                .contains("/api/ventas:")
                .contains("/api/ventas/{id}:")
                .contains("/api/ventas/{id}/anular:")
                .contains("bearerAuth")
                .contains("Venta")
                .contains("VentaInput")
                .contains("AnulacionInput")
                .contains("ErrorResponse")
                .contains("\"201\"")
                .contains("\"202\"")
                .contains("\"409\"");

        Files.createDirectories(DESTINO.getParent());
        Files.writeString(DESTINO, contrato, StandardCharsets.UTF_8);
    }
}
