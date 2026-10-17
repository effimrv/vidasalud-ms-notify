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

/**
 * Apoyo común de los consumidores: lectura del evento y política de reintentos.
 * Cada listener decide explícitamente ack / nack; esta clase solo resuelve los dos pasos que comparten:
 * <ul>
 *   <li>{@link #leerEvento}: convierte el JSON y valida los datos mínimos. Si falla lanza
 *       {@link MensajeNoRecuperableException} → el listener hace {@code basicNack} sin requeue → DLQ.</li>
 *   <li>{@link #reintentarOEnviarADlq}: ante un error transitorio republica el mensaje en la misma cola con el
 *       header de reintentos incrementado y hace {@code basicAck} del original. Al llegar al máximo hace
 *       {@code basicNack} sin requeue → DLQ. Si ni siquiera puede republicar, {@code basicNack} con requeue
 *       para no perder el mensaje.</li>
 * </ul>
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

    /** Convierte el cuerpo a {@link CitaEvento}. Un JSON inválido o sin citaId/tipo no se arregla reintentando. */
    public CitaEvento leerEvento(Message mensaje) {
        CitaEvento evento;
        try {
            evento = objectMapper.readValue(mensaje.getBody(), CitaEvento.class);
        } catch (IOException e) {
            throw new MensajeNoRecuperableException("JSON inválido para CitaEvento. Cuerpo: " + cuerpo(mensaje), e);
        }
        if (evento == null || evento.citaId() == null || evento.tipo() == null || evento.tipo().isBlank()) {
            throw new MensajeNoRecuperableException("El evento no trae citaId o tipo. Cuerpo: " + cuerpo(mensaje));
        }
        return evento;
    }

    /** Número de intento actual (1 la primera vez), leído del header de reintentos. */
    public int intentoActual(Message mensaje) {
        Object valor = mensaje.getMessageProperties().getHeader(props.reintentos().header());
        return (valor instanceof Number n ? n.intValue() : 0) + 1;
    }

    public int maximoIntentos() {
        return props.reintentos().maximo();
    }

    /** Decide entre reintentar o mandar a la DLQ un mensaje cuyo procesamiento falló por un error transitorio. */
    public void reintentarOEnviarADlq(Message mensaje, Channel canal, long deliveryTag, Exception error) throws IOException {
        String cola = mensaje.getMessageProperties().getConsumerQueue();
        int intento = intentoActual(mensaje);
        int maximo = maximoIntentos();

        if (intento >= maximo) {
            canal.basicNack(deliveryTag, false, false);
            log.error("[{}] NACK tras agotar {} intentos → DLQ. Último error: {}", cola, maximo, error.getMessage());
            return;
        }
        try {
            Message reintento = MessageBuilder.fromMessage(mensaje)
                    .setHeader(props.reintentos().header(), intento)
                    .build();
            rabbitTemplate.send(EXCHANGE_POR_DEFECTO, cola, reintento);
            canal.basicAck(deliveryTag, false);
            log.warn("[{}] Error transitorio en intento {}/{}, se reintenta: {}", cola, intento, maximo, error.getMessage());
        } catch (AmqpException errorRepublicando) {
            canal.basicNack(deliveryTag, false, true);
            log.error("[{}] No se pudo republicar para reintento, se devuelve a la cola: {}", cola, errorRepublicando.getMessage());
        }
    }

    private String cuerpo(Message mensaje) {
        String texto = new String(mensaje.getBody(), StandardCharsets.UTF_8);
        return texto.length() > 300 ? texto.substring(0, 300) + "..." : texto;
    }
}
