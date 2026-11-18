package com.inventaya.inventario.mapper;

import com.inventaya.inventario.dto.AlertaStockResponse;
import com.inventaya.inventario.entity.AlertaStock;

public final class AlertaStockMapper {

    private AlertaStockMapper() {
    }

    public static AlertaStockResponse aResponse(AlertaStock alerta) {
        return new AlertaStockResponse(
                alerta.getId(),
                alerta.getProducto().getId(),
                alerta.getProducto().getNombre(),
                alerta.getStockAlMomento(),
                alerta.getUmbral(),
                alerta.getFecha(),
                alerta.isAtendida());
    }
}
