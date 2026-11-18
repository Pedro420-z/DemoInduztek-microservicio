package com.induztek.protocolos.dto;

import com.induztek.protocolos.model.EstadoReporte;

import java.time.LocalDateTime;

public record ReporteActaResponse(
        Long id,
        Long visitaId,
        String numeroOt,
        Long protocoloId,
        String protocoloNumero,
        Long clienteId,
        String texto,
        EstadoReporte estado,
        String respuesta,
        Long respondidoPorId,
        LocalDateTime fechaHoraCreacion,
        LocalDateTime fechaHoraActualizacion
) {
}
