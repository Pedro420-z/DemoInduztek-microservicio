package com.induztek.equipos.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AppConfigTest {

    @Test
    void reloj_devuelveUnRelojDelSistema() {
        Clock reloj = new AppConfig().reloj();

        assertNotNull(reloj);
        assertNotNull(reloj.instant());
    }
}
