package com.induztek.protocolos.dto;

import com.induztek.protocolos.model.MomentoPrueba;
import com.induztek.protocolos.model.ResultadoPrueba;

import java.math.BigDecimal;
import java.util.List;

public record PruebaResponse(
        Long id,
        Long tipoPruebaId,
        String tipoPruebaCodigo,
        String tipoPruebaNombre,
        String elemento,
        MomentoPrueba momento,
        boolean realizada,
        String motivoNoRealizada,
        Long instrumentoId,
        ResultadoPrueba resultado,
        BigDecimal valorCalculado,
        String comentario,
        List<MedicionResponse> mediciones
) {
}
