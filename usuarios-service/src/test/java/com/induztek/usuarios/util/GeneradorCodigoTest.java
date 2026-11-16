package com.induztek.usuarios.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorCodigoTest {

    @Test
    void generar_devuelveSiempreSeisDigitos() {
        GeneradorCodigo generador = new GeneradorCodigo();
        for (int i = 0; i < 100; i++) {
            assertTrue(generador.generar().matches("\\d{6}"));
        }
    }
}
