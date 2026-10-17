package com.vidasalud.notify.admin.dto;

/** Reglas de nombres compartidas por los DTO y las variables de ruta de la API de administración. */
public final class NombresRabbit {

    /** 3 a 120 caracteres [A-Za-z0-9._-], sin el prefijo "amq." que RabbitMQ reserva para sí mismo. */
    public static final String PATRON_NOMBRE = "^(?!amq\\.)[A-Za-z0-9._-]{3,120}$";
    public static final String MENSAJE_NOMBRE =
            "debe tener entre 3 y 120 caracteres (letras, números, '.', '_' o '-') y no puede empezar con 'amq.'";

    /** Routing key o patrón topic: palabras separadas por puntos, con comodines * y #. Puede ir vacía (fanout). */
    public static final String PATRON_ROUTING_KEY = "^[A-Za-z0-9._*#-]{0,255}$";
    public static final String MENSAJE_ROUTING_KEY =
            "solo admite letras, números, '.', '_', '-', '*' y '#' (máximo 255 caracteres)";

    private NombresRabbit() {
    }
}
