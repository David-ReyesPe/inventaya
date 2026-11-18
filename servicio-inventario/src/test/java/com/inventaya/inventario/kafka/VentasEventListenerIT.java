package com.inventaya.inventario.kafka;

import com.inventaya.inventario.entity.Producto;
import com.inventaya.inventario.repository.AlertaStockRepository;
import com.inventaya.inventario.repository.EventoProcesadoRepository;
import com.inventaya.inventario.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Ejercita el consumidor de ventas-topic con los eventos JSON tal como estan definidos en el
 * contrato, contra la base real (H2) y los repositorios reales. Solo se sustituye el productor
 * de alertas, para no necesitar un broker levantado.
 */
@SpringBootTest
@ActiveProfiles("test")
class VentasEventListenerIT {

    @Autowired
    private VentasEventListener listener;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private AlertaStockRepository alertaStockRepository;

    @Autowired
    private EventoProcesadoRepository eventoProcesadoRepository;

    @MockBean
    private AlertaEventProducer alertaEventProducer;

    @BeforeEach
    void limpiarBase() {
        alertaStockRepository.deleteAll();
        eventoProcesadoRepository.deleteAll();
        productoRepository.deleteAll();
    }

    @Test
    @DisplayName("RF3: venta-realizada descuenta el stock de cada item de la venta")
    void ventaRealizadaDescuentaStockDeCadaItem() {
        Producto arroz = productoRepository.save(Producto.crear("Arroz 500g", new BigDecimal("3200.00"), 50, 10));
        Producto aceite = productoRepository.save(Producto.crear("Aceite 1L", new BigDecimal("8000.00"), 20, 5));
        UUID ventaId = UUID.randomUUID();

        listener.escuchar(eventoVentaRealizada(UUID.randomUUID(), ventaId, arroz.getId(), 3, aceite.getId(), 1),
                ventaId.toString());

        assertThat(productoRepository.findById(arroz.getId()).orElseThrow().getStock()).isEqualTo(47);
        assertThat(productoRepository.findById(aceite.getId()).orElseThrow().getStock()).isEqualTo(19);
        verify(alertaEventProducer, never()).publicarStockBajo(any());
    }

    @Test
    @DisplayName("RF4: si el stock queda bajo el umbral se registra la alerta y se publica stock-bajo")
    void stockBajoElUmbralGeneraAlertaYEvento() {
        Producto arroz = productoRepository.save(Producto.crear("Arroz 500g", new BigDecimal("3200.00"), 12, 10));
        UUID ventaId = UUID.randomUUID();

        listener.escuchar(eventoVentaRealizada(UUID.randomUUID(), ventaId, arroz.getId(), 8), ventaId.toString());

        assertThat(productoRepository.findById(arroz.getId()).orElseThrow().getStock()).isEqualTo(4);
        assertThat(alertaStockRepository.findAll())
                .singleElement()
                .satisfies(alerta -> {
                    assertThat(alerta.getProducto().getId()).isEqualTo(arroz.getId());
                    assertThat(alerta.getStockAlMomento()).isEqualTo(4);
                    assertThat(alerta.getUmbral()).isEqualTo(10);
                    assertThat(alerta.isAtendida()).isFalse();
                });
        verify(alertaEventProducer).publicarStockBajo(any());
    }

