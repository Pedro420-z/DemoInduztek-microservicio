package com.induztek.reportes.dto;

import java.time.LocalDateTime;

public record HistorialExterno(
        Long id,
        String estadoAnterior,
        String estadoNuevo,
        Long usuarioId,
        LocalDateTime fechaHora,
        String motivo
) {
}
