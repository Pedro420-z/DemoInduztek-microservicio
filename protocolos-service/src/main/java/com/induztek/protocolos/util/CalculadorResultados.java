package com.induztek.protocolos.util;

import com.induztek.protocolos.model.ResultadoPrueba;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

public final class CalculadorResultados {

    public static final String AISLACION = "AISLACION";
    public static final String TTR = "TTR";
    public static final String DEVANADOS = "DEVANADOS";
    public static final String CONTACTOS = "CONTACTOS";
    public static final String OPERACION_RELE = "OPERACION_RELE";

    private static final int ESCALA = 4;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal MINIMO_AISLACION_MOHM = BigDecimal.valueOf(100);
    private static final BigDecimal IP_APROBADO = BigDecimal.valueOf(2);

    private CalculadorResultados() {
    }

    public record Calculo(BigDecimal valor, ResultadoPrueba resultado) {
    }

    public static Calculo calcular(String tipoPrueba, Map<String, BigDecimal> valores,
                                   BigDecimal maximoContactos, boolean noOpera) {
        return switch (tipoPrueba) {
            case AISLACION -> aislacion(valores);
            case TTR -> transformacion(valores);
            case CONTACTOS -> contactos(valores, maximoContactos);
            case OPERACION_RELE -> rele(valores, noOpera);
            default -> throw new IllegalArgumentException("La prueba " + tipoPrueba + " no tiene cálculo automático");
        };
    }

    public static ResultadoPrueba peor(Collection<ResultadoPrueba> resultados) {
        return resultados.stream().max(Comparator.naturalOrder()).orElse(null);
    }

    public static LocalDate sumarDiasHabiles(LocalDate desde, int dias) {
        LocalDate fecha = desde;
        int restantes = dias;
        while (restantes > 0) {
            fecha = fecha.plusDays(1);
            if (fecha.getDayOfWeek() != DayOfWeek.SATURDAY && fecha.getDayOfWeek() != DayOfWeek.SUNDAY) {
                restantes--;
            }
        }
        return fecha;
    }

    private static Calculo aislacion(Map<String, BigDecimal> valores) {
        BigDecimal lectura1 = valores.get("LECTURA_1MIN");
        BigDecimal lectura10 = valores.get("LECTURA_10MIN");
        boolean aislacionBaja = lectura1.compareTo(MINIMO_AISLACION_MOHM) < 0;
        if (lectura10 == null) {
            return new Calculo(null, aislacionBaja ? ResultadoPrueba.RECHAZADO : ResultadoPrueba.APROBADO);
        }
        BigDecimal indice = lectura10.divide(lectura1, ESCALA, RoundingMode.HALF_UP);
        ResultadoPrueba resultado;
        if (aislacionBaja || indice.compareTo(BigDecimal.ONE) < 0) {
            resultado = ResultadoPrueba.RECHAZADO;
        } else if (indice.compareTo(IP_APROBADO) < 0) {
            resultado = ResultadoPrueba.OBSERVADO;
        } else {
            resultado = ResultadoPrueba.APROBADO;
        }
        return new Calculo(indice, resultado);
    }

    private static Calculo transformacion(Map<String, BigDecimal> valores) {
        BigDecimal teorica = valores.get("RELACION_TEORICA");
        BigDecimal medida = valores.get("RELACION_MEDIDA");
        BigDecimal error = porcentajeDeDiferencia(medida, teorica);
        return new Calculo(error, clasificar(error, "0.5", "1.0"));
    }

    private static Calculo contactos(Map<String, BigDecimal> valores, BigDecimal maximo) {
        BigDecimal porcentaje = valores.get("RESISTENCIA_CONTACTO").multiply(CIEN)
                .divide(maximo, ESCALA, RoundingMode.HALF_UP);
        return new Calculo(porcentaje, clasificar(porcentaje, "100", "120"));
    }

    private static Calculo rele(Map<String, BigDecimal> valores, boolean noOpera) {
        if (noOpera) {
            return new Calculo(null, ResultadoPrueba.RECHAZADO);
        }
        BigDecimal diferencia = porcentajeDeDiferencia(valores.get("TIEMPO_MEDIDO"), valores.get("TIEMPO_ESPERADO"));
        return new Calculo(diferencia, clasificar(diferencia, "5", "10"));
    }

    private static BigDecimal porcentajeDeDiferencia(BigDecimal medido, BigDecimal esperado) {
        return medido.subtract(esperado).abs().multiply(CIEN).divide(esperado, ESCALA, RoundingMode.HALF_UP);
    }

    private static ResultadoPrueba clasificar(BigDecimal valor, String aprobadoHasta, String observadoHasta) {
        if (valor.compareTo(new BigDecimal(aprobadoHasta)) <= 0) {
            return ResultadoPrueba.APROBADO;
        }
        if (valor.compareTo(new BigDecimal(observadoHasta)) <= 0) {
            return ResultadoPrueba.OBSERVADO;
        }
        return ResultadoPrueba.RECHAZADO;
    }
}
