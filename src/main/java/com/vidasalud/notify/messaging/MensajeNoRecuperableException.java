package com.vidasalud.notify.messaging;

/**
 * Error que no se arregla reintentando (JSON inválido, datos obligatorios ausentes).
 * El mensaje se rechaza sin reencolar y va directo a la DLQ.
 * Cualquier otra excepción se considera transitoria y se reintenta.
 */
public class MensajeNoRecuperableException extends RuntimeException {
    public MensajeNoRecuperableException(String mensaje) {
        super(mensaje);
    }

    public MensajeNoRecuperableException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
