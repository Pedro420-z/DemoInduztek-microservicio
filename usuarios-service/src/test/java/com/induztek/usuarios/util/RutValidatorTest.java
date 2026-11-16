package com.induztek.usuarios.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RutValidatorTest {

    @Test
    void normalizar_quitaPuntosYDejaGuion() {
        assertEquals("14226791-8", RutValidator.normalizar("14.226.791-8"));
    }

    @Test
    void normalizar_pasaLaKAMayuscula() {
        assertEquals("10000013-K", RutValidator.normalizar(" 10000013-k "));
    }

    @Test
    void normalizar_conNuloDevuelveVacio() {
        assertEquals("", RutValidator.normalizar(null));
    }

    @Test
    void normalizar_conUnSoloCaracterNoAgregaGuion() {
        assertEquals("1", RutValidator.normalizar("1"));
    }

    @Test
    void calcularDigito_devuelveDigitoNumerico() {
        assertEquals('8', RutValidator.calcularDigito("14226791"));
    }

    @Test
    void calcularDigito_devuelveKCuandoElRestoEsDiez() {
        assertEquals('K', RutValidator.calcularDigito("10000013"));
    }

    @Test
    void calcularDigito_devuelveCeroCuandoElRestoEsOnce() {
        assertEquals('0', RutValidator.calcularDigito("10000004"));
    }

    @Test
    void esValido_aceptaRutConPuntosYGuion() {
        assertTrue(RutValidator.esValido("14.226.791-8"));
    }

    @Test
    void esValido_aceptaRutSinFormato() {
        assertTrue(RutValidator.esValido("142267918"));
    }

    @Test
    void esValido_aceptaDigitoK() {
        assertTrue(RutValidator.esValido("10000013-K"));
    }

    @Test
    void esValido_rechazaDigitoVerificadorIncorrecto() {
        assertFalse(RutValidator.esValido("14.226.791-9"));
    }

    @Test
    void esValido_rechazaCuerpoMuyCorto() {
        assertFalse(RutValidator.esValido("123456-5"));
    }

    @Test
    void esValido_rechazaKDentroDelCuerpo() {
        assertFalse(RutValidator.esValido("1K000013-8"));
    }

    @Test
    void esValido_rechazaNuloYVacio() {
        assertFalse(RutValidator.esValido(null));
        assertFalse(RutValidator.esValido("   "));
    }
}
