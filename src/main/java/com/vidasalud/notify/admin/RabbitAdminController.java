package com.vidasalud.notify.admin;

import com.vidasalud.notify.admin.dto.BindingRequest;
import com.vidasalud.notify.admin.dto.ColaInfoResponse;
import com.vidasalud.notify.admin.dto.CrearColaRequest;
import com.vidasalud.notify.admin.dto.CrearExchangeRequest;
import com.vidasalud.notify.admin.dto.NombresRabbit;
import com.vidasalud.notify.admin.dto.PublicarMensajeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

/**
 * API REST para administrar RabbitMQ (solo rol Admin).
 *
 * <pre>
 * GET    /api/rabbit/queues/{nombre}   información de una cola (mensajes y consumidores)
 * POST   /api/rabbit/queues            crea una cola                        → 201
 * DELETE /api/rabbit/queues/{nombre}   elimina una cola                     → 204
 * POST   /api/rabbit/exchanges         crea un exchange                     → 201
 * DELETE /api/rabbit/exchanges/{nombre} elimina un exchange                 → 204
 * POST   /api/rabbit/bindings          enlaza un exchange con una cola      → 201
 * DELETE /api/rabbit/bindings?exchange=&cola=&routingKey=  elimina el enlace → 204
 * POST   /api/rabbit/messages          publica un mensaje de prueba         → 202
 * </pre>
 */
@Tag(name = "Administración RabbitMQ", description = "Colas, exchanges y bindings (solo rol Admin)")
@RestController
@RequestMapping("/api/rabbit")
public class RabbitAdminController {

    private final RabbitAdminService service;

    public RabbitAdminController(RabbitAdminService service) {
        this.service = service;
    }

    @Operation(summary = "Consulta una cola: mensajes pendientes y consumidores")
    @GetMapping("/queues/{nombre}")
    public ColaInfoResponse obtenerCola(
            @PathVariable @Pattern(regexp = NombresRabbit.PATRON_NOMBRE, message = NombresRabbit.MENSAJE_NOMBRE) String nombre) {
        return service.obtenerCola(nombre);
    }

    @Operation(summary = "Crea una cola (opcionalmente con dead-letter, TTL y largo máximo)")
    @PostMapping("/queues")
    public ResponseEntity<ColaInfoResponse> crearCola(@Valid @RequestBody CrearColaRequest req) {
        ColaInfoResponse cola = service.crearCola(req);
        return ResponseEntity.created(URI.create("/api/rabbit/queues/" + cola.nombre())).body(cola);
    }

    @Operation(summary = "Elimina una cola (no permite eliminar las del sistema)")
    @DeleteMapping("/queues/{nombre}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarCola(
            @PathVariable @Pattern(regexp = NombresRabbit.PATRON_NOMBRE, message = NombresRabbit.MENSAJE_NOMBRE) String nombre) {
        service.eliminarCola(nombre);
    }

    @Operation(summary = "Crea un exchange DIRECT, TOPIC, FANOUT o HEADERS")
    @PostMapping("/exchanges")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> crearExchange(@Valid @RequestBody CrearExchangeRequest req) {
        service.crearExchange(req);
        return Map.of("nombre", req.nombre(), "tipo", req.tipo().name());
    }

    @Operation(summary = "Elimina un exchange (no permite eliminar los del sistema)")
    @DeleteMapping("/exchanges/{nombre}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarExchange(
            @PathVariable @Pattern(regexp = NombresRabbit.PATRON_NOMBRE, message = NombresRabbit.MENSAJE_NOMBRE) String nombre) {
        service.eliminarExchange(nombre);
    }

    @Operation(summary = "Enlaza un exchange con una cola mediante una routing key")
    @PostMapping("/bindings")
    @ResponseStatus(HttpStatus.CREATED)
    public BindingRequest crearBinding(@Valid @RequestBody BindingRequest req) {
        service.crearBinding(req);
        return req;
    }

    @Operation(summary = "Elimina el enlace entre un exchange y una cola")
    @DeleteMapping("/bindings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarBinding(@Valid @ModelAttribute BindingRequest req) {
        service.eliminarBinding(req);
    }

    @Operation(summary = "Publica un mensaje de prueba en un exchange")
    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> publicarMensaje(@Valid @RequestBody PublicarMensajeRequest req) {
        service.publicarMensaje(req);
        return Map.of("estado", "publicado", "exchange", req.exchange(), "routingKey", req.routingKey());
    }
}
