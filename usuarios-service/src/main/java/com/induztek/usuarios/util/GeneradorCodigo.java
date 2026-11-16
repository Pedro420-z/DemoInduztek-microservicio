package com.induztek.usuarios.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class GeneradorCodigo {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int LARGO_CONTRASENA_TEMPORAL = 10;

    private final SecureRandom azar = new SecureRandom();

    public String generar() {
        return String.format("%06d", azar.nextInt(1_000_000));
    }

    public String generarContrasenaTemporal() {
        StringBuilder contrasena = new StringBuilder(LARGO_CONTRASENA_TEMPORAL);
        for (int i = 0; i < LARGO_CONTRASENA_TEMPORAL; i++) {
            contrasena.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
        }
        return contrasena.toString();
    }
}
