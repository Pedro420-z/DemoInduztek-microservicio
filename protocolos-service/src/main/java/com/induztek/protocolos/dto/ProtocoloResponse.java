package com.induztek.protocolos.dto;

import com.induztek.protocolos.model.EstadoProtocolo;
import com.induztek.protocolos.model.ResultadoPrueba;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProtocoloResponse(
        Long id,
        String numero,
        Long visitaId,
        String visitaNumeroOt,
        Long equipoId,
        Long usuarioRegistraId,
        LocalDateTime fechaHoraPrueba,
        BigDecimal temperaturaC,
        BigDecimal humedadPct,
        String descripcionMantenimiento,
        String observaciones,
        EstadoProtocolo estado,
        Long protocoloReemplazadoId,
        ResultadoPrueba resultadoGlobal,
        boolean atrasado,
        List<PruebaResponse> pruebas,
        List<HistorialResponse> historial
) {
}
