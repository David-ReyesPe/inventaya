package com.inventaya.inventario.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventaya.inventario.service.ProcesadorEventosVentaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumidor de ventas-topic (grupo inventario-service-group).
 *
 * Los mensajes se reciben como texto y se deserializan aqui contra el sobre comun, en vez
 * de confiar en los headers de tipo de Spring Kafka: los servicios no comparten clases y el
 * contrato entre ellos es el JSON del evento, no el nombre del paquete del productor.
 */
@Component
public class VentasEventListener {

    private static final Logger log = LoggerFactory.getLogger(VentasEventListener.class);

    private final ObjectMapper objectMapper;
    private final ProcesadorEventosVentaService procesador;

    public VentasEventListener(ObjectMapper objectMapper, ProcesadorEventosVentaService procesador) {
        this.objectMapper = objectMapper;
        this.procesador = procesador;
    }

    @KafkaListener(
            topics = "${inventaya.kafka.topics.ventas}",
            groupId = "${inventaya.kafka.grupo-consumidor}")
    public void escuchar(String mensaje,
                         @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String ventaId) {

        EventoEnvelope<JsonNode> sobre;
        try {
            sobre = objectMapper.readValue(mensaje, new TypeReference<EventoEnvelope<JsonNode>>() {
            });
        } catch (JsonProcessingException ex) {
            // Mensaje irrecuperable: reintentarlo bloquearia la particion para siempre.
            // El descarte queda registrado hasta que se defina la DLQ (ventas-topic-dlq).
            log.error("Mensaje ilegible en ventas-topic (key={}); se descarta. Contenido: {}", ventaId, mensaje, ex);
            return;
        }

        log.debug("Evento recibido de ventas-topic: tipo={}, eventoId={}, key={}",
                sobre.tipo(), sobre.eventoId(), ventaId);

        switch (sobre.tipo() == null ? "" : sobre.tipo()) {
            case TiposEvento.VENTA_REALIZADA ->
                    procesador.aplicarVentaRealizada(convertir(sobre, VentaRealizadaPayload.class));
            case TiposEvento.VENTA_ANULADA ->
                    procesador.aplicarVentaAnulada(convertir(sobre, VentaAnuladaPayload.class));
            default ->
                    log.warn("Tipo de evento no manejado por Inventario: '{}' (eventoId={})", sobre.tipo(), sobre.eventoId());
        }
    }

    private <T> EventoEnvelope<T> convertir(EventoEnvelope<JsonNode> sobre, Class<T> tipoPayload) {
        T payload = sobre.payload() == null ? null : objectMapper.convertValue(sobre.payload(), tipoPayload);
        return new EventoEnvelope<>(sobre.eventoId(), sobre.tipo(), sobre.fecha(), payload);
    }
}
