package com.vidasalud.notify.notificaciones;

import com.vidasalud.notify.messaging.CitaEvento;
import org.springframework.lang.NonNull;
import org.springframework.web.util.HtmlUtils;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** Arma el contenido del correo de reserva: versión de texto y versión HTML con la misma estética del sitio. */
final class PlantillaCorreoReserva {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    /** Mismo dominio que el frontend de VidaSalud, para el botón del correo. */
    private static final String SITIO = "https://e7u1iiqh4k.execute-api.us-east-1.amazonaws.com";

    private static final String HTML = """
            <!doctype html>
            <html lang="es">
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;600;700&family=Space+Grotesk:wght@600;700&display=swap" rel="stylesheet">
            <style>
            @media only screen and (max-width:620px) {
              .exterior { padding:0 !important; }
              .card { border-radius:0 !important; border:0 !important; }
              .pad { padding-left:20px !important; padding-right:20px !important; }
              .hero { padding-top:26px !important; padding-bottom:24px !important; }
              .titulo { font-size:26px !important; }
              .fecha { font-size:22px !important; }
              .tarjeta { padding:14px 14px !important; }
              .valor { font-size:18px !important; }
              .boton { display:block !important; text-align:center !important; }
            }
            </style>
            </head>
            <body style="margin:0;padding:0;background:#eaf5ff;font-family:'DM Sans',Arial,Helvetica,sans-serif;color:#123a66;">
            <table class="exterior" role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background:#eaf5ff;padding:28px 12px;">
            <tr><td align="center">
            <table class="card" role="presentation" width="600" cellspacing="0" cellpadding="0" style="max-width:600px;width:100%;background:#ffffff;border-radius:20px;overflow:hidden;border:1px solid #cfe4fa;">

            <tr><td class="hero pad" style="background:#123255;background-image:linear-gradient(135deg,#1f5288 0%,#123255 55%,#081729 100%);padding:32px 32px 30px;color:#ffffff;">
              <div style="font-family:'Space Grotesk',Arial,sans-serif;font-size:20px;font-weight:700;letter-spacing:-.3px;">VidaSalud</div>
              <div style="margin-top:18px;">
                <span style="display:inline-block;background:rgba(255,255,255,.14);color:#bfe0ff;font-size:12px;font-weight:700;letter-spacing:.8px;text-transform:uppercase;padding:6px 12px;border-radius:20px;">{{etiqueta}}</span>
              </div>
              <div class="titulo" style="font-family:'Space Grotesk',Arial,sans-serif;font-size:30px;font-weight:700;line-height:1.15;margin-top:14px;">Tu cita #{{citaId}}</div>
              <div style="font-size:15px;color:#cfe4fa;margin-top:8px;line-height:1.5;">{{subtitulo}}</div>
            </td></tr>

            <tr><td class="pad" style="padding:28px 32px 8px;font-size:16px;line-height:1.6;">
              Hola <strong style="color:#0d2f66;">{{paciente}}</strong>, {{mensaje}}
            </td></tr>

            <tr><td class="pad" style="padding:12px 32px 0;">
              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background:#1a6bf0;background-image:linear-gradient(135deg,#1a6bf0 0%,#1558c9 100%);border-radius:14px;">
              <tr><td class="tarjeta" style="padding:20px 24px;color:#ffffff;">
                <div style="font-size:12px;font-weight:700;letter-spacing:.8px;text-transform:uppercase;opacity:.85;">Fecha y hora</div>
                <div class="fecha" style="font-family:'Space Grotesk',Arial,sans-serif;font-size:26px;font-weight:700;margin-top:4px;">{{fecha}}</div>
              </td></tr>
              </table>
            </td></tr>

            <tr><td class="pad" style="padding:14px 32px 0;">
              <table role="presentation" width="100%" cellspacing="0" cellpadding="0">
              <tr>
                <td width="50%" valign="top" style="padding-right:7px;">
                  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background:#eaf5ff;border:1px solid #cfe4fa;border-radius:14px;">
                  <tr><td class="tarjeta" style="padding:16px 18px;">
                    <div style="font-size:12px;font-weight:700;letter-spacing:.6px;text-transform:uppercase;color:#5b7ca8;">Servicio</div>
                    <div class="valor" style="font-family:'Space Grotesk',Arial,sans-serif;font-size:20px;font-weight:700;color:#0d2f66;margin-top:4px;">{{servicio}}</div>
                  </td></tr>
                  </table>
                </td>
                <td width="50%" valign="top" style="padding-left:7px;">
                  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background:#eaf5ff;border:1px solid #cfe4fa;border-radius:14px;">
                  <tr><td class="tarjeta" style="padding:16px 18px;">
                    <div style="font-size:12px;font-weight:700;letter-spacing:.6px;text-transform:uppercase;color:#5b7ca8;">Box</div>
                    <div class="valor" style="font-family:'Space Grotesk',Arial,sans-serif;font-size:20px;font-weight:700;color:#0d2f66;margin-top:4px;">{{box}}</div>
                  </td></tr>
                  </table>
                </td>
              </tr>
              </table>
            </td></tr>

            <tr><td class="pad" style="padding:14px 32px 0;">
              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background:#ffffff;border:1px solid #cfe4fa;border-radius:14px;">
              <tr><td class="tarjeta" style="padding:16px 18px;">
                <div style="font-size:12px;font-weight:700;letter-spacing:.6px;text-transform:uppercase;color:#5b7ca8;">Estado</div>
                <div class="valor" style="font-size:18px;font-weight:700;color:#0d2f66;margin-top:4px;">{{estado}}</div>
                <div style="margin-top:12px;">
                  <span style="display:inline-block;background:#d8ecfd;color:#0d2f66;font-size:13px;font-weight:700;padding:6px 14px;border-radius:20px;">&#9679; {{badge}}</span>
                </div>
              </td></tr>
              </table>
            </td></tr>

            <tr><td class="pad" align="center" style="padding:28px 32px 8px;">
              <a class="boton" href="{{sitio}}" style="display:inline-block;background:#1a6bf0;color:#ffffff;text-decoration:none;font-weight:700;font-size:15px;padding:14px 28px;border-radius:8px;">Ver mi cita en VidaSalud</a>
            </td></tr>

            <tr><td class="pad" style="padding:20px 32px 26px;font-size:12px;line-height:1.5;color:#5b7ca8;" align="center">
              Correo automático de VidaSalud. Por favor no respondas a este mensaje.
            </td></tr>

            <tr><td class="pad" style="background:#0d2f66;padding:16px 32px;font-size:12px;color:#cfe4fa;" align="center">
              VidaSalud · Atención médica de calidad
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
        Textos t = textos(evento);
        return "Tu cita #" + evento.citaId() + " " + t.verbo() + "\n"
                + "Hola " + paciente + ", " + t.mensaje() + "\n"
                + "Fecha y hora: " + fecha(evento) + "\n"
                + "Servicio: " + evento.servicioId() + "\n"
                + "Box: " + evento.boxId() + "\n";
    }

    static @NonNull String html(CitaEvento evento, @NonNull String paciente) {
        Textos t = textos(evento);
        return Objects.requireNonNull(HTML
                .replace("{{citaId}}", String.valueOf(evento.citaId()))
                .replace("{{etiqueta}}", t.etiqueta())
                .replace("{{subtitulo}}", t.subtitulo())
                .replace("{{mensaje}}", t.mensaje())
                .replace("{{estado}}", t.estado())
                .replace("{{badge}}", t.badge())
                .replace("{{paciente}}", HtmlUtils.htmlEscape(paciente))
                .replace("{{fecha}}", fecha(evento))
                .replace("{{servicio}}", HtmlUtils.htmlEscape("N° " + evento.servicioId()))
                .replace("{{box}}", HtmlUtils.htmlEscape("N° " + evento.boxId()))
                .replace("{{sitio}}", SITIO));
    }

    /** Textos del correo según el estado de la cita: solicitada (CREADA) o confirmada (CONFIRMADA). */
    private record Textos(String etiqueta, String subtitulo, String mensaje, String estado, String badge, String verbo) {
    }

    private static @NonNull Textos textos(CitaEvento evento) {
        return "CONFIRMADA".equals(evento.tipo())
                ? new Textos("Cita confirmada", "Tu hora quedó confirmada.",
                        "te esperamos en la fecha indicada.", "Hora confirmada", "Confirmada", "fue confirmada")
                : new Textos("Cita solicitada", "Tu solicitud de atención quedó registrada.",
                        "te avisaremos por este mismo correo cuando la hora sea confirmada.",
                        "Pendiente de confirmación", "Solicitada", "fue solicitada");
    }

    private static @NonNull String fecha(CitaEvento evento) {
        return Objects.requireNonNull(
                evento.fechaHora() == null ? "Por confirmar" : evento.fechaHora().format(FORMATO_FECHA));
    }
}
