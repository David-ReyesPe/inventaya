package com.inventaya.ventas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = "venta_items")
@Getter
@NoArgsConstructor
public class VentaItem {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    /**
     * Referencia al producto del catalogo. Es una FK real porque Ventas e Inventario comparten
     * la base inventaya_db, pero el stock de ese producto solo lo modifica Inventario.
     */
    @Column(name = "producto_id", nullable = false)
    private UUID productoId;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    static VentaItem de(UUID productoId, int cantidad, BigDecimal precioUnitario) {
        VentaItem item = new VentaItem();
        item.id = UUID.randomUUID();
        item.productoId = productoId;
        item.cantidad = cantidad;
        item.precioUnitario = precioUnitario.setScale(2, RoundingMode.HALF_UP);
        item.subtotal = item.precioUnitario.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
        return item;
    }

    void asignarVenta(Venta venta) {
        this.venta = venta;
    }
}
