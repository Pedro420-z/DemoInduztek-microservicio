package com.induztek.usuarios.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class GeneradorCodigo {

    private final SecureRandom azar = new SecureRandom();

    public String generar() {
        return String.format("%06d", azar.nextInt(1_000_000));
    }
}
