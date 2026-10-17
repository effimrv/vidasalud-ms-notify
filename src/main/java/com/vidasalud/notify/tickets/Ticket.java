package com.vidasalud.notify.tickets;

import java.time.Instant;

public record Ticket(String numero, Long citaId, String paciente, Long servicioId, Long boxId, Instant emitidoEn) {
}
