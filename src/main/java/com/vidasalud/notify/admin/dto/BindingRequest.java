package com.vidasalud.notify.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Enlace entre un exchange y una cola. Se usa tanto para crear como para eliminar un binding. */
public record BindingRequest(
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = NombresRabbit.PATRON_NOMBRE, message = NombresRabbit.MENSAJE_NOMBRE)
        String exchange,

        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = NombresRabbit.PATRON_NOMBRE, message = NombresRabbit.MENSAJE_NOMBRE)
        String cola,

        @NotNull(message = "es obligatoria (puede ser vacía para exchanges fanout)")
        @Pattern(regexp = NombresRabbit.PATRON_ROUTING_KEY, message = NombresRabbit.MENSAJE_ROUTING_KEY)
        String routingKey) {
}
