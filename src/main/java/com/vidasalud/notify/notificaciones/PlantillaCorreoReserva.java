package com.vidasalud.notify.notificaciones;

import com.vidasalud.notify.messaging.CitaEvento;
import org.springframework.lang.NonNull;
import org.springframework.web.util.HtmlUtils;

import java.time.format.DateTimeFormatter;

/** Arma el contenido del correo de reserva: versión de texto y versión HTML con estilos en línea. */
final class PlantillaCorreoReserva {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private static final String HTML = """
            <!doctype html>
            <html lang="es">
            <body style="margin:0;padding:0;background:#eef3f2;font-family:Arial,Helvetica,sans-serif;color:#1f2d2b;">
            <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background:#eef3f2;padding:24px 12px;">
            <tr><td align="center">
            <table role="presentation" width="560" cellspacing="0" cellpadding="0" style="max-width:560px;width:100%;background:#ffffff;border-radius:12px;">
            <tr><td style="background:#0f766e;padding:24px 28px;border-radius:12px 12px 0 0;color:#ffffff;">
              <div style="font-size:12px;letter-spacing:1px;text-transform:uppercase;opacity:.85;">VidaSalud</div>
              <div style="font-size:22px;font-weight:bold;margin-top:6px;">Nueva reserva #{{citaId}}</div>
            </td></tr>
            <tr><td style="padding:24px 28px 8px;font-size:15px;line-height:1.5;">
              Se registró una nueva reserva a nombre de <strong>{{paciente}}</strong>.
            </td></tr>
            <tr><td style="padding:8px 28px 24px;">
              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="border:1px solid #d7e6e3;border-radius:10px;">
              {{filas}}
              </table>
            </td></tr>
            <tr><td style="padding:0 28px 24px;font-size:12px;line-height:1.5;color:#5b7470;">
              Correo automático de VidaSalud. Por favor no respondas a este mensaje.
            </td></tr>
            </table>
            </td></tr>
            </table>
            </body>
            </html>
            """;

    private PlantillaCorreoReserva() {
    }

    static @NonNull String texto(CitaEvento evento, @NonNull String paciente) {
        return "Nueva reserva #" + evento.citaId() + "\n"
                + "Paciente: " + paciente + "\n"
                + "Fecha y hora: " + fecha(evento) + "\n"
                + "Servicio: " + evento.servicioId() + "\n"
                + "Box: " + evento.boxId() + "\n";
    }

    static @NonNull String html(CitaEvento evento, @NonNull String paciente) {
        // Este correo solo se envía para reservas nuevas, así que el estado es siempre "Solicitada".
        String filas = fila("Fecha y hora", fecha(evento), true)
                + fila("Paciente", paciente, false)
                + fila("Servicio", "N° " + evento.servicioId(), false)
                + fila("Box", "N° " + evento.boxId(), false)
                + fila("Estado", "Solicitada", false);

        return HTML
                .replace("{{citaId}}", String.valueOf(evento.citaId()))
                .replace("{{paciente}}", HtmlUtils.htmlEscape(paciente))
                .replace("{{filas}}", filas);
    }

    private static @NonNull String fecha(CitaEvento evento) {
        return evento.fechaHora() == null ? "Por confirmar" : evento.fechaHora().format(FORMATO_FECHA);
    }

    private static String fila(@NonNull String etiqueta, @NonNull String valor, boolean primera) {
        String borde = primera ? "" : "border-top:1px solid #d7e6e3;";
        return "<tr><td style=\"padding:14px 18px;" + borde + "\">"
                + "<div style=\"font-size:12px;color:#5b7470;text-transform:uppercase;letter-spacing:.5px;\">"
                + HtmlUtils.htmlEscape(etiqueta) + "</div>"
                + "<div style=\"font-size:16px;font-weight:bold;margin-top:2px;\">"
                + HtmlUtils.htmlEscape(valor) + "</div>"
                + "</td></tr>\n";
    }
}
