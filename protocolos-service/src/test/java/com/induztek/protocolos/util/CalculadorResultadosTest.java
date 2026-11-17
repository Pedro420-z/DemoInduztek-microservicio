package com.induztek.protocolos.util;

import com.induztek.protocolos.model.ResultadoPrueba;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculadorResultadosTest {

    private static BigDecimal n(String valor) {
        return new BigDecimal(valor);
    }

    private static CalculadorResultados.Calculo aislacion(String un, String diez) {
        Map<String, BigDecimal> valores = diez == null
                ? Map.of("LECTURA_1MIN", n(un))
                : Map.of("LECTURA_1MIN", n(un), "LECTURA_10MIN", n(diez));
        return CalculadorResultados.calcular(CalculadorResultados.AISLACION, valores, null, false);
    }

    @Test
    void aislacion_conIndiceDeDosOMas_aprueba() {
        CalculadorResultados.Calculo calculo = aislacion("500", "1000");

        assertEquals(0, n("2").compareTo(calculo.valor()));
        assertEquals(ResultadoPrueba.APROBADO, calculo.resultado());
    }

    @Test
    void aislacion_conIndiceIntermedio_observa() {
        assertEquals(ResultadoPrueba.OBSERVADO, aislacion("500", "750").resultado());
    }

    @Test
    void aislacion_conIndiceMenorAUno_rechaza() {
        assertEquals(ResultadoPrueba.RECHAZADO, aislacion("500", "400").resultado());
    }

    @Test
    void aislacion_conLecturaDeUnMinutoBajoElMinimo_rechazaAunqueElIndiceSeaBueno() {
        assertEquals(ResultadoPrueba.RECHAZADO, aislacion("50", "200").resultado());
    }

    @Test
    void aislacion_sinLecturaDeDiezMinutos_juzgaSoloLaLecturaDeUnMinuto() {
        CalculadorResultados.Calculo bueno = aislacion("500", null);
        CalculadorResultados.Calculo malo = aislacion("99", null);

        assertNull(bueno.valor());
        assertEquals(ResultadoPrueba.APROBADO, bueno.resultado());
        assertEquals(ResultadoPrueba.RECHAZADO, malo.resultado());
    }

    private static CalculadorResultados.Calculo ttr(String teorica, String medida) {
        return CalculadorResultados.calcular(CalculadorResultados.TTR,
                Map.of("RELACION_TEORICA", n(teorica), "RELACION_MEDIDA", n(medida)), null, false);
    }

    @Test
    void ttr_clasificaSegunElErrorPorcentual() {
        assertEquals(ResultadoPrueba.APROBADO, ttr("100", "100.5").resultado());
        assertEquals(ResultadoPrueba.OBSERVADO, ttr("100", "99.2").resultado());
        assertEquals(ResultadoPrueba.OBSERVADO, ttr("100", "101").resultado());
        assertEquals(ResultadoPrueba.RECHAZADO, ttr("100", "101.5").resultado());
        assertEquals(0, n("0.5").compareTo(ttr("100", "100.5").valor()));
    }

    private static CalculadorResultados.Calculo contactos(String medida, String maximo) {
        return CalculadorResultados.calcular(CalculadorResultados.CONTACTOS,
                Map.of("RESISTENCIA_CONTACTO", n(medida)), n(maximo), false);
    }

    @Test
    void contactos_clasificaSegunElPorcentajeDelMaximoDePlaca() {
        assertEquals(ResultadoPrueba.APROBADO, contactos("100", "100").resultado());
        assertEquals(ResultadoPrueba.OBSERVADO, contactos("110", "100").resultado());
        assertEquals(ResultadoPrueba.RECHAZADO, contactos("121", "100").resultado());
        assertEquals(0, n("110").compareTo(contactos("110", "100").valor()));
    }

    private static CalculadorResultados.Calculo rele(String esperado, String medido, boolean noOpera) {
        return CalculadorResultados.calcular(CalculadorResultados.OPERACION_RELE,
                Map.of("TIEMPO_ESPERADO", n(esperado), "TIEMPO_MEDIDO", n(medido)), null, noOpera);
    }

    @Test
    void rele_clasificaSegunLaDiferenciaDeTiempo() {
        assertEquals(ResultadoPrueba.APROBADO, rele("100", "104", false).resultado());
        assertEquals(ResultadoPrueba.OBSERVADO, rele("100", "92", false).resultado());
        assertEquals(ResultadoPrueba.RECHAZADO, rele("100", "120", false).resultado());
    }

    @Test
    void rele_queNoOpera_rechazaSinValor() {
        CalculadorResultados.Calculo calculo = rele("100", "100", true);

        assertEquals(ResultadoPrueba.RECHAZADO, calculo.resultado());
        assertNull(calculo.valor());
    }

    @Test
    void calcular_conPruebaSinCalculoAutomatico_lanzaExcepcion() {
        Map<String, BigDecimal> valores = Map.of();

        assertThrows(IllegalArgumentException.class,
                () -> CalculadorResultados.calcular(CalculadorResultados.DEVANADOS, valores, null, false));
    }

    @Test
    void peor_devuelveElResultadoMasGrave() {
        assertEquals(ResultadoPrueba.RECHAZADO, CalculadorResultados.peor(
                List.of(ResultadoPrueba.APROBADO, ResultadoPrueba.RECHAZADO, ResultadoPrueba.OBSERVADO)));
        assertEquals(ResultadoPrueba.OBSERVADO, CalculadorResultados.peor(
                List.of(ResultadoPrueba.APROBADO, ResultadoPrueba.OBSERVADO)));
        assertNull(CalculadorResultados.peor(List.of()));
    }

    @Test
    void sumarDiasHabiles_saltaLosFinesDeSemana() {
        assertEquals(LocalDate.of(2026, 11, 18), CalculadorResultados.sumarDiasHabiles(LocalDate.of(2026, 11, 16), 2));
        assertEquals(LocalDate.of(2026, 11, 17), CalculadorResultados.sumarDiasHabiles(LocalDate.of(2026, 11, 13), 2));
        assertEquals(LocalDate.of(2026, 11, 16), CalculadorResultados.sumarDiasHabiles(LocalDate.of(2026, 11, 16), 0));
    }
}
