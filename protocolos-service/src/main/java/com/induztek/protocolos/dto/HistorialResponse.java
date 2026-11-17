package com.induztek.protocolos.dto;

import com.induztek.protocolos.model.EstadoProtocolo;

import java.time.LocalDateTime;

public record HistorialResponse(
        Long id,
        EstadoProtocolo estadoAnterior,
        EstadoProtocolo estadoNuevo,
        Long usuarioId,
        LocalDateTime fechaHora,
        String motivo
) {
}
