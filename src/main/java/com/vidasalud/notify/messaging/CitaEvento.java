package com.vidasalud.notify.messaging;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Evento publicado por ms-appointments cuando se crea una cita o cambia su estado.
 *
 * @param tipo       CREADA, CONFIRMADA, EN_ESPERA, EN_ATENCION, CERRADA o CANCELADA
 * @param fechaHora  fecha y hora agendada de la cita
 * @param ocurridoEn instante en que se generó el evento
 */
public record CitaEvento(
        String tipo,
        Long citaId,
        String pacienteNombre,
        Long servicioId,
        Long boxId,
        String estado,
        LocalDateTime fechaHora,
        Instant ocurridoEn) {
}
