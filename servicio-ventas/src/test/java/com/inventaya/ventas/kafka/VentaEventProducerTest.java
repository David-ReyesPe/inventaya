package com.inventaya.ventas.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inventaya.ventas.entity.Venta;
import com.inventaya.ventas.exception.EventoPublicacionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifica el formato exacto del mensaje que sale a ventas-topic. Es la contraparte del
 * VentasEventListenerIT de Inventario: como los dos servicios no comparten clases, el
 * contrato entre ellos es este JSON, y estas dos pruebas son la unica garantia automatica
 * de que productor y consumidor siguen hablando el mismo idioma.
 */
class VentaEventProducerTest {

    private static final UUID ARROZ = UUID.fromString("b3f1c2a0-1234-4a56-9abc-1234567890ab");
    private static final UUID ACEITE = UUID.fromString("a9d8e7f6-4321-4b65-8fed-0987654321cd");
    private static final String TOPICO = "ventas-topic";

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private VentaEventProducer producer;

    @BeforeEach
    void configurar() {
        producer = new VentaEventProducer(kafkaTemplate, objectMapper, TOPICO);
    }

    @Test
    @DisplayName("venta-realizada lleva el sobre comun y el detalle completo de cada item")
    void ventaRealizadaRespetaElContrato() throws Exception {
        aceptarEnvios();
        Venta venta = ventaDeDosLineas();

        producer.publicarVentaRealizada(venta);

        JsonNode evento = objectMapper.readTree(mensajePublicado());
        assertThat(evento.get("eventoId").asText()).isNotBlank();
        assertThat(evento.get("tipo").asText()).isEqualTo("venta-realizada");
        assertThat(evento.get("fecha").asText()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");

        JsonNode payload = evento.get("payload");
        assertThat(payload.get("ventaId").asText()).isEqualTo(venta.getId().toString());
        assertThat(payload.get("total").decimalValue()).isEqualByComparingTo("21500");

        JsonNode items = payload.get("items");
        assertThat(items).hasSize(2);
        assertThat(items.get(0).get("productoId").asText()).isEqualTo(ARROZ.toString());
        assertThat(items.get(0).get("cantidad").asInt()).isEqualTo(3);
        assertThat(items.get(0).get("precioUnitario").decimalValue()).isEqualByComparingTo("4500");
        assertThat(items.get(0).get("subtotal").decimalValue()).isEqualByComparingTo("13500");
    }

    @Test
    @DisplayName("venta-anulada lleva el motivo y solo productoId y cantidad en cada item")
    void ventaAnuladaRespetaElContrato() throws Exception {
        aceptarEnvios();
        Venta venta = ventaDeDosLineas();
        venta.anular("Cantidad registrada por error, se corrige con una nueva venta.");

        producer.publicarVentaAnulada(venta);

        JsonNode evento = objectMapper.readTree(mensajePublicado());
        assertThat(evento.get("tipo").asText()).isEqualTo("venta-anulada");

        JsonNode payload = evento.get("payload");
        assertThat(payload.get("ventaId").asText()).isEqualTo(venta.getId().toString());
        assertThat(payload.get("motivo").asText())
                .isEqualTo("Cantidad registrada por error, se corrige con una nueva venta.");

        JsonNode primerItem = payload.get("items").get(0);
        assertThat(primerItem.fieldNames()).toIterable().containsExactly("productoId", "cantidad");
        assertThat(primerItem.get("productoId").asText()).isEqualTo(ARROZ.toString());
        assertThat(primerItem.get("cantidad").asInt()).isEqualTo(3);
    }

    @Test
    @DisplayName("La key del mensaje es el ventaId, que es lo que garantiza el orden por venta")
    void laKeyEsElVentaId() {
        aceptarEnvios();
        Venta venta = ventaDeDosLineas();

        producer.publicarVentaRealizada(venta);

        verify(kafkaTemplate).send(eq(TOPICO), eq(venta.getId().toString()), anyString());
    }

    @Test
    @DisplayName("Si Kafka no acepta el evento, la publicacion falla y la transaccion se revierte")
    void falloDeKafkaSePropaga() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker caido")));

        assertThatThrownBy(() -> producer.publicarVentaRealizada(ventaDeDosLineas()))
                .isInstanceOf(EventoPublicacionException.class)
                .hasMessageContaining("No se pudo publicar el evento venta-realizada en Kafka.");
    }

    private void aceptarEnvios() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    private String mensajePublicado() {
        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(TOPICO), any(), mensaje.capture());
        return mensaje.getValue();
    }

    private Venta ventaDeDosLineas() {
        return Venta.registrar(List.of(
                new Venta.LineaNueva(ARROZ, 3, new BigDecimal("4500")),
                new Venta.LineaNueva(ACEITE, 1, new BigDecimal("8000"))));
    }
}
