package com.inventaya.inventario.kafka;

public final class TiposEvento {

    public static final String VENTA_REALIZADA = "venta-realizada";
    public static final String VENTA_ANULADA = "venta-anulada";
    public static final String STOCK_BAJO = "stock-bajo";

    private TiposEvento() {
    }
}
