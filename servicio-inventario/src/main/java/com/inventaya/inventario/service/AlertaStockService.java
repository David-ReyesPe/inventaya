package com.inventaya.inventario.service;

import com.inventaya.inventario.dto.AlertaStockResponse;
import com.inventaya.inventario.entity.AlertaStock;
import com.inventaya.inventario.entity.Producto;
import com.inventaya.inventario.kafka.AlertaEventProducer;
import com.inventaya.inventario.mapper.AlertaStockMapper;
import com.inventaya.inventario.repository.AlertaStockRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertaStockService {

    private final AlertaStockRepository alertaStockRepository;
    private final AlertaEventProducer alertaEventProducer;

    public AlertaStockService(AlertaStockRepository alertaStockRepository,
                              AlertaEventProducer alertaEventProducer) {
        this.alertaStockRepository = alertaStockRepository;
        this.alertaEventProducer = alertaEventProducer;
    }

    /**
     * RF4: si el stock quedo por debajo del umbral minimo, deja registro en alertas_stock
     * y publica "stock-bajo" en alertas-topic. Se invoca despues de cualquier movimiento
     * de stock (ajuste manual o consumo de eventos de venta).
     */
    @Transactional
    public void registrarSiCorresponde(Producto producto) {
        if (!producto.stockPorDebajoDelUmbral()) {
            return;
        }
        alertaStockRepository.save(AlertaStock.de(producto));
        alertaEventProducer.publicarStockBajo(producto);
    }

    @Transactional(readOnly = true)
    public Page<AlertaStockResponse> listar(Boolean atendida, Pageable pageable) {
        Page<AlertaStock> alertas = atendida == null
                ? alertaStockRepository.findAll(pageable)
                : alertaStockRepository.findByAtendida(atendida, pageable);
        return alertas.map(AlertaStockMapper::aResponse);
    }
}
