package com.induztek.reportes.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProtocoloExterno(
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
        String estado,
        Long protocoloReemplazadoId,
        String resultadoGlobal,
        boolean atrasado,
        List<PruebaExterno> pruebas,
        List<HistorialExterno> historial
) {
}
