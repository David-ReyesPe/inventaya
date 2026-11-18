package com.inventaya.inventario.service;

import com.inventaya.inventario.dto.OperacionStock;
import com.inventaya.inventario.dto.ProductoInput;
import com.inventaya.inventario.dto.ProductoResponse;
import com.inventaya.inventario.dto.ProductoUpdateInput;
import com.inventaya.inventario.dto.StockUpdateInput;
import com.inventaya.inventario.entity.Producto;
import com.inventaya.inventario.exception.InsufficientStockException;
import com.inventaya.inventario.exception.ResourceNotFoundException;
import com.inventaya.inventario.mapper.ProductoMapper;
import com.inventaya.inventario.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProductoService {

    private static final Logger log = LoggerFactory.getLogger(ProductoService.class);

    private final ProductoRepository productoRepository;
    private final AlertaStockService alertaStockService;

    public ProductoService(ProductoRepository productoRepository, AlertaStockService alertaStockService) {
        this.productoRepository = productoRepository;
        this.alertaStockService = alertaStockService;
    }

    @Transactional
    public ProductoResponse crear(ProductoInput input) {
        Producto producto = productoRepository.save(ProductoMapper.aEntidad(input));
        log.info("Producto registrado: {} ({})", producto.getId(), producto.getNombre());
        return ProductoMapper.aResponse(producto);
    }

    @Transactional(readOnly = true)
    public Page<ProductoResponse> listar(Pageable pageable) {
        return productoRepository.findAll(pageable).map(ProductoMapper::aResponse);
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(UUID id) {
        return ProductoMapper.aResponse(buscarOFallar(id));
    }

    @Transactional
    public ProductoResponse actualizar(UUID id, ProductoUpdateInput input) {
        Producto producto = buscarOFallar(id);
        producto.setNombre(input.nombre().trim());
        producto.setPrecio(input.precio());
        producto.setUmbralMinimo(input.umbralMinimo());

        // Subir el umbral puede dejar al producto por debajo del nuevo minimo.
        alertaStockService.registrarSiCorresponde(producto);
        return ProductoMapper.aResponse(producto);
    }

    /**
     * Ajuste manual de stock (ej. recepcion de mercancia). El descuento por ventas
     * normales no pasa por aqui: lo aplica el consumidor de ventas-topic.
     *
     * @throws InsufficientStockException si un DECREMENTO dejaria el stock negativo (409).
     */
    @Transactional
    public ProductoResponse ajustarStock(UUID id, StockUpdateInput input) {
        Producto producto = productoRepository.buscarParaActualizar(id)
                .orElseThrow(ProductoService::noEncontrado);

        if (input.operacion() == OperacionStock.INCREMENTO) {
            producto.incrementarStock(input.cantidad());
        } else {
            if (input.cantidad() > producto.getStock()) {
                throw new InsufficientStockException("No hay stock suficiente para realizar este ajuste.");
            }
            producto.descontarStock(input.cantidad());
        }
        log.info("Stock ajustado para producto {}: {} {} -> {}",
                producto.getId(), input.operacion(), input.cantidad(), producto.getStock());

        alertaStockService.registrarSiCorresponde(producto);
        return ProductoMapper.aResponse(producto);
    }

    private Producto buscarOFallar(UUID id) {
        return productoRepository.findById(id).orElseThrow(ProductoService::noEncontrado);
    }

    private static ResourceNotFoundException noEncontrado() {
        return new ResourceNotFoundException("No se encontro el producto solicitado.");
    }
}
