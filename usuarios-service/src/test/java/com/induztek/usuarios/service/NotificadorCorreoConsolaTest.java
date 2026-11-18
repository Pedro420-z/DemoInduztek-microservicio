package com.induztek.usuarios.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificadorCorreoConsolaTest {

    @Test
    void enviarContrasenaTemporal_noLanzaErrores() {
        NotificadorCorreoConsola notificador = new NotificadorCorreoConsola();

        assertDoesNotThrow(() -> notificador.enviarContrasenaTemporal("ana@induztek.cl", "Ana Torres", "Abc23456xY"));
    }

    @Test
    void enviarCodigoRecuperacion_noLanzaErrores() {
        NotificadorCorreoConsola notificador = new NotificadorCorreoConsola();

        assertDoesNotThrow(() -> notificador.enviarCodigoRecuperacion("ana@induztek.cl", "Ana Torres", "123456",
                LocalDateTime.of(2026, 11, 15, 12, 15)));
    }
}
