package com.inventaya.ventas.exception;

public class EventoPublicacionException extends RuntimeException {

    public EventoPublicacionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
