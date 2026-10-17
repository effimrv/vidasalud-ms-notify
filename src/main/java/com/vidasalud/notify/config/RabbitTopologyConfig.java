package com.vidasalud.notify.config;

import com.vidasalud.notify.config.MensajeriaProperties.Cola;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología de RabbitMQ del sistema. RabbitAdmin la declara automáticamente en el broker al conectarse.
 *
 * <pre>
 * ms-appointments ──► [vidasalud.citas.topic] (topic)
 *                        ├─ cita.#          ──► vidasalud.notificaciones ─┐
 *                        ├─ cita.confirmada ──► vidasalud.tickets ────────┤ nack sin requeue
 *                        └─ cita.cerrada    ──► vidasalud.documentos ─────┤
 *                                                                         ▼
 *                     [vidasalud.dlx.direct] (direct)
 *                        ├─ notificaciones.dlq ──► vidasalud.notificaciones.dlq
 *                        ├─ tickets.dlq        ──► vidasalud.tickets.dlq
 *                        └─ documentos.dlq     ──► vidasalud.documentos.dlq
 * </pre>
 */
@Configuration
public class RabbitTopologyConfig {

    private final MensajeriaProperties props;

    public RabbitTopologyConfig(MensajeriaProperties props) {
        this.props = props;
    }

    // ---------- Exchanges ----------

    /** Exchange topic de eventos de citas: permite que cada cola elija qué eventos recibe por patrón. */
    @Bean
    public TopicExchange exchangeCitas() {
        return ExchangeBuilder.topicExchange(props.exchanges().citas()).durable(true).build();
    }

    /** Exchange direct de dead-letter: cada DLQ se enlaza con una routing key exacta. */
    @Bean
    public DirectExchange exchangeDeadLetter() {
        return ExchangeBuilder.directExchange(props.exchanges().deadLetter()).durable(true).build();
    }

    // ---------- Caso de uso 1: notificaciones al paciente (todos los eventos de cita) ----------

    @Bean
    public Queue colaNotificaciones() {
        return colaConDeadLetter(props.colas().notificaciones());
    }

    @Bean
    public Queue colaNotificacionesDlq() {
        return QueueBuilder.durable(props.colas().notificaciones().dlq()).build();
    }

    @Bean
    public Binding bindingNotificaciones() {
        return BindingBuilder.bind(colaNotificaciones()).to(exchangeCitas())
                .with(props.colas().notificaciones().patron());
    }

    @Bean
    public Binding bindingNotificacionesDlq() {
        return BindingBuilder.bind(colaNotificacionesDlq()).to(exchangeDeadLetter())
                .with(props.colas().notificaciones().dlqRoutingKey());
    }

    // ---------- Caso de uso 2: ticket de atención (cita confirmada) ----------

    @Bean
    public Queue colaTickets() {
        return colaConDeadLetter(props.colas().tickets());
    }

    @Bean
    public Queue colaTicketsDlq() {
        return QueueBuilder.durable(props.colas().tickets().dlq()).build();
    }

    @Bean
    public Binding bindingTickets() {
        return BindingBuilder.bind(colaTickets()).to(exchangeCitas())
                .with(props.colas().tickets().patron());
    }

    @Bean
    public Binding bindingTicketsDlq() {
        return BindingBuilder.bind(colaTicketsDlq()).to(exchangeDeadLetter())
                .with(props.colas().tickets().dlqRoutingKey());
    }

    // ---------- Caso de uso 3: comprobante de atención (cita cerrada) ----------

    @Bean
    public Queue colaDocumentos() {
        return colaConDeadLetter(props.colas().documentos());
    }

    @Bean
    public Queue colaDocumentosDlq() {
        return QueueBuilder.durable(props.colas().documentos().dlq()).build();
    }

    @Bean
    public Binding bindingDocumentos() {
        return BindingBuilder.bind(colaDocumentos()).to(exchangeCitas())
                .with(props.colas().documentos().patron());
    }

    @Bean
    public Binding bindingDocumentosDlq() {
        return BindingBuilder.bind(colaDocumentosDlq()).to(exchangeDeadLetter())
                .with(props.colas().documentos().dlqRoutingKey());
    }

    /** Cola durable cuyos mensajes rechazados (nack sin requeue) se desvían al exchange de dead-letter. */
    private Queue colaConDeadLetter(Cola cola) {
        return QueueBuilder.durable(cola.nombre())
                .deadLetterExchange(props.exchanges().deadLetter())
                .deadLetterRoutingKey(cola.dlqRoutingKey())
                .build();
    }
}
