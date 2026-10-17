package com.vidasalud.notify.tickets;

import com.rabbitmq.client.Channel;
import com.vidasalud.notify.messaging.ConfirmacionMensajes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Consumidor del dominio tickets: recibe solo las citas confirmadas. */
@Component
public class TicketCitaListener {

    private final ConfirmacionMensajes confirmacion;
    private final TicketService service;

    public TicketCitaListener(ConfirmacionMensajes confirmacion, TicketService service) {
        this.confirmacion = confirmacion;
        this.service = service;
    }

    @RabbitListener(queues = "${vidasalud.mensajeria.colas.tickets.nombre}")
    public void recibir(Message mensaje, Channel canal) throws IOException {
        confirmacion.procesar(mensaje, canal, service::emitirTicket);
    }
}
