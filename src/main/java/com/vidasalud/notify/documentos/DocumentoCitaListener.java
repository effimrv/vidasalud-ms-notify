package com.vidasalud.notify.documentos;

import com.rabbitmq.client.Channel;
import com.vidasalud.notify.messaging.CitaEvento;
import com.vidasalud.notify.messaging.ConfirmacionMensajes;
import com.vidasalud.notify.messaging.MensajeNoRecuperableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidor del dominio documentos: recibe las citas cerradas y genera el comprobante.
 * <p>
 * Confirmación manual: éxito → basicAck; dato inválido → basicNack sin requeue (DLQ);
 * error transitorio → reintento con contador y, al agotar los intentos, DLQ.
 */
@Component
public class DocumentoCitaListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentoCitaListener.class);

    private final ConfirmacionMensajes confirmacion;
    private final DocumentoService service;

    public DocumentoCitaListener(ConfirmacionMensajes confirmacion, DocumentoService service) {
        this.confirmacion = confirmacion;
        this.service = service;
    }

    @RabbitListener(queues = "${vidasalud.mensajeria.colas.documentos.nombre}")
    public void recibir(Message mensaje, Channel canal, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        String cola = mensaje.getMessageProperties().getConsumerQueue();
        try {
            CitaEvento evento = confirmacion.leerEvento(mensaje);
            service.generarComprobante(evento);
            canal.basicAck(deliveryTag, false);
            log.info("[{}] ACK evento {} de la cita {} (intento {}/{})", cola, evento.tipo(), evento.citaId(),
                    confirmacion.intentoActual(mensaje), confirmacion.maximoIntentos());
        } catch (MensajeNoRecuperableException e) {
            canal.basicNack(deliveryTag, false, false);
            log.error("[{}] NACK no recuperable → DLQ. Motivo: {}", cola, e.getMessage());
        } catch (RuntimeException e) {
            confirmacion.reintentarOEnviarADlq(mensaje, canal, deliveryTag, e);
        }
    }
}
