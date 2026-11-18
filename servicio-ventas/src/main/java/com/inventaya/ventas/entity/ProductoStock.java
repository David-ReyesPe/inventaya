package com.inventaya.ventas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Vista de solo lectura de la tabla productos, que vive en la base compartida inventaya_db.
 *
 * Ventas necesita conocer el stock para poder rechazar con 409 una venta que no se puede
 * cumplir; el dueno de esos datos sigue siendo servicio-inventario, que es el unico que los
 * modifica. Por eso el repositorio asociado solo expone consultas (ver ProductoStockRepository)
 * y esta clase no mapea columnas que Ventas no necesita.
 */
@Entity
@Table(name = "productos")
@Getter
@NoArgsConstructor
public class ProductoStock {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "stock", nullable = false)
    private int stock;
}
