package com.induztek.usuarios.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    private final AppConfig config = new AppConfig();

    @Test
    void passwordEncoder_guardaHashYNoLaClaveEnTextoPlano() {
        PasswordEncoder encoder = config.passwordEncoder();
        String hash = encoder.encode("secreto1");

        assertNotEquals("secreto1", hash);
        assertTrue(encoder.matches("secreto1", hash));
    }

    @Test
    void reloj_devuelveUnRelojDelSistema() {
        Clock reloj = config.reloj();

        assertNotNull(reloj);
    }
}
