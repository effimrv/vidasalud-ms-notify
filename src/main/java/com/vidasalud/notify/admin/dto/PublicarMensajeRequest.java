package com.vidasalud.notify.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Mensaje de prueba para demostrar el flujo: por ejemplo, un cuerpo sin citaId termina en la DLQ.
 *
 * @param cuerpo texto (normalmente JSON) que se publica tal cual
 */
public record PublicarMensajeRequest(
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = NombresRabbit.PATRON_NOMBRE, message = NombresRabbit.MENSAJE_NOMBRE)
        String exchange,

        @NotNull(message = "es obligatoria")
        @Pattern(regexp = NombresRabbit.PATRON_ROUTING_KEY, message = NombresRabbit.MENSAJE_ROUTING_KEY)
        String routingKey,

        @NotNull(message = "es obligatorio")
        @Size(max = 10_000, message = "no puede superar 10.000 caracteres")
        String cuerpo) {
}
