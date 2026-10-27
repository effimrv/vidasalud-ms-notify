package com.vidasalud.notify.notificaciones;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/** Envía por SMTP el correo de cada reserva nueva al destinatario configurado en vidasalud.correo. */
@Component
public class CorreoReservas {

    private final JavaMailSender mailSender;
    private final @NonNull String remitente;
    private final @NonNull String destinatario;

    public CorreoReservas(JavaMailSender mailSender,
                        @Value("${spring.mail.username}") @NonNull String remitente,
                        @Value("${vidasalud.correo.reservas-destinatario}") @NonNull String destinatario) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.destinatario = destinatario;
    }

    /** Envía un correo multiparte: texto plano para clientes sin HTML y la versión con estilos. */
    public void enviar(@NonNull String asunto, @NonNull String texto, @NonNull String html) {
        try {
            MimeMessage correo = mailSender.createMimeMessage();
            MimeMessageHelper ayuda = new MimeMessageHelper(correo, true, "UTF-8");
            ayuda.setFrom(remitente);
            ayuda.setTo(destinatario);
            ayuda.setSubject(asunto);
            ayuda.setText(texto, html);
            mailSender.send(correo);
        } catch (MessagingException e) {
            throw new IllegalStateException("No se pudo armar el correo de reserva", e);
        }
    }
}
