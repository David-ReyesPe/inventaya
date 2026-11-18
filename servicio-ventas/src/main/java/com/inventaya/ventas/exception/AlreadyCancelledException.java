package com.inventaya.ventas.exception;

public class AlreadyCancelledException extends RuntimeException {

    public AlreadyCancelledException(String mensaje) {
        super(mensaje);
    }
}
