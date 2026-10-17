# VidaSalud MS Notify

Microservicio de VidaSalud que **consume los eventos de citas desde RabbitMQ** (notificaciones al paciente, tickets de atención y comprobantes) y **administra RabbitMQ por REST** (colas, exchanges y bindings).

## Tecnologías

- Java 17, Spring Boot 3.2.5, Spring AMQP (RabbitMQ), Spring Validation.
- Spring Security con OAuth2 Resource Server: valida el JWT de Microsoft Entra ID.
- Spring Boot Actuator para el estado del servicio.
- Maven y Docker.

## Topología de mensajería

Todos los nombres están en `src/main/resources/application.yml` (`vidasalud.mensajeria`). Los beans de `Queue`, `Exchange` y `Binding` se declaran en `config/RabbitTopologyConfig`.

```
ms-appointments ──► vidasalud.citas.topic (topic)
                      ├─ cita.#          ──► vidasalud.notificaciones ─┐
                      ├─ cita.confirmada ──► vidasalud.tickets ────────┤ nack sin requeue
                      └─ cita.cerrada    ──► vidasalud.documentos ─────┤
                                                                       ▼
                    vidasalud.dlx.direct (direct)
                      ├─ notificaciones.dlq ──► vidasalud.notificaciones.dlq
                      ├─ tickets.dlq        ──► vidasalud.tickets.dlq
                      └─ documentos.dlq     ──► vidasalud.documentos.dlq
```

| Caso de uso | Cola | Recibe | Consumidor |
|---|---|---|---|
| Avisar al paciente de cada cambio | `vidasalud.notificaciones` | todos los eventos (`cita.#`) | `notificaciones/NotificacionCitaListener` |
| Emitir el número de atención | `vidasalud.tickets` | `cita.confirmada` | `tickets/TicketCitaListener` |
| Generar el comprobante de atención | `vidasalud.documentos` | `cita.cerrada` | `documentos/DocumentoCitaListener` |

## Confirmación de mensajes y errores

Los consumidores usan `@RabbitListener` con **ACK manual**. La política es la misma para todos y está en `messaging/ConfirmacionMensajes`:

| Situación | Acción | Resultado |
|---|---|---|
| Procesado correctamente | `basicAck` | El mensaje sale de la cola |
| Error no recuperable (JSON inválido, falta `citaId` o `tipo`) | `basicNack` sin requeue | Va a la DLQ de inmediato |
| Error transitorio (cualquier otra excepción) | Se republica con el header `x-reintentos` incrementado y se hace `basicAck` del original | Se reintenta |
| Error transitorio en el último intento (3 por defecto) | `basicNack` sin requeue | Va a la DLQ |
| No se puede republicar (broker caído) | `basicNack` con requeue | Vuelve a la cola, no se pierde |

Cada envío a la DLQ queda registrado en el log con nivel `ERROR` y el motivo. Los servicios de tickets y documentos son idempotentes: un evento repetido para la misma cita no genera un segundo ticket o comprobante.

## API REST

Todas las rutas requieren un JWT de Entra ID (`Authorization: Bearer <token>`).

### Administración de RabbitMQ (rol `Admin`)

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| GET | `/api/rabbit/queues/{nombre}` | Información de una cola (mensajes y consumidores) | 200 |
| POST | `/api/rabbit/queues` | Crea una cola | 201 |
| DELETE | `/api/rabbit/queues/{nombre}` | Elimina una cola | 204 |
| POST | `/api/rabbit/exchanges` | Crea un exchange | 201 |
| DELETE | `/api/rabbit/exchanges/{nombre}` | Elimina un exchange | 204 |
| POST | `/api/rabbit/bindings` | Enlaza un exchange con una cola | 201 |
| DELETE | `/api/rabbit/bindings?exchange=&cola=&routingKey=` | Elimina un enlace | 204 |
| POST | `/api/rabbit/messages` | Publica un mensaje de prueba | 202 |

Toda la lógica está en `admin/RabbitAdminService`. El controlador no usa directamente las clases de RabbitMQ.

**Ejemplos de cuerpo:**

```json
POST /api/rabbit/queues
{ "nombre": "demo.pagos", "durable": true, "exchangeDeadLetter": "vidasalud.dlx.direct",
  "routingKeyDeadLetter": "pagos.dlq", "ttlMensajesMs": 60000, "largoMaximo": 1000 }

POST /api/rabbit/exchanges
{ "nombre": "demo.pagos.direct", "tipo": "DIRECT", "durable": true }

POST /api/rabbit/bindings
{ "exchange": "demo.pagos.direct", "cola": "demo.pagos", "routingKey": "pago.aprobado" }

POST /api/rabbit/messages
{ "exchange": "vidasalud.citas.topic", "routingKey": "cita.confirmada",
  "cuerpo": "{\"tipo\":\"CONFIRMADA\",\"citaId\":101,\"pacienteNombre\":\"Ana\",\"servicioId\":1}" }
```

**Validaciones** (DTO con Bean Validation):
- Los nombres deben tener entre 3 y 120 caracteres (letras, números, `.`, `_`, `-`) y no pueden empezar con `amq.`, que está reservado por RabbitMQ.
- `tipo` debe ser `DIRECT`, `TOPIC`, `FANOUT` o `HEADERS`.
- TTL y largo máximo deben ser positivos y tienen un tope.
- `routingKeyDeadLetter` exige indicar `exchangeDeadLetter`.

| Código | Cuándo |
|---|---|
| 400 | Parámetros inválidos, con el campo y el motivo en `detalles` |
| 404 | La cola o el exchange no existe |
| 409 | El recurso ya existe, RabbitMQ rechazó la operación o es parte de la topología del sistema (protegido) |
| 503 | No hay conexión con RabbitMQ |

### Actividad de los consumidores (roles `Admin` y `Recepcionista`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/notify/notificaciones` | Últimas notificaciones enviadas |
| GET | `/api/notify/tickets` | Últimos tickets emitidos |
| GET | `/api/notify/documentos` | Últimos comprobantes generados |
| GET | `/actuator/health` | Estado del servicio y de la conexión a RabbitMQ (público) |

## Puerto y configuración

Puerto `8083`. Variables de entorno:

| Variable | Por defecto | Descripción |
|---|---|---|
| `RABBITMQ_ADDRESSES` | `localhost:5672,localhost:5673` | Nodos del clúster |
| `RABBITMQ_USER` / `RABBITMQ_PASS` | `vidasalud` / `vidasalud` | Credenciales de RabbitMQ |
| `SEGURIDAD_HABILITADA` | `true` | Validación del JWT. Solo se desactiva con el perfil `local` |

## Ejecución

El clúster de RabbitMQ se levanta desde el repo `vidasalud-infra`.

```bash
# Local, sin token (perfil local)
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Pruebas
mvn test

# Docker
docker build -t notify .
docker run -d --name notify -p 8083:8083 -e RABBITMQ_ADDRESSES=rabbitmq1:5672,rabbitmq2:5672 notify
```

## Demostrar reintentos y DLQ

- **Error no recuperable:** publica con `POST /api/rabbit/messages` un `cuerpo` que no sea JSON. El mensaje llega a `vidasalud.notificaciones.dlq` sin reintentos.
- **Error transitorio:** publica un evento cuyo `pacienteNombre` contenga `[falla-temporal]`. El log muestra los intentos 1/3 y 2/3, y luego el envío a la DLQ.
- Para ver cuántos mensajes hay en la DLQ: `GET /api/rabbit/queues/vidasalud.notificaciones.dlq`, o el panel de RabbitMQ en el puerto `15672`.
