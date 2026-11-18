package com.inventaya.inventario.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventaya.inventario.entity.Producto;
import com.inventaya.inventario.exception.EventoPublicacionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Publica "stock-bajo" en alertas-topic usando productoId como key.
 *
 * El envio es sincrono a proposito: si Kafka no acepta el evento, la operacion que lo
 * origino falla (500 en el endpoint de ajuste, reintento en el consumidor) en lugar de
 * dejar el inventario y las alertas en estados divergentes.
 */
@Component
public class AlertaEventProducer {

    private static final Logger log = LoggerFactory.getLogger(AlertaEventProducer.class);
    private static final long TIMEOUT_SEGUNDOS = 10;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topicoAlertas;

    public AlertaEventProducer(KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper,
                               @Value("${inventaya.kafka.topics.alertas}") String topicoAlertas) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topicoAlertas = topicoAlertas;
    }

    public void publicarStockBajo(Producto producto) {
        EventoEnvelope<StockBajoPayload> evento = EventoEnvelope.nuevo(
                TiposEvento.STOCK_BAJO,
                new StockBajoPayload(producto.getId(), producto.getStock(), producto.getUmbralMinimo()));

        try {
            String mensaje = objectMapper.writeValueAsString(evento);
            kafkaTemplate.send(topicoAlertas, producto.getId().toString(), mensaje)
                    .get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            log.info("Evento stock-bajo publicado en {} (eventoId={}, productoId={}, stock={}, umbral={})",
                    topicoAlertas, evento.eventoId(), producto.getId(), producto.getStock(), producto.getUmbralMinimo());
        } catch (JsonProcessingException ex) {
            throw new EventoPublicacionException("No se pudo serializar el evento stock-bajo.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new EventoPublicacionException("Publicacion de stock-bajo interrumpida.", ex);
        } catch (Exception ex) {
            throw new EventoPublicacionException("No se pudo publicar el evento stock-bajo en Kafka.", ex);
        }
    }
}
