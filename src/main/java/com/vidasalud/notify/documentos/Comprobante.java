package com.vidasalud.notify.documentos;

import java.time.Instant;

public record Comprobante(String folio, Long citaId, String paciente, Long servicioId, String contenido, Instant generadoEn) {
}
