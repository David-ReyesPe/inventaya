package com.inventaya.ventas.repository;

import com.inventaya.ventas.entity.Venta;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

/**
 * Filtros opcionales de GET /api/ventas. Se usan Specifications en lugar de un @Query con
 * "(:desde is null or ...)" para no enviar parametros nulos sin tipo al driver de Postgres.
 */
public final class VentaSpecifications {

    private VentaSpecifications() {
    }

    public static Specification<Venta> enRangoDeFechas(Instant desde, Instant hasta) {
        return (raiz, consulta, cb) -> {
            if (desde != null && hasta != null) {
                return cb.between(raiz.get("fecha"), desde, hasta);
            }
            if (desde != null) {
                return cb.greaterThanOrEqualTo(raiz.get("fecha"), desde);
            }
            if (hasta != null) {
                return cb.lessThanOrEqualTo(raiz.get("fecha"), hasta);
            }
            return cb.conjunction();
        };
    }
}
