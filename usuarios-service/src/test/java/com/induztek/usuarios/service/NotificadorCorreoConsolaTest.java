package com.induztek.usuarios.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificadorCorreoConsolaTest {

    @Test
    void enviarContrasenaTemporal_noLanzaErrores() {
        NotificadorCorreoConsola notificador = new NotificadorCorreoConsola();

        assertDoesNotThrow(() -> notificador.enviarContrasenaTemporal("ana@induztek.cl", "Ana Torres", "Abc23456xY"));
    }
}
