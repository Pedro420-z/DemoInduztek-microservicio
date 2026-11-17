package com.induztek.protocolos.dto;

import com.induztek.protocolos.model.MomentoPrueba;
import com.induztek.protocolos.model.ResultadoPrueba;

import java.util.List;

public record PruebaRequest(
        Long tipoPruebaId,
        String elemento,
        MomentoPrueba momento,
        Boolean realizada,
        String motivoNoRealizada,
        Long instrumentoId,
        Boolean noOpera,
        ResultadoPrueba resultadoManual,
        String comentario,
        List<MedicionRequest> mediciones
) {
}
