package com.vidasalud.notify.admin;

import com.vidasalud.notify.admin.dto.BindingRequest;
import com.vidasalud.notify.admin.dto.ColaInfoResponse;
import com.vidasalud.notify.admin.dto.CrearColaRequest;
import com.vidasalud.notify.admin.dto.CrearExchangeRequest;
import com.vidasalud.notify.admin.dto.PublicarMensajeRequest;
import com.vidasalud.notify.config.MensajeriaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 * Toda la administración de RabbitMQ (colas, exchanges, bindings y mensajes de prueba) pasa por este servicio.
 * El controlador solo invoca estos métodos y no conoce AmqpAdmin ni RabbitTemplate.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private final AmqpAdmin amqpAdmin;
    private final RabbitTemplate rabbitTemplate;
    private final MensajeriaProperties props;

    public RabbitAdminService(AmqpAdmin amqpAdmin, RabbitTemplate rabbitTemplate, MensajeriaProperties props) {
        this.amqpAdmin = amqpAdmin;
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    // ---------- Colas ----------

    public ColaInfoResponse obtenerCola(String nombre) {
        QueueInformation info = ejecutar(() -> amqpAdmin.getQueueInfo(nombre));
        if (info == null) {
            throw new RecursoNoEncontradoException("La cola '" + nombre + "' no existe");
        }
        return new ColaInfoResponse(info.getName(), info.getMessageCount(), info.getConsumerCount());
    }

    public ColaInfoResponse crearCola(CrearColaRequest req) {
        if (existeCola(req.nombre())) {
            throw new OperacionNoPermitidaException("La cola '" + req.nombre() + "' ya existe");
        }
        if (req.exchangeDeadLetter() != null && !existeExchange(req.exchangeDeadLetter())) {
            throw new RecursoNoEncontradoException("El exchange de dead-letter '" + req.exchangeDeadLetter() + "' no existe");
        }

        QueueBuilder builder = req.esDurable() ? QueueBuilder.durable(req.nombre()) : QueueBuilder.nonDurable(req.nombre());
        if (req.exchangeDeadLetter() != null) builder.deadLetterExchange(req.exchangeDeadLetter());
        if (req.routingKeyDeadLetter() != null) builder.deadLetterRoutingKey(req.routingKeyDeadLetter());
        if (req.ttlMensajesMs() != null) builder.ttl(req.ttlMensajesMs());
        if (req.largoMaximo() != null) builder.maxLength(req.largoMaximo().longValue());

        ejecutar(() -> amqpAdmin.declareQueue(builder.build()));
        log.info("Admin: cola '{}' creada (durable={}, dlx={})", req.nombre(), req.esDurable(), req.exchangeDeadLetter());
        return obtenerCola(req.nombre());
    }

    public void eliminarCola(String nombre) {
        verificarNoProtegido(nombre);
        if (!existeCola(nombre)) {
            throw new RecursoNoEncontradoException("La cola '" + nombre + "' no existe");
        }
        ejecutar(() -> amqpAdmin.deleteQueue(nombre));
        log.info("Admin: cola '{}' eliminada", nombre);
    }

    // ---------- Exchanges ----------

    public void crearExchange(CrearExchangeRequest req) {
        if (existeExchange(req.nombre())) {
            throw new OperacionNoPermitidaException("El exchange '" + req.nombre() + "' ya existe");
        }
        String tipo = req.tipo().name().toLowerCase();
        ejecutar(() -> {
            amqpAdmin.declareExchange(new ExchangeBuilder(req.nombre(), tipo).durable(req.esDurable()).build());
            return null;
        });
        log.info("Admin: exchange '{}' de tipo {} creado", req.nombre(), tipo);
    }

    public void eliminarExchange(String nombre) {
        verificarNoProtegido(nombre);
        if (!existeExchange(nombre)) {
            throw new RecursoNoEncontradoException("El exchange '" + nombre + "' no existe");
        }
        ejecutar(() -> amqpAdmin.deleteExchange(nombre));
        log.info("Admin: exchange '{}' eliminado", nombre);
    }

    // ---------- Bindings ----------

    public void crearBinding(BindingRequest req) {
        verificarExistenAmbos(req);
        ejecutar(() -> {
            amqpAdmin.declareBinding(aBinding(req));
            return null;
        });
        log.info("Admin: binding {} --[{}]--> {} creado", req.exchange(), req.routingKey(), req.cola());
    }

    public void eliminarBinding(BindingRequest req) {
        verificarNoProtegido(req.exchange());
        verificarNoProtegido(req.cola());
        verificarExistenAmbos(req);
        ejecutar(() -> {
            amqpAdmin.removeBinding(aBinding(req));
            return null;
        });
        log.info("Admin: binding {} --[{}]--> {} eliminado", req.exchange(), req.routingKey(), req.cola());
    }

    // ---------- Mensajes de prueba ----------

    public void publicarMensaje(PublicarMensajeRequest req) {
        if (!existeExchange(req.exchange())) {
            throw new RecursoNoEncontradoException("El exchange '" + req.exchange() + "' no existe");
        }
        var mensaje = MessageBuilder.withBody(req.cuerpo().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();
        ejecutar(() -> {
            rabbitTemplate.send(req.exchange(), req.routingKey(), mensaje);
            return null;
        });
        log.info("Admin: mensaje de prueba publicado en '{}' con routing key '{}'", req.exchange(), req.routingKey());
    }

    // ---------- Apoyo ----------

    private boolean existeCola(String nombre) {
        return ejecutar(() -> amqpAdmin.getQueueInfo(nombre)) != null;
    }

    /** Declaración pasiva: el broker responde 404 NOT_FOUND si el exchange no existe. */
    private boolean existeExchange(String nombre) {
        try {
            rabbitTemplate.execute(canal -> canal.exchangeDeclarePassive(nombre));
            return true;
        } catch (AmqpConnectException e) {
            throw e;
        } catch (AmqpException e) {
            if (contiene(e, "NOT_FOUND")) {
                return false;
            }
            throw new OperacionNoPermitidaException("RabbitMQ rechazó la consulta del exchange '" + nombre + "'", e);
        }
    }

    private void verificarExistenAmbos(BindingRequest req) {
        if (!existeExchange(req.exchange())) {
            throw new RecursoNoEncontradoException("El exchange '" + req.exchange() + "' no existe");
        }
        if (!existeCola(req.cola())) {
            throw new RecursoNoEncontradoException("La cola '" + req.cola() + "' no existe");
        }
    }

    private void verificarNoProtegido(String nombre) {
        if (props.recursosProtegidos().contains(nombre)) {
            throw new OperacionNoPermitidaException(
                    "'" + nombre + "' es parte de la topología del sistema y no se puede eliminar desde la API");
        }
    }

    private Binding aBinding(BindingRequest req) {
        return new Binding(req.cola(), Binding.DestinationType.QUEUE, req.exchange(), req.routingKey(), null);
    }

    /** Traduce los errores del broker (que no sean de conexión) a un 409 con un mensaje claro. */
    private <T> T ejecutar(Supplier<T> operacion) {
        try {
            return operacion.get();
        } catch (AmqpConnectException e) {
            throw e;
        } catch (AmqpException e) {
            throw new OperacionNoPermitidaException("RabbitMQ rechazó la operación: " + causaRaiz(e), e);
        }
    }

    private static boolean contiene(Throwable e, String texto) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains(texto)) return true;
        }
        return false;
    }

    private static String causaRaiz(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage();
    }
}
