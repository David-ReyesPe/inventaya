package com.inventaya.inventario.entity;

import com.inventaya.inventario.exception.InsufficientStockException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "precio", nullable = false, precision = 14, scale = 2)
    private BigDecimal precio;

    @Column(name = "stock", nullable = false)
    private int stock;

    @Column(name = "umbral_minimo", nullable = false)
    private int umbralMinimo;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    public static Producto crear(String nombre, BigDecimal precio, int stockInicial, int umbralMinimo) {
        Producto producto = new Producto();
        producto.id = UUID.randomUUID();
        producto.nombre = nombre;
        producto.precio = precio;
        producto.stock = stockInicial;
        producto.umbralMinimo = umbralMinimo;
        producto.creadoEn = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        return producto;
    }

    public void incrementarStock(int cantidad) {
        this.stock += cantidad;
    }

    /**
     * @throws InsufficientStockException si el descuento dejaria el stock en negativo.
     */
    public void descontarStock(int cantidad) {
        if (cantidad > this.stock) {
            throw new InsufficientStockException(
                    "Stock insuficiente para el producto " + this.id + ".");
        }
        this.stock -= cantidad;
    }

    public boolean stockPorDebajoDelUmbral() {
        return this.stock < this.umbralMinimo;
    }
}
