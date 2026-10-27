package com.vidasalud.notify.notificaciones;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Envía por SMTP el correo de cada reserva nueva al destinatario configurado en vidasalud.correo. */
@Component
public class CorreoReservas {

    private final JavaMailSender mailSender;
    private final String remitente;
    private final String destinatario;

    public CorreoReservas(JavaMailSender mailSender,
                          @Value("${spring.mail.username}") String remitente,
                          @Value("${vidasalud.correo.reservas-destinatario}") String destinatario) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.destinatario = destinatario;
    }

    /** Un fallo de SMTP lanza excepción: el listener lo trata como transitorio y reintenta. */
    public void enviar(String asunto, String cuerpo) {
        SimpleMailMessage correo = new SimpleMailMessage();
        correo.setFrom(remitente);
        correo.setTo(destinatario);
        correo.setSubject(asunto);
        correo.setText(cuerpo);
        mailSender.send(correo);
    }
}
