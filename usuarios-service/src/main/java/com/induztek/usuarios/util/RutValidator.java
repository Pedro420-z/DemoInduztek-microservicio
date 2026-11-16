package com.induztek.usuarios.util;

public final class RutValidator {

    private RutValidator() {
    }

    public static String normalizar(String rut) {
        if (rut == null) {
            return "";
        }
        String limpio = rut.trim().toUpperCase().replaceAll("[^0-9K]", "");
        if (limpio.length() < 2) {
            return limpio;
        }
        return limpio.substring(0, limpio.length() - 1) + "-" + limpio.charAt(limpio.length() - 1);
    }

    public static char calcularDigito(String cuerpo) {
        int suma = 0;
        int factor = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += (cuerpo.charAt(i) - '0') * factor;
            factor = factor == 7 ? 2 : factor + 1;
        }
        int resto = 11 - (suma % 11);
        if (resto == 11) {
            return '0';
        }
        if (resto == 10) {
            return 'K';
        }
        return (char) ('0' + resto);
    }

    public static boolean esValido(String rut) {
        String normalizado = normalizar(rut);
        int guion = normalizado.indexOf('-');
        if (guion < 0) {
            return false;
        }
        String cuerpo = normalizado.substring(0, guion);
        char digito = normalizado.charAt(normalizado.length() - 1);
        boolean soloDigitos = cuerpo.chars().allMatch(Character::isDigit);
        if (!soloDigitos || cuerpo.length() < 7 || cuerpo.length() > 8) {
            return false;
        }
        return calcularDigito(cuerpo) == digito;
    }
}
