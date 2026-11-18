package com.inventaya.ventas.mapper;

import com.inventaya.ventas.dto.VentaItemResponse;
import com.inventaya.ventas.dto.VentaResponse;
import com.inventaya.ventas.entity.Venta;
import com.inventaya.ventas.entity.VentaItem;

import java.util.List;

public final class VentaMapper {

    private VentaMapper() {
    }

    public static VentaResponse aResponse(Venta venta) {
        List<VentaItemResponse> items = venta.getItems().stream()
                .map(VentaMapper::aItemResponse)
                .toList();
        return new VentaResponse(
                venta.getId(),
                items,
                venta.getTotal(),
                venta.getEstado().name(),
                venta.getFecha());
    }

    private static VentaItemResponse aItemResponse(VentaItem item) {
        return new VentaItemResponse(
                item.getProductoId(),
                item.getCantidad(),
                item.getPrecioUnitario(),
                item.getSubtotal());
    }
}
