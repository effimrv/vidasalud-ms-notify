package com.vidasalud.notify.documentos;

import com.vidasalud.notify.actividad.RegistroActividad;
import com.vidasalud.notify.messaging.CitaEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Genera el comprobante de atención cuando la cita se cierra. Es idempotente por cita. */
@Service
public class DocumentoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoService.class);
    private static final DateTimeFormatter FORMATO_FOLIO = DateTimeFormatter.BASIC_ISO_DATE;

    private final Map<Long, Comprobante> comprobantesPorCita = new ConcurrentHashMap<>();
    private final RegistroActividad<Comprobante> registro = new RegistroActividad<>();

    public void generarComprobante(CitaEvento evento) {
        if (comprobantesPorCita.containsKey(evento.citaId())) {
            log.info("La cita {} ya tiene comprobante, se ignora el duplicado", evento.citaId());
            return;
        }
        String folio = "DOC-" + LocalDate.now().format(FORMATO_FOLIO) + "-" + evento.citaId();
        String contenido = "Comprobante de atención " + folio + "\n"
                + "Paciente: " + evento.pacienteNombre() + "\n"
                + "Prestación: " + evento.servicioId() + "\n"
                + "Fecha de la cita: " + (evento.fechaHora() == null ? "-" : evento.fechaHora()) + "\n"
                + "Estado: atención finalizada";
        Comprobante comprobante = new Comprobante(folio, evento.citaId(), evento.pacienteNombre(),
                evento.servicioId(), contenido, Instant.now());
        comprobantesPorCita.put(evento.citaId(), comprobante);
        registro.agregar(comprobante);
        log.info("Comprobante {} generado para la cita {}", folio, evento.citaId());
    }

    public List<Comprobante> recientes() {
        return registro.recientes();
    }
}
