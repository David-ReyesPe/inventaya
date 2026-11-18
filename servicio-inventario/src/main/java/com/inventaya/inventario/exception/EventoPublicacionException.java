package com.inventaya.inventario.exception;

public class EventoPublicacionException extends RuntimeException {

    public EventoPublicacionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
