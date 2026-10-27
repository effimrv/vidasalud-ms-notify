package com.vidasalud.notify.notificaciones;

import com.vidasalud.notify.actividad.RegistroActividad;
import com.vidasalud.notify.messaging.CitaEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/** Avisa al paciente de cada cambio en su cita. El envío de correo está simulado con un log. */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    /** Cambios de estado que avisan por correo; el resto solo queda en el registro. */
    private static final Set<String> CON_CORREO = Set.of("CREADA", "CONFIRMADA");

    private final RegistroActividad<Notificacion> registro = new RegistroActividad<>();
    private final String marcaFallaTransitoria;
    private final CorreoReservas correoReservas;

    public NotificacionService(@Value("${vidasalud.simulacion.falla-transitoria-paciente}") String marcaFallaTransitoria,
                               CorreoReservas correoReservas) {
        this.marcaFallaTransitoria = marcaFallaTransitoria;
        this.correoReservas = correoReservas;
    }

    public void notificar(CitaEvento evento) {
        String paciente = evento.pacienteNombre() == null ? "Paciente" : evento.pacienteNombre();
        if (paciente.contains(marcaFallaTransitoria)) {
            throw new IllegalStateException("Servidor de correo no disponible (falla simulada)");
        }

        String asunto = "VidaSalud: tu cita #" + evento.citaId() + " " + describir(evento.tipo());
        String fecha = evento.fechaHora() == null ? "" : " para el " + evento.fechaHora().format(FORMATO_FECHA);
        String mensaje = "Hola " + paciente + ", tu cita #" + evento.citaId() + fecha + " " + describir(evento.tipo()) + ".";

        if (CON_CORREO.contains(evento.tipo())) {
            // Se envía antes de registrar: si el SMTP falla, el reintento no deja una notificación duplicada.
            correoReservas.enviar(evento.pacienteCorreo(), asunto,
                    PlantillaCorreoReserva.texto(evento, paciente),
                    PlantillaCorreoReserva.html(evento, paciente));
            log.info("Correo enviado a {}: {}", paciente, asunto);
        }

        registro.agregar(new Notificacion(evento.citaId(), paciente, "EMAIL", asunto, mensaje, Instant.now()));
    }

    public List<Notificacion> recientes() {
        return registro.recientes();
    }

    private String describir(String tipo) {
        return switch (tipo) {
            case "CREADA" -> "fue solicitada";
            case "CONFIRMADA" -> "fue confirmada";
            case "EN_ESPERA" -> "está en sala de espera";
            case "EN_ATENCION" -> "está en atención";
            case "CERRADA" -> "fue completada";
            case "CANCELADA" -> "fue cancelada";
            default -> "cambió de estado (" + tipo + ")";
        };
    }
}
