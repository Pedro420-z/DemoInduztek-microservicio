package com.induztek.usuarios.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificadorCorreoSmtpTest {

    @Mock
    private JavaMailSender enviador;

    @Mock
    private NotificadorCorreoConsola respaldo;

    private NotificadorCorreoSmtp notificador;

    @BeforeEach
    void preparar() {
        notificador = new NotificadorCorreoSmtp(enviador, respaldo, "no-responder@induztek.cl");
    }

    @Test
    void enviarContrasenaTemporal_enviaUnCorreoConLaClave() {
        notificador.enviarContrasenaTemporal("ana@induztek.cl", "Ana", "Abc23456xY");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(enviador).send(captor.capture());
        assertEquals("no-responder@induztek.cl", captor.getValue().getFrom());
        assertEquals("ana@induztek.cl", captor.getValue().getTo()[0]);
        assertTrue(captor.getValue().getText().contains("Abc23456xY"));
    }

    @Test
    void enviarCodigoRecuperacion_enviaUnCorreoConElCodigo() {
        notificador.enviarCodigoRecuperacion("ana@induztek.cl", "Ana", "123456", LocalDateTime.of(2026, 11, 15, 12, 15));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(enviador).send(captor.capture());
        assertTrue(captor.getValue().getText().contains("123456"));
        assertTrue(captor.getValue().getText().contains("12:15"));
    }

    @Test
    void siFallaElSmtp_usaElRespaldoPorConsola() {
        doThrow(new MailSendException("sin servidor")).when(enviador).send(any(SimpleMailMessage.class));
        LocalDateTime expira = LocalDateTime.of(2026, 11, 15, 12, 15);

        assertDoesNotThrow(() -> notificador.enviarContrasenaTemporal("ana@induztek.cl", "Ana", "Abc23456xY"));
        assertDoesNotThrow(() -> notificador.enviarCodigoRecuperacion("ana@induztek.cl", "Ana", "123456", expira));

        verify(respaldo).enviarContrasenaTemporal("ana@induztek.cl", "Ana", "Abc23456xY");
        verify(respaldo).enviarCodigoRecuperacion("ana@induztek.cl", "Ana", "123456", expira);
    }
}
