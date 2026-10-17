package com.vidasalud.notify.notificaciones;

import com.rabbitmq.client.Channel;
import com.vidasalud.notify.messaging.ConfirmacionMensajes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Consumidor del dominio notificaciones: recibe todos los eventos de cita (patrón cita.#). */
@Component
public class NotificacionCitaListener {

    private final ConfirmacionMensajes confirmacion;
    private final NotificacionService service;

    public NotificacionCitaListener(ConfirmacionMensajes confirmacion, NotificacionService service) {
        this.confirmacion = confirmacion;
        this.service = service;
    }

    @RabbitListener(queues = "${vidasalud.mensajeria.colas.notificaciones.nombre}")
    public void recibir(Message mensaje, Channel canal) throws IOException {
        confirmacion.procesar(mensaje, canal, service::notificar);
    }
}
