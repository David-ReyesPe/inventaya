package com.inventaya.inventario.mapper;

import com.inventaya.inventario.dto.ProductoInput;
import com.inventaya.inventario.dto.ProductoResponse;
import com.inventaya.inventario.entity.Producto;

public final class ProductoMapper {

    private ProductoMapper() {
    }

    public static Producto aEntidad(ProductoInput input) {
        return Producto.crear(
                input.nombre().trim(),
                input.precio(),
                input.stockInicial(),
                input.umbralMinimo());
    }

    public static ProductoResponse aResponse(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getPrecio(),
                producto.getStock(),
                producto.getUmbralMinimo());
    }
}
