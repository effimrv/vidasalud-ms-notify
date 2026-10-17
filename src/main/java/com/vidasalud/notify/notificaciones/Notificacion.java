package com.vidasalud.notify.notificaciones;

import java.time.Instant;

public record Notificacion(Long citaId, String paciente, String canal, String asunto, String mensaje, Instant enviadaEn) {
}
