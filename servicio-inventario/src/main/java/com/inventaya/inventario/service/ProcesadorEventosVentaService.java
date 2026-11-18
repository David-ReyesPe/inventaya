package com.inventaya.inventario.service;

import com.inventaya.inventario.entity.EventoProcesado;
import com.inventaya.inventario.entity.Producto;
import com.inventaya.inventario.kafka.EventoEnvelope;
import com.inventaya.inventario.kafka.ItemEventoPayload;
import com.inventaya.inventario.kafka.VentaAnuladaPayload;
import com.inventaya.inventario.kafka.VentaRealizadaPayload;
import com.inventaya.inventario.repository.EventoProcesadoRepository;
import com.inventaya.inventario.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Aplica sobre el inventario los eventos que llegan de ventas-topic.
 *
 * RF3: "venta-realizada" descuenta el stock de cada item.
 * RF7: "venta-anulada" revierte (suma de vuelta) el stock de cada item.
 *
 * Todo el procesamiento es idempotente por {@code eventoId}: una reentrega del mismo
 * evento no vuelve a mover el stock.
 */
@Service
public class ProcesadorEventosVentaService {

    private static final Logger log = LoggerFactory.getLogger(ProcesadorEventosVentaService.class);

    private final ProductoRepository productoRepository;
    private final EventoProcesadoRepository eventoProcesadoRepository;
    private final AlertaStockService alertaStockService;

    public ProcesadorEventosVentaService(ProductoRepository productoRepository,
                                        EventoProcesadoRepository eventoProcesadoRepository,
                                        AlertaStockService alertaStockService) {
        this.productoRepository = productoRepository;
        this.eventoProcesadoRepository = eventoProcesadoRepository;
        this.alertaStockService = alertaStockService;
    }

    /**
     * @return true si el evento se aplico; false si era duplicado o venia sin items.
     */
    @Transactional
    public boolean aplicarVentaRealizada(EventoEnvelope<VentaRealizadaPayload> evento) {
        List<ItemEventoPayload> items = itemsDe(evento, evento.payload() == null ? null : evento.payload().items());
        if (items == null || yaProcesado(evento)) {
            return false;
        }

        for (ItemEventoPayload item : items) {
            buscarProducto(item).ifPresent(producto -> {
                int solicitado = item.cantidad();
                int aDescontar = Math.min(solicitado, producto.getStock());
                if (aDescontar < solicitado) {
                    // No deberia ocurrir: Ventas valida el stock antes de registrar. Si pasa
                    // (dos ventas concurrentes del mismo producto), se descuenta hasta cero y
                    // se registra para revision manual, en lugar de bloquear la particion.
                    log.warn("Stock insuficiente al aplicar venta {}: producto {} pedia {} y solo habia {}",
                            evento.payload().ventaId(), producto.getId(), solicitado, producto.getStock());
                }
                producto.descontarStock(aDescontar);
                log.info("Stock descontado por venta {}: producto {} -{} -> {}",
                        evento.payload().ventaId(), producto.getId(), aDescontar, producto.getStock());
                alertaStockService.registrarSiCorresponde(producto);
            });
        }

        marcarProcesado(evento);
        return true;
    }

    /**
     * @return true si el evento se aplico; false si era duplicado o venia sin items.
     */
    @Transactional
    public boolean aplicarVentaAnulada(EventoEnvelope<VentaAnuladaPayload> evento) {
        List<ItemEventoPayload> items = itemsDe(evento, evento.payload() == null ? null : evento.payload().items());
        if (items == null || yaProcesado(evento)) {
            return false;
        }

        for (ItemEventoPayload item : items) {
            buscarProducto(item).ifPresent(producto -> {
                producto.incrementarStock(item.cantidad());
                log.info("Stock revertido por anulacion de venta {}: producto {} +{} -> {}",
                        evento.payload().ventaId(), producto.getId(), item.cantidad(), producto.getStock());
            });
        }

        marcarProcesado(evento);
        return true;
    }

    private Optional<Producto> buscarProducto(ItemEventoPayload item) {
        Optional<Producto> producto = productoRepository.buscarParaActualizar(item.productoId());
        if (producto.isEmpty()) {
            log.error("El evento referencia un producto inexistente ({}); se ignora esa linea.", item.productoId());
        }
        return producto;
    }

    private List<ItemEventoPayload> itemsDe(EventoEnvelope<?> evento, List<ItemEventoPayload> items) {
        if (items == null || items.isEmpty()) {
            log.error("Evento {} de tipo {} sin items; no hay nada que aplicar.", evento.eventoId(), evento.tipo());
            return null;
        }
        boolean cantidadesValidas = items.stream()
                .allMatch(item -> item.productoId() != null && item.cantidad() != null && item.cantidad() > 0);
        if (!cantidadesValidas) {
            log.error("Evento {} de tipo {} con items invalidos; se descarta.", evento.eventoId(), evento.tipo());
            return null;
        }
        return items;
    }

    private boolean yaProcesado(EventoEnvelope<?> evento) {
        UUID eventoId = evento.eventoId();
        if (eventoId == null) {
            log.error("Evento de tipo {} sin eventoId; no se puede garantizar idempotencia, se descarta.", evento.tipo());
            return true;
        }
        if (eventoProcesadoRepository.existsById(eventoId)) {
            log.info("Evento {} ({}) ya fue procesado; se ignora por idempotencia.", eventoId, evento.tipo());
            return true;
        }
        return false;
    }

    private void marcarProcesado(EventoEnvelope<?> evento) {
        eventoProcesadoRepository.save(EventoProcesado.de(evento.eventoId(), evento.tipo()));
    }
}