    @Test
    @DisplayName("Idempotencia: reentregar el mismo eventoId no vuelve a descontar stock")
    void eventoDuplicadoNoSeAplicaDosVeces() {
        Producto arroz = productoRepository.save(Producto.crear("Arroz 500g", new BigDecimal("3200.00"), 50, 10));
        UUID eventoId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();
        String evento = eventoVentaRealizada(eventoId, ventaId, arroz.getId(), 3);

        listener.escuchar(evento, ventaId.toString());
        listener.escuchar(evento, ventaId.toString());

        assertThat(productoRepository.findById(arroz.getId()).orElseThrow().getStock()).isEqualTo(47);
        assertThat(eventoProcesadoRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("RF7: venta-anulada devuelve al stock las unidades de la venta")
    void ventaAnuladaRevierteElStock() {
        Producto arroz = productoRepository.save(Producto.crear("Arroz 500g", new BigDecimal("3200.00"), 50, 10));
        UUID ventaId = UUID.randomUUID();

        listener.escuchar(eventoVentaRealizada(UUID.randomUUID(), ventaId, arroz.getId(), 3), ventaId.toString());
        assertThat(productoRepository.findById(arroz.getId()).orElseThrow().getStock()).isEqualTo(47);

        listener.escuchar(eventoVentaAnulada(UUID.randomUUID(), ventaId, arroz.getId(), 3), ventaId.toString());

        assertThat(productoRepository.findById(arroz.getId()).orElseThrow().getStock()).isEqualTo(50);
        assertThat(eventoProcesadoRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Un mensaje ilegible se descarta sin romper el consumidor")
    void mensajeIlegibleSeDescarta() {
        listener.escuchar("{ esto no es json valido", UUID.randomUUID().toString());

        assertThat(eventoProcesadoRepository.count()).isZero();
        verify(alertaEventProducer, never()).publicarStockBajo(any());
    }

    @Test
    @DisplayName("Un tipo de evento desconocido se ignora")
    void tipoDeEventoDesconocidoSeIgnora() {
        String evento = """
                {
                  "eventoId": "%s",
                  "tipo": "algo-que-inventario-no-maneja",
                  "fecha": "2026-08-19T15:32:00Z",
                  "payload": {}
                }
                """.formatted(UUID.randomUUID());

        listener.escuchar(evento, UUID.randomUUID().toString());

        assertThat(eventoProcesadoRepository.count()).isZero();
    }

    @Test
    @DisplayName("Idempotencia: un evento sin eventoId se descarta")
    void eventoSinEventoIdSeDescarta() {
        Producto arroz = productoRepository.save(Producto.crear("Arroz 500g", new BigDecimal("3200.00"), 50, 10));
        String evento = """
                {
                  "tipo": "venta-realizada",
                  "fecha": "2026-08-19T15:32:00Z",
                  "payload": {
                    "ventaId": "%s",
                    "items": [
                      { "productoId": "%s", "cantidad": 3, "precioUnitario": 3200, "subtotal": 9600 }
                    ],
                    "total": 9600
                  }
                }
                """.formatted(UUID.randomUUID(), arroz.getId());

        listener.escuchar(evento, UUID.randomUUID().toString());

        assertThat(productoRepository.findById(arroz.getId()).orElseThrow().getStock()).isEqualTo(50);
        assertThat(eventoProcesadoRepository.count()).isZero();
    }

    @Test
    @DisplayName("Si la venta pide mas unidades de las disponibles, el stock se deja en cero y no queda negativo")
    void stockNuncaQuedaNegativo() {
        Producto arroz = productoRepository.save(Producto.crear("Arroz 500g", new BigDecimal("3200.00"), 2, 10));
        UUID ventaId = UUID.randomUUID();

        listener.escuchar(eventoVentaRealizada(UUID.randomUUID(), ventaId, arroz.getId(), 5), ventaId.toString());

        assertThat(productoRepository.findById(arroz.getId()).orElseThrow().getStock()).isZero();
        verify(alertaEventProducer, times(1)).publicarStockBajo(any());
    }

    private String eventoVentaRealizada(UUID eventoId, UUID ventaId, UUID productoId, int cantidad) {
        return """
                {
                  "eventoId": "%s",
                  "tipo": "venta-realizada",
                  "fecha": "2026-08-19T15:32:00Z",
                  "payload": {
                    "ventaId": "%s",
                    "items": [
                      { "productoId": "%s", "cantidad": %d, "precioUnitario": 4500, "subtotal": %d }
                    ],
                    "total": %d
                  }
                }
                """.formatted(eventoId, ventaId, productoId, cantidad, 4500 * cantidad, 4500 * cantidad);
    }

    private String eventoVentaRealizada(UUID eventoId, UUID ventaId,
                                        UUID primerProducto, int primeraCantidad,
                                        UUID segundoProducto, int segundaCantidad) {
        return """
                {
                  "eventoId": "%s",
                  "tipo": "venta-realizada",
                  "fecha": "2026-08-19T15:32:00Z",
                  "payload": {
                    "ventaId": "%s",
                    "items": [
                      { "productoId": "%s", "cantidad": %d, "precioUnitario": 4500, "subtotal": %d },
                      { "productoId": "%s", "cantidad": %d, "precioUnitario": 8000, "subtotal": %d }
                    ],
                    "total": %d
                  }
                }
                """.formatted(eventoId, ventaId,
                primerProducto, primeraCantidad, 4500 * primeraCantidad,
                segundoProducto, segundaCantidad, 8000 * segundaCantidad,
                4500 * primeraCantidad + 8000 * segundaCantidad);
    }

    private String eventoVentaAnulada(UUID eventoId, UUID ventaId, UUID productoId, int cantidad) {
        return """
                {
                  "eventoId": "%s",
                  "tipo": "venta-anulada",
                  "fecha": "2026-08-19T16:05:00Z",
                  "payload": {
                    "ventaId": "%s",
                    "motivo": "Cantidad registrada por error, se corrige con una nueva venta.",
                    "items": [
                      { "productoId": "%s", "cantidad": %d }
                    ]
                  }
                }
                """.formatted(eventoId, ventaId, productoId, cantidad);
    }
}
