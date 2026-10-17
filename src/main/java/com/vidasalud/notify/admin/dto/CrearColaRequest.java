package com.vidasalud.notify.admin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * @param nombre                  nombre de la cola
 * @param durable                 si sobrevive a reinicios del broker (por defecto true)
 * @param exchangeDeadLetter      exchange al que se envían los mensajes rechazados (opcional)
 * @param routingKeyDeadLetter    routing key para el dead-letter (opcional, requiere exchangeDeadLetter)
 * @param ttlMensajesMs           tiempo de vida de los mensajes en milisegundos (opcional)
 * @param largoMaximo             cantidad máxima de mensajes en la cola (opcional)
 */
public record CrearColaRequest(
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = NombresRabbit.PATRON_NOMBRE, message = NombresRabbit.MENSAJE_NOMBRE)
        String nombre,

        Boolean durable,

        @Pattern(regexp = NombresRabbit.PATRON_NOMBRE, message = NombresRabbit.MENSAJE_NOMBRE)
        String exchangeDeadLetter,

        @Pattern(regexp = NombresRabbit.PATRON_ROUTING_KEY, message = NombresRabbit.MENSAJE_ROUTING_KEY)
        String routingKeyDeadLetter,

        @Positive(message = "debe ser mayor que 0")
        @Max(value = 604_800_000, message = "no puede superar 7 días (604800000 ms)")
        Integer ttlMensajesMs,

        @Positive(message = "debe ser mayor que 0")
        @Max(value = 1_000_000, message = "no puede superar 1.000.000 mensajes")
        Integer largoMaximo) {

    public boolean esDurable() {
        return durable == null || durable;
    }

    @AssertTrue(message = "routingKeyDeadLetter requiere indicar exchangeDeadLetter")
    public boolean isDeadLetterConsistente() {
        return routingKeyDeadLetter == null || exchangeDeadLetter != null;
    }
}
