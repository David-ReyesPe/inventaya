package com.inventaya.ventas.repository;

import com.inventaya.ventas.entity.Venta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba contra una base real (H2) el filtro de fechas y la paginacion de GET /api/ventas,
 * ademas del mapeo de la venta con sus lineas.
 */
@DataJpaTest
@ActiveProfiles("test")
class VentaRepositoryIT {

    private static final UUID ARROZ = UUID.fromString("b3f1c2a0-1234-4a56-9abc-1234567890ab");

    @Autowired
    private VentaRepository ventaRepository;

    @Test
    @DisplayName("El filtro de fechas devuelve solo las ventas dentro del rango")
    void filtraVentasPorRangoDeFechas() {
        Venta venta = ventaRepository.save(nuevaVenta());
        Instant fecha = venta.getFecha();

        Page<Venta> dentroDelRango = ventaRepository.findAll(
                VentaSpecifications.enRangoDeFechas(fecha.minus(1, ChronoUnit.HOURS), fecha.plus(1, ChronoUnit.HOURS)),
                PageRequest.of(0, 20));
        Page<Venta> fueraDelRango = ventaRepository.findAll(
                VentaSpecifications.enRangoDeFechas(fecha.plus(1, ChronoUnit.HOURS), null),
                PageRequest.of(0, 20));

        assertThat(dentroDelRango.getTotalElements()).isEqualTo(1);
        assertThat(fueraDelRango.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Sin filtros de fecha devuelve todas las ventas de la pagina solicitada")
    void sinFiltrosDevuelveTodasLasVentasPaginadas() {
        ventaRepository.save(nuevaVenta());
        ventaRepository.save(nuevaVenta());
        ventaRepository.save(nuevaVenta());

        Page<Venta> primeraPagina = ventaRepository.findAll(
                VentaSpecifications.enRangoDeFechas(null, null),
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "fecha")));

        assertThat(primeraPagina.getTotalElements()).isEqualTo(3);
        assertThat(primeraPagina.getTotalPages()).isEqualTo(2);
        assertThat(primeraPagina.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("La venta se recupera con todas sus lineas y su total calculado")
    void recuperaVentaConSusLineas() {
        UUID id = ventaRepository.save(nuevaVenta()).getId();

        Venta encontrada = ventaRepository.findWithItemsById(id).orElseThrow();

        assertThat(encontrada.getItems()).hasSize(2);
        assertThat(encontrada.getTotal()).isEqualByComparingTo("21500");
    }

    private Venta nuevaVenta() {
        return Venta.registrar(List.of(
                new Venta.LineaNueva(ARROZ, 3, new BigDecimal("4500")),
                new Venta.LineaNueva(UUID.randomUUID(), 1, new BigDecimal("8000"))));
    }
}
