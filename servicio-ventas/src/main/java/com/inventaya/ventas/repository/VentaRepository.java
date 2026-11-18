package com.inventaya.ventas.repository;

import com.inventaya.ventas.entity.Venta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface VentaRepository extends JpaRepository<Venta, UUID>, JpaSpecificationExecutor<Venta> {

    @EntityGraph(attributePaths = "items")
    Optional<Venta> findWithItemsById(UUID id);
}
