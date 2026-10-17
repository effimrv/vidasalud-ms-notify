package com.vidasalud.notify.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.vidasalud.notify.config.MensajeriaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Política única de confirmación para todos los consumidores (ack manual):
 * <ul>
 *   <li><b>Éxito</b> → {@code basicAck}.</li>
 *   <li><b>Error no recuperable</b> ({@link MensajeNoRecuperableException}: JSON inválido o datos faltantes)
 *       → {@code basicNack} sin requeue → el broker lo desvía a la DLQ de la cola.</li>
 *   <li><b>Error transitorio</b> (cualquier otra excepción) → se republica en la misma cola con el header
 *       de reintentos incrementado y se hace ack del original. Al alcanzar el máximo de intentos
 *       → {@code basicNack} sin requeue → DLQ.</li>
 *   <li>Si ni siquiera se puede republicar (broker caído) → {@code basicNack} con requeue, para no perder el mensaje.</li>
 * </ul>
 * Todo rechazo hacia la DLQ queda registrado en el log con nivel ERROR.
 */
@Component
public class ConfirmacionMensajes {

    private static final Logger log = LoggerFactory.getLogger(ConfirmacionMensajes.class);

    /** Exchange por defecto de AMQP: enruta directamente a la cola cuyo nombre coincide con la routing key. */
    private static final String EXCHANGE_POR_DEFECTO = "";

    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final MensajeriaProperties props;

    public ConfirmacionMensajes(ObjectMapper objectMapper, RabbitTemplate rabbitTemplate, MensajeriaProperties props) {
        this.objectMapper = objectMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    public void procesar(Message mensaje, Channel canal, Consumer<CitaEvento> accion) throws IOException {
        long tag = mensaje.getMessageProperties().getDeliveryTag();
        String cola = mensaje.getMessageProperties().getConsumerQueue();
        int intento = intentosPrevios(mensaje) + 1;
        int maximo = props.reintentos().maximo();

        try {
            CitaEvento evento = convertir(mensaje);
            accion.accept(evento);
            canal.basicAck(tag, false);
            log.info("[{}] ACK evento {} de la cita {} (intento {}/{})", cola, evento.tipo(), evento.citaId(), intento, maximo);
        } catch (MensajeNoRecuperableException e) {
            canal.basicNack(tag, false, false);
            log.error("[{}] NACK no recuperable → DLQ. Motivo: {}. Cuerpo: {}", cola, e.getMessage(), cuerpo(mensaje));
        } catch (Exception e) {
            if (intento >= maximo) {
                canal.basicNack(tag, false, false);
                log.error("[{}] NACK tras agotar {} intentos → DLQ. Último error: {}", cola, maximo, e.getMessage());
                return;
            }
            try {
                Message reintento = MessageBuilder.fromMessage(mensaje)
                        .setHeader(props.reintentos().header(), intento)
                        .build();
                rabbitTemplate.send(EXCHANGE_POR_DEFECTO, cola, reintento);
                canal.basicAck(tag, false);
                log.warn("[{}] Error transitorio en intento {}/{}, se reintenta: {}", cola, intento, maximo, e.getMessage());
            } catch (AmqpException errorRepublicando) {
                canal.basicNack(tag, false, true);
                log.error("[{}] No se pudo republicar para reintento, se devuelve a la cola: {}", cola, errorRepublicando.getMessage());
            }
        }
    }

    private CitaEvento convertir(Message mensaje) {
        CitaEvento evento;
        try {
            evento = objectMapper.readValue(mensaje.getBody(), CitaEvento.class);
        } catch (IOException e) {
            throw new MensajeNoRecuperableException("JSON inválido para CitaEvento", e);
        }
        if (evento == null || evento.citaId() == null || evento.tipo() == null || evento.tipo().isBlank()) {
            throw new MensajeNoRecuperableException("El evento no trae citaId o tipo");
        }
        return evento;
    }

    private int intentosPrevios(Message mensaje) {
        Object valor = mensaje.getMessageProperties().getHeader(props.reintentos().header());
        return valor instanceof Number n ? n.intValue() : 0;
    }

    private String cuerpo(Message mensaje) {
        String texto = new String(mensaje.getBody(), StandardCharsets.UTF_8);
        return texto.length() > 300 ? texto.substring(0, 300) + "..." : texto;
    }
}
