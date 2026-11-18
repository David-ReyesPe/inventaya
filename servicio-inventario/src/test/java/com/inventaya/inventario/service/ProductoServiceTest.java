package com.inventaya.inventario.service;

import com.inventaya.inventario.dto.OperacionStock;
import com.inventaya.inventario.dto.ProductoResponse;
import com.inventaya.inventario.dto.StockUpdateInput;
import com.inventaya.inventario.entity.Producto;
import com.inventaya.inventario.exception.InsufficientStockException;
import com.inventaya.inventario.exception.ResourceNotFoundException;
import com.inventaya.inventario.repository.ProductoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private AlertaStockService alertaStockService;

    @InjectMocks
    private ProductoService productoService;

    @Test
    @DisplayName("Un DECREMENTO mayor al stock disponible se rechaza y no genera alerta")
    void decrementoMayorAlStockDisponibleSeRechaza() {
        Producto producto = productoConStock(5, 10);
        when(productoRepository.buscarParaActualizar(producto.getId())).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> productoService.ajustarStock(producto.getId(),
                new StockUpdateInput(OperacionStock.DECREMENTO, 6)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("No hay stock suficiente para realizar este ajuste.");

        assertThat(producto.getStock()).isEqualTo(5);
        verify(alertaStockService, never()).registrarSiCorresponde(any());
    }

    @Test
    @DisplayName("Un DECREMENTO que deja el stock bajo el umbral dispara la evaluacion de alerta")
    void decrementoQueDejaElStockBajoElUmbralEvaluaAlerta() {
        Producto producto = productoConStock(12, 10);
        when(productoRepository.buscarParaActualizar(producto.getId())).thenReturn(Optional.of(producto));

        ProductoResponse respuesta = productoService.ajustarStock(producto.getId(),
                new StockUpdateInput(OperacionStock.DECREMENTO, 8));

        assertThat(respuesta.stock()).isEqualTo(4);
        assertThat(producto.stockPorDebajoDelUmbral()).isTrue();
        verify(alertaStockService).registrarSiCorresponde(producto);
    }

    @Test
    @DisplayName("Un INCREMENTO suma unidades al stock disponible")
    void incrementoSumaUnidades() {
        Producto producto = productoConStock(50, 10);
        when(productoRepository.buscarParaActualizar(producto.getId())).thenReturn(Optional.of(producto));

        ProductoResponse respuesta = productoService.ajustarStock(producto.getId(),
                new StockUpdateInput(OperacionStock.INCREMENTO, 30));

        assertThat(respuesta.stock()).isEqualTo(80);
    }

    @Test
    @DisplayName("Ajustar el stock de un producto inexistente devuelve 404 de dominio")
    void ajustarStockDeProductoInexistenteFalla() {
        UUID idDesconocido = UUID.randomUUID();
        when(productoRepository.buscarParaActualizar(idDesconocido)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.ajustarStock(idDesconocido,
                new StockUpdateInput(OperacionStock.INCREMENTO, 1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No se encontro el producto solicitado.");
    }

    private Producto productoConStock(int stock, int umbralMinimo) {
        return Producto.crear("Arroz 500g", new BigDecimal("3200.00"), stock, umbralMinimo);
    }
}
