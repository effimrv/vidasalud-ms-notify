package com.vidasalud.notify.admin;

import jakarta.validation.ConstraintViolationException;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Convierte los errores de la API de administración en respuestas JSON con un mensaje claro en español. */
@RestControllerAdvice(assignableTypes = RabbitAdminController.class)
public class RabbitAdminExceptionHandler {

    /** Errores de @Valid en el cuerpo (MethodArgumentNotValidException) o en query params (@ModelAttribute). */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> validacion(BindException e) {
        Map<String, String> detalles = new LinkedHashMap<>();
        e.getFieldErrors().forEach(fe -> detalles.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        e.getGlobalErrors().forEach(ge -> detalles.putIfAbsent(ge.getObjectName(), ge.getDefaultMessage()));
        return respuesta(HttpStatus.BAD_REQUEST, "Parámetros inválidos", detalles);
    }

    /** Errores de validación de variables de ruta (y de @Valid cuando Spring aplica validación de método). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> validacionMetodo(HandlerMethodValidationException e) {
        Map<String, String> detalles = new LinkedHashMap<>();
        e.getAllValidationResults().forEach(resultado -> {
            if (resultado instanceof ParameterErrors errores) {
                errores.getFieldErrors().forEach(fe -> detalles.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
            } else {
                String parametro = resultado.getMethodParameter().getParameterName();
                resultado.getResolvableErrors().stream()
                        .map(MessageSourceResolvable::getDefaultMessage)
                        .forEach(msg -> detalles.putIfAbsent(parametro, msg));
            }
        });
        return respuesta(HttpStatus.BAD_REQUEST, "Parámetros inválidos", detalles);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> restriccion(ConstraintViolationException e) {
        Map<String, String> detalles = new LinkedHashMap<>();
        e.getConstraintViolations().forEach(v -> {
            String ruta = v.getPropertyPath().toString();
            detalles.putIfAbsent(ruta.substring(ruta.lastIndexOf('.') + 1), v.getMessage());
        });
        return respuesta(HttpStatus.BAD_REQUEST, "Parámetros inválidos", detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> cuerpoIlegible(HttpMessageNotReadableException e) {
        return respuesta(HttpStatus.BAD_REQUEST,
                "El cuerpo no es un JSON válido o tiene un valor no permitido (por ejemplo, 'tipo' debe ser DIRECT, TOPIC, FANOUT o HEADERS)",
                Map.of());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage(), Map.of());
    }

    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ResponseEntity<Map<String, Object>> conflicto(OperacionNoPermitidaException e) {
        return respuesta(HttpStatus.CONFLICT, e.getMessage(), Map.of());
    }

    @ExceptionHandler(AmqpConnectException.class)
    public ResponseEntity<Map<String, Object>> sinConexion(AmqpConnectException e) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, "No hay conexión con RabbitMQ", Map.of());
    }

    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus estado, String mensaje, Map<String, String> detalles) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("estado", estado.value());
        cuerpo.put("error", estado.getReasonPhrase());
        cuerpo.put("mensaje", mensaje);
        if (!detalles.isEmpty()) cuerpo.put("detalles", detalles);
        cuerpo.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
