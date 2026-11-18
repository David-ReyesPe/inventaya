package com.inventaya.ventas.repository;

import com.inventaya.ventas.entity.ProductoStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Acceso de solo lectura al catalogo de productos de la base compartida. Extiende Repository
 * (no JpaRepository) a proposito: asi Ventas no tiene ni la posibilidad de escribir sobre
 * datos cuyo dueno es servicio-inventario.
 */
public interface ProductoStockRepository extends Repository<ProductoStock, UUID> {

    /**
     * Lee el stock de un producto tomando un bloqueo de escritura sobre su fila, para que dos
     * ventas concurrentes del mismo producto no validen contra el mismo stock. El servicio
     * recorre los productos de la venta en orden de id, de modo que dos transacciones que
     * comparten productos siempre adquieren los bloqueos en el mismo orden y no se bloquean
     * mutuamente.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductoStock p where p.id = :id")
    Optional<ProductoStock> bloquearPorId(@Param("id") UUID id);
}
