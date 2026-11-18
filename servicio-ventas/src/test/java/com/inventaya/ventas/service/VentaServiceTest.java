package com.inventaya.ventas.service;

import com.inventaya.ventas.dto.AnulacionInput;
import com.inventaya.ventas.dto.VentaInput;
import com.inventaya.ventas.dto.VentaItemInput;
import com.inventaya.ventas.dto.VentaResponse;
import com.inventaya.ventas.entity.EstadoVenta;
import com.inventaya.ventas.entity.ProductoStock;
import com.inventaya.ventas.entity.Venta;
import com.inventaya.ventas.exception.AlreadyCancelledException;
import com.inventaya.ventas.exception.InsufficientStockException;
import com.inventaya.ventas.exception.ResourceNotFoundException;
import com.inventaya.ventas.exception.ValidacionNegocioException;
import com.inventaya.ventas.kafka.VentaEventProducer;
import com.inventaya.ventas.repository.ProductoStockRepository;
import com.inventaya.ventas.repository.VentaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    private static final UUID ARROZ = UUID.fromString("b3f1c2a0-1234-4a56-9abc-1234567890ab");
    private static final UUID ACEITE = UUID.fromString("a9d8e7f6-4321-4b65-8fed-0987654321cd");

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private ProductoStockRepository productoStockRepository;

    @Mock
    private VentaEventProducer ventaEventProducer;

    @InjectMocks
    private VentaService ventaService;

    @Test
    @DisplayName("Una venta sin stock suficiente se rechaza completa: no se persiste ni se publica evento")
    void ventaConStockInsuficienteSeRechaza() {
        ProductoStock arrozConDosUnidades = productoConStock(2);
        when(productoStockRepository.bloquearPorId(ARROZ)).thenReturn(Optional.of(arrozConDosUnidades));

        VentaInput input = new VentaInput(List.of(new VentaItemInput(ARROZ, 3, new BigDecimal("4500"))));

        assertThatThrownBy(() -> ventaService.registrar(input))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Stock insuficiente para el producto " + ARROZ + ".");

        verify(ventaRepository, never()).save(any());
        verifyNoInteractions(ventaEventProducer);
    }

    @Test
    @DisplayName("El stock se valida sumando las unidades de todas las lineas del mismo producto")
    void variasLineasDelMismoProductoSeValidanSumadas() {
        ProductoStock arrozConCincoUnidades = productoConStock(5);
        when(productoStockRepository.bloquearPorId(ARROZ)).thenReturn(Optional.of(arrozConCincoUnidades));

        VentaInput input = new VentaInput(List.of(
                new VentaItemInput(ARROZ, 3, new BigDecimal("4500")),
                new VentaItemInput(ARROZ, 3, new BigDecimal("4500"))));

        assertThatThrownBy(() -> ventaService.registrar(input))
                .isInstanceOf(InsufficientStockException.class);

        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una venta con un producto que no existe en el catalogo se rechaza con error de validacion")
    void ventaConProductoInexistenteSeRechaza() {
        when(productoStockRepository.bloquearPorId(ARROZ)).thenReturn(Optional.empty());

        VentaInput input = new VentaInput(List.of(new VentaItemInput(ARROZ, 1, new BigDecimal("4500"))));

        assertThatThrownBy(() -> ventaService.registrar(input))
                .isInstanceOf(ValidacionNegocioException.class)
                .hasMessage("El producto " + ARROZ + " no existe en el catalogo.");

        verifyNoInteractions(ventaEventProducer);
    }

    @Test
    @DisplayName("Una venta valida calcula subtotales y total, y publica venta-realizada")
    void ventaValidaCalculaTotalesYPublicaEvento() {
        ProductoStock conStockSuficiente = productoConStock(50);
        when(productoStockRepository.bloquearPorId(any())).thenReturn(Optional.of(conStockSuficiente));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        VentaResponse respuesta = ventaService.registrar(new VentaInput(List.of(
                new VentaItemInput(ARROZ, 3, new BigDecimal("4500")),
                new VentaItemInput(ACEITE, 1, new BigDecimal("8000")))));

        assertThat(respuesta.estado()).isEqualTo("REGISTRADA");
        assertThat(respuesta.total()).isEqualByComparingTo("21500");
        assertThat(respuesta.items()).hasSize(2);
        assertThat(respuesta.items().get(0).subtotal()).isEqualByComparingTo("13500");
        assertThat(respuesta.items().get(1).subtotal()).isEqualByComparingTo("8000");
        verify(ventaEventProducer).publicarVentaRealizada(any(Venta.class));
    }

    @Test
    @DisplayName("Anular una venta la marca como ANULADA y publica venta-anulada")
    void anularVentaPublicaEvento() {
        Venta venta = ventaRegistrada();
        when(ventaRepository.findWithItemsById(venta.getId())).thenReturn(Optional.of(venta));

        VentaResponse respuesta = ventaService.anular(venta.getId(),
                new AnulacionInput("Cantidad registrada por error, se corrige con una nueva venta."));

        assertThat(respuesta.estado()).isEqualTo("ANULADA");
        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.ANULADA);
        assertThat(venta.getMotivoAnulacion()).isEqualTo("Cantidad registrada por error, se corrige con una nueva venta.");
        verify(ventaEventProducer).publicarVentaAnulada(venta);
    }

    @Test
    @DisplayName("Anular una venta ya anulada no vuelve a publicar el evento")
    void anularVentaYaAnuladaFalla() {
        Venta venta = ventaRegistrada();
        venta.anular("Anulada en una solicitud anterior.");
        when(ventaRepository.findWithItemsById(venta.getId())).thenReturn(Optional.of(venta));

        assertThatThrownBy(() -> ventaService.anular(venta.getId(), new AnulacionInput("Motivo del segundo intento")))
                .isInstanceOf(AlreadyCancelledException.class)
                .hasMessage("La venta ya fue anulada previamente.");

        verifyNoInteractions(ventaEventProducer);
    }

    @Test
    @DisplayName("Consultar una venta inexistente lanza el error de recurso no encontrado")
    void consultarVentaInexistenteFalla() {
        UUID idDesconocido = UUID.randomUUID();
        when(ventaRepository.findWithItemsById(idDesconocido)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ventaService.obtener(idDesconocido))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No se encontro la venta solicitada.");
    }

    @Test
    @DisplayName("Un rango de fechas invertido se rechaza antes de consultar la base")
    void rangoDeFechasInvertidoSeRechaza() {
        Instant desde = Instant.parse("2026-08-31T00:00:00Z");
        Instant hasta = Instant.parse("2026-08-01T00:00:00Z");

        assertThatThrownBy(() -> ventaService.listar(desde, hasta, null))
                .isInstanceOf(ValidacionNegocioException.class)
                .hasMessageContaining("El rango de fechas es invalido");

        verifyNoInteractions(ventaRepository);
    }

    private ProductoStock productoConStock(int stock) {
        ProductoStock producto = mock(ProductoStock.class);
        when(producto.getStock()).thenReturn(stock);
        return producto;
    }

    private Venta ventaRegistrada() {
        return Venta.registrar(List.of(
                new Venta.LineaNueva(ARROZ, 3, new BigDecimal("4500")),
                new Venta.LineaNueva(ACEITE, 1, new BigDecimal("8000"))));
    }
}
