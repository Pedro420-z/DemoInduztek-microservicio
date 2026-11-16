package com.induztek.protocolos.dto;

import java.util.List;

public record TipoPruebaResponse(
        Long id,
        String codigo,
        String nombre,
        List<ParametroResponse> parametros
) {
}
