package com.inventaya.inventario.repository;

import com.inventaya.inventario.entity.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductoRepository extends JpaRepository<Producto, UUID> {

    /**
     * Bloqueo pesimista de la fila del producto: evita que dos ajustes de stock
     * concurrentes (o dos eventos de venta en paralelo) se pisen entre si.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> buscarParaActualizar(@Param("id") UUID id);
}
