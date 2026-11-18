package com.inventaya.inventario.repository;

import com.inventaya.inventario.entity.AlertaStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlertaStockRepository extends JpaRepository<AlertaStock, UUID> {

    @EntityGraph(attributePaths = "producto")
    Page<AlertaStock> findByAtendida(boolean atendida, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "producto")
    Page<AlertaStock> findAll(Pageable pageable);
}
