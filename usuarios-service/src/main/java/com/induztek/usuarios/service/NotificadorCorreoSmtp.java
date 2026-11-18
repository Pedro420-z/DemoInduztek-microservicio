package com.induztek.usuarios.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Primary
@Component
public class NotificadorCorreoSmtp implements NotificadorCorreo {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final JavaMailSender enviador;
    private final NotificadorCorreoConsola respaldo;
    private final String remitente;

    public NotificadorCorreoSmtp(JavaMailSender enviador,
                                 NotificadorCorreoConsola respaldo,
                                 @Value("${correo.remitente}") String remitente) {
        this.enviador = enviador;
        this.respaldo = respaldo;
        this.remitente = remitente;
    }

    @Override
    public void enviarContrasenaTemporal(String correo, String nombre, String contrasenaTemporal) {
        String texto = "Hola " + nombre + ",\n\n"
                + "Se creó tu cuenta en Induztek. Tu contraseña temporal es: " + contrasenaTemporal + "\n\n"
                + "Debes cambiarla al ingresar por primera vez.";
        enviar(correo, "Tu cuenta en Induztek", texto,
                () -> respaldo.enviarContrasenaTemporal(correo, nombre, contrasenaTemporal));
    }

    @Override
    public void enviarCodigoRecuperacion(String correo, String nombre, String codigo, LocalDateTime expira) {
        String texto = "Hola " + nombre + ",\n\n"
                + "Tu código de recuperación es: " + codigo + "\n\n"
                + "Vence a las " + expira.format(HORA) + ". Si no lo pediste, ignora este mensaje.";
        enviar(correo, "Recuperación de contraseña", texto,
                () -> respaldo.enviarCodigoRecuperacion(correo, nombre, codigo, expira));
    }

    private void enviar(String para, String asunto, String texto, Runnable alFallar) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(para);
            mensaje.setSubject(asunto);
            mensaje.setText(texto);
            enviador.send(mensaje);
        } catch (MailException e) {
            log.warn("No se pudo enviar el correo por SMTP ({}). Se muestra en consola.", e.getMessage());
            alFallar.run();
        }
    }
}
