package com.vidasalud.notify.tickets;

import com.vidasalud.notify.actividad.RegistroActividad;
import com.vidasalud.notify.messaging.CitaEvento;
import com.vidasalud.notify.messaging.MensajeNoRecuperableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Emite el ticket (número de atención) cuando la cita se confirma. Es idempotente por cita. */
@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final AtomicInteger correlativo = new AtomicInteger();
    private final Map<Long, Ticket> ticketsPorCita = new ConcurrentHashMap<>();
    private final RegistroActividad<Ticket> registro = new RegistroActividad<>();

    public void emitirTicket(CitaEvento evento) {
        if (evento.servicioId() == null) {
            throw new MensajeNoRecuperableException("No se puede emitir un ticket sin servicioId (cita " + evento.citaId() + ")");
        }
        Ticket existente = ticketsPorCita.get(evento.citaId());
        if (existente != null) {
            log.info("La cita {} ya tiene el ticket {}, se ignora el duplicado", evento.citaId(), existente.numero());
            return;
        }
        Ticket ticket = new Ticket(String.format("T-%04d", correlativo.incrementAndGet()), evento.citaId(),
                evento.pacienteNombre(), evento.servicioId(), evento.boxId(), Instant.now());
        ticketsPorCita.put(evento.citaId(), ticket);
        registro.agregar(ticket);
        log.info("Ticket {} emitido para la cita {} ({})", ticket.numero(), ticket.citaId(), ticket.paciente());
    }

    public List<Ticket> recientes() {
        return registro.recientes();
    }
}
