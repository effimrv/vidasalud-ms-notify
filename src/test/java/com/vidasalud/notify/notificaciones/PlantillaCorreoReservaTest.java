package com.vidasalud.notify.notificaciones;

import com.vidasalud.notify.messaging.CitaEvento;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PlantillaCorreoReservaTest {

    private static CitaEvento reserva(String paciente) {
        return new CitaEvento("CREADA", 42L, paciente, "paciente@example.com", 3L, 7L, "SOLICITADA",
                LocalDateTime.of(2026, 11, 2, 9, 30), Instant.now());
    }

    @Test
    void htmlIncluyeDatosDeLaReserva() {
        String html = PlantillaCorreoReserva.html(reserva("Ana Perez"), "Ana Perez");

        assertThat(html).contains("Tu cita #42", "Ana Perez", "02-11-2026 09:30", "N&deg; 3", "N&deg; 7");
    }

    @Test
    void escapaElNombreDelPacienteParaNoInyectarHtml() {
        String html = PlantillaCorreoReserva.html(reserva("<script>x</script>"), "<script>x</script>");

        assertThat(html).doesNotContain("<script>").contains("&lt;script&gt;");
    }

    @Test
    void sinFechaMuestraPorConfirmar() {
        CitaEvento sinFecha = new CitaEvento("CREADA", 1L, "Luis", "luis@example.com", 1L, 1L, null, null, Instant.now());

        assertThat(PlantillaCorreoReserva.texto(sinFecha, "Luis")).contains("Por confirmar");
        assertThat(PlantillaCorreoReserva.html(sinFecha, "Luis")).contains("Por confirmar", "Solicitada");
    }
}
