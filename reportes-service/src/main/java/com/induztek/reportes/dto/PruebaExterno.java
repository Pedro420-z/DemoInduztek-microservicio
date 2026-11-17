package com.induztek.reportes.dto;

import java.math.BigDecimal;
import java.util.List;

public record PruebaExterno(
        Long id,
        Long tipoPruebaId,
        String tipoPruebaCodigo,
        String tipoPruebaNombre,
        String elemento,
        String momento,
        boolean realizada,
        String motivoNoRealizada,
        Long instrumentoId,
        String resultado,
        BigDecimal valorCalculado,
        String comentario,
        List<MedicionExterno> mediciones
) {
}
