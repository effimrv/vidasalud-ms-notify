package com.vidasalud.notify.actividad;

import com.vidasalud.notify.documentos.Comprobante;
import com.vidasalud.notify.documentos.DocumentoService;
import com.vidasalud.notify.notificaciones.Notificacion;
import com.vidasalud.notify.notificaciones.NotificacionService;
import com.vidasalud.notify.tickets.Ticket;
import com.vidasalud.notify.tickets.TicketService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consulta de lo último que procesaron los consumidores (evidencia de que la mensajería funciona). */
@RestController
@RequestMapping("/api/notify")
public class ActividadController {

    private final NotificacionService notificaciones;
    private final TicketService tickets;
    private final DocumentoService documentos;

    public ActividadController(NotificacionService notificaciones, TicketService tickets, DocumentoService documentos) {
        this.notificaciones = notificaciones;
        this.tickets = tickets;
        this.documentos = documentos;
    }

    @GetMapping("/notificaciones")
    public List<Notificacion> notificaciones() {
        return notificaciones.recientes();
    }

    @GetMapping("/tickets")
    public List<Ticket> tickets() {
        return tickets.recientes();
    }

    @GetMapping("/documentos")
    public List<Comprobante> documentos() {
        return documentos.recientes();
    }
}
