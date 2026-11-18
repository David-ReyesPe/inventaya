package com.inventaya.ventas.entity;

import com.inventaya.ventas.exception.AlreadyCancelledException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Una venta con una o varias lineas. Es inmutable desde el punto de vista del negocio:
 * lo unico que puede cambiar tras su registro es pasar a estado ANULADA.
 */
@Entity
@Table(name = "ventas")
@Getter
@NoArgsConstructor
public class Venta {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoVenta estado;

    @Column(name = "motivo_anulacion", length = 500)
    private String motivoAnulacion;

    @Column(name = "fecha", nullable = false)
    private Instant fecha;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<VentaItem> items = new ArrayList<>();

    /**
     * @param lineas productoId -> (cantidad, precioUnitario) ya validadas por el servicio.
     */
    public static Venta registrar(List<LineaNueva> lineas) {
        Venta venta = new Venta();
        venta.id = UUID.randomUUID();
        venta.estado = EstadoVenta.REGISTRADA;
        venta.fecha = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        for (LineaNueva linea : lineas) {
            VentaItem item = VentaItem.de(linea.productoId(), linea.cantidad(), linea.precioUnitario());
            item.asignarVenta(venta);
            venta.items.add(item);
        }
        venta.total = venta.items.stream()
                .map(VentaItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        return venta;
    }

    /**
     * @throws AlreadyCancelledException si la venta ya estaba anulada (409).
     */
    public void anular(String motivo) {
        if (estado == EstadoVenta.ANULADA) {
            throw new AlreadyCancelledException("La venta ya fue anulada previamente.");
        }
        this.estado = EstadoVenta.ANULADA;
        this.motivoAnulacion = motivo;
    }

    public List<VentaItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public record LineaNueva(UUID productoId, int cantidad, BigDecimal precioUnitario) {
    }
}
