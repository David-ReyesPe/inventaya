package com.inventaya.inventario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Registro historico de cada vez que un producto quedo por debajo de su umbral minimo.
 * Se persiste aunque el Servicio de Notificaciones todavia no exista, para dejar evidencia
 * del evento "stock-bajo" publicado en alertas-topic.
 */
@Entity
@Table(name = "alertas_stock")
@Getter
@Setter
@NoArgsConstructor
public class AlertaStock {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "stock_al_momento", nullable = false)
    private int stockAlMomento;

    @Column(name = "umbral", nullable = false)
    private int umbral;

    @Column(name = "fecha", nullable = false)
    private Instant fecha;

    @Column(name = "atendida", nullable = false)
    private boolean atendida;

    public static AlertaStock de(Producto producto) {
        AlertaStock alerta = new AlertaStock();
        alerta.id = UUID.randomUUID();
        alerta.producto = producto;
        alerta.stockAlMomento = producto.getStock();
        alerta.umbral = producto.getUmbralMinimo();
        alerta.fecha = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        alerta.atendida = false;
        return alerta;
    }
}
