package com.vidasalud.notify.tickets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.Channel;
import com.vidasalud.notify.config.MensajeriaProperties;
import com.vidasalud.notify.config.MensajeriaProperties.Cola;
import com.vidasalud.notify.messaging.CitaEvento;
import com.vidasalud.notify.messaging.ConfirmacionMensajes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Verifica las decisiones de confirmación del consumidor: ACK, NACK a DLQ, reintento y DLQ al agotar intentos. */
class TicketCitaListenerTest {

    private static final long TAG = 7L;
    private static final String COLA = "vidasalud.tickets";
    private static final String EVENTO_VALIDO = "{\"tipo\":\"CONFIRMADA\",\"citaId\":15,\"pacienteNombre\":\"Ana\",\"servicioId\":2}";

    private Channel canal;
    private RabbitTemplate rabbitTemplate;
    private TicketService service;
    private TicketCitaListener listener;

    @BeforeEach
    void preparar() {
        canal = mock(Channel.class);
        rabbitTemplate = mock(RabbitTemplate.class);
        service = mock(TicketService.class);
        Cola cola = new Cola(COLA, "cita.confirmada", COLA + ".dlq", "tickets.dlq");
        MensajeriaProperties props = new MensajeriaProperties(
                new MensajeriaProperties.Exchanges("vidasalud.citas.topic", "vidasalud.dlx.direct"),
                new MensajeriaProperties.Colas(cola, cola, cola),
                new MensajeriaProperties.Reintentos(3, "x-reintentos"),
                10);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        listener = new TicketCitaListener(new ConfirmacionMensajes(mapper, rabbitTemplate, props), service);
    }

    @Test
    void haceAckCuandoElTicketSeEmite() throws Exception {
        listener.recibir(mensaje(EVENTO_VALIDO, null), canal, TAG);

        verify(service).emitirTicket(any(CitaEvento.class));
        verify(canal).basicAck(TAG, false);
        verify(canal, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void mandaALaDlqUnJsonInvalidoSinReintentar() throws Exception {
        listener.recibir(mensaje("esto no es json", null), canal, TAG);

        verify(canal).basicNack(TAG, false, false);
        verify(service, never()).emitirTicket(any());
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
    }

    @Test
    void mandaALaDlqUnEventoSinCitaId() throws Exception {
        listener.recibir(mensaje("{\"tipo\":\"CONFIRMADA\"}", null), canal, TAG);

        verify(canal).basicNack(TAG, false, false);
    }

    @Test
    void reintentaUnErrorTransitorioRepublicandoConElContadorIncrementado() throws Exception {
        doThrow(new IllegalStateException("caído")).when(service).emitirTicket(any());

        listener.recibir(mensaje(EVENTO_VALIDO, null), canal, TAG);

        ArgumentCaptor<Message> republicado = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(""), eq(COLA), republicado.capture());
        assertThat((Integer) republicado.getValue().getMessageProperties().getHeader("x-reintentos")).isEqualTo(1);
        verify(canal).basicAck(TAG, false);
    }

    @Test
    void mandaALaDlqCuandoSeAgotanLosReintentos() throws Exception {
        doThrow(new IllegalStateException("sigue caído")).when(service).emitirTicket(any());

        listener.recibir(mensaje(EVENTO_VALIDO, 2), canal, TAG);

        verify(canal).basicNack(TAG, false, false);
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
    }

    private Message mensaje(String cuerpo, Integer reintentosPrevios) {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setDeliveryTag(TAG);
        propiedades.setConsumerQueue(COLA);
        if (reintentosPrevios != null) propiedades.setHeader("x-reintentos", reintentosPrevios);
        return new Message(cuerpo.getBytes(StandardCharsets.UTF_8), propiedades);
    }
}
