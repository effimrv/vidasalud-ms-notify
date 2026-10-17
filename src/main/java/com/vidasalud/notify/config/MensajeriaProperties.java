package com.vidasalud.notify.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.stream.Stream;

/**
 * Nombres de exchanges, colas, DLQ y routing keys, leídos desde {@code vidasalud.mensajeria} en application.yml.
 * Ninguna otra clase escribe nombres de RabbitMQ a mano: todas los obtienen de aquí.
 */
@Validated
@ConfigurationProperties(prefix = "vidasalud.mensajeria")
public record MensajeriaProperties(
        @Valid @NotNull Exchanges exchanges,
        @Valid @NotNull Colas colas,
        @Valid @NotNull Reintentos reintentos,
        @Min(1) int prefetch) {

    public record Exchanges(@NotBlank String citas, @NotBlank String deadLetter) {}

    public record Colas(@Valid @NotNull Cola notificaciones, @Valid @NotNull Cola tickets, @Valid @NotNull Cola documentos) {
        public List<Cola> todas() {
            return List.of(notificaciones, tickets, documentos);
        }
    }

    /**
     * @param nombre        cola principal
     * @param patron        patrón de binding contra el exchange topic de citas
     * @param dlq           cola de mensajes muertos asociada
     * @param dlqRoutingKey routing key con la que el exchange de dead-letter llega a la DLQ
     */
    public record Cola(@NotBlank String nombre, @NotBlank String patron, @NotBlank String dlq, @NotBlank String dlqRoutingKey) {}

    public record Reintentos(@Min(1) int maximo, @NotBlank String header) {}

    /** Exchanges y colas propios del sistema, que la API de administración no permite eliminar. */
    public List<String> recursosProtegidos() {
        return Stream.concat(
                Stream.of(exchanges.citas(), exchanges.deadLetter()),
                colas.todas().stream().flatMap(c -> Stream.of(c.nombre(), c.dlq()))
        ).toList();
    }
}
