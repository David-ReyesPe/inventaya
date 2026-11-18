package com.inventaya.ventas.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventaya.ventas.entity.Venta;
import com.inventaya.ventas.exception.EventoPublicacionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Publica los eventos de venta en ventas-topic usando ventaId como key: asi "venta-realizada"
 * y la eventual "venta-anulada" de una misma venta caen en la misma particion y se consumen
 * en orden.
 *
 * El envio es sincrono y ocurre dentro de la transaccion que persiste la venta: si Kafka no
 * acepta el evento, la transaccion se revierte y el endpoint responde 500, en lugar de dejar
 * una venta registrada que el inventario nunca vera.
 */
@Component
public class VentaEventProducer {

    private static final Logger log = LoggerFactory.getLogger(VentaEventProducer.class);
    private static final long TIMEOUT_SEGUNDOS = 10;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topicoVentas;

    public VentaEventProducer(KafkaTemplate<String, String> kafkaTemplate,
                              ObjectMapper objectMapper,
                              @Value("${inventaya.kafka.topics.ventas}") String topicoVentas) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topicoVentas = topicoVentas;
    }

    public void publicarVentaRealizada(Venta venta) {
        List<ItemVendidoPayload> items = venta.getItems().stream()
                .map(item -> new ItemVendidoPayload(
                        item.getProductoId(),
                        item.getCantidad(),
                        item.getPrecioUnitario(),
                        item.getSubtotal()))
                .toList();

        publicar(EventoEnvelope.nuevo(
                        TiposEvento.VENTA_REALIZADA,
                        new VentaRealizadaPayload(venta.getId(), items, venta.getTotal())),
                venta.getId().toString());
    }

    public void publicarVentaAnulada(Venta venta) {
        List<ItemRevertidoPayload> items = venta.getItems().stream()
                .map(item -> new ItemRevertidoPayload(item.getProductoId(), item.getCantidad()))
                .toList();

        publicar(EventoEnvelope.nuevo(
                        TiposEvento.VENTA_ANULADA,
                        new VentaAnuladaPayload(venta.getId(), venta.getMotivoAnulacion(), items)),
                venta.getId().toString());
    }

    private void publicar(EventoEnvelope<?> evento, String key) {
        try {
            String mensaje = objectMapper.writeValueAsString(evento);
            kafkaTemplate.send(topicoVentas, key, mensaje).get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            log.info("Evento {} publicado en {} (eventoId={}, key={})",
                    evento.tipo(), topicoVentas, evento.eventoId(), key);
        } catch (JsonProcessingException ex) {
            throw new EventoPublicacionException("No se pudo serializar el evento " + evento.tipo() + ".", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new EventoPublicacionException("Publicacion del evento " + evento.tipo() + " interrumpida.", ex);
        } catch (Exception ex) {
            throw new EventoPublicacionException(
                    "No se pudo publicar el evento " + evento.tipo() + " en Kafka.", ex);
        }
    }
}
