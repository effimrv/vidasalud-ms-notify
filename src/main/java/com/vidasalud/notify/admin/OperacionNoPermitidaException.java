package com.vidasalud.notify.admin;

/** La operación es válida en forma pero choca con el estado del broker o con un recurso protegido. */
public class OperacionNoPermitidaException extends RuntimeException {
    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }

    public OperacionNoPermitidaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
