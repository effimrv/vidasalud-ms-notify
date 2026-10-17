package com.vidasalud.notify.documentos;

import com.rabbitmq.client.Channel;
import com.vidasalud.notify.messaging.ConfirmacionMensajes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Consumidor del dominio documentos: recibe solo las citas cerradas. */
@Component
public class DocumentoCitaListener {

    private final ConfirmacionMensajes confirmacion;
    private final DocumentoService service;

    public DocumentoCitaListener(ConfirmacionMensajes confirmacion, DocumentoService service) {
        this.confirmacion = confirmacion;
        this.service = service;
    }

    @RabbitListener(queues = "${vidasalud.mensajeria.colas.documentos.nombre}")
    public void recibir(Message mensaje, Channel canal) throws IOException {
        confirmacion.procesar(mensaje, canal, service::generarComprobante);
    }
}
