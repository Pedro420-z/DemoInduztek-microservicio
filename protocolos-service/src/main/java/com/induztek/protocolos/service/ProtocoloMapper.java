package com.induztek.protocolos.service;

import com.induztek.protocolos.dto.HistorialResponse;
import com.induztek.protocolos.dto.MedicionResponse;
import com.induztek.protocolos.dto.ProtocoloResponse;
import com.induztek.protocolos.dto.PruebaResponse;
import com.induztek.protocolos.model.EstadoProtocolo;
import com.induztek.protocolos.model.HistorialEstado;
import com.induztek.protocolos.model.Protocolo;
import com.induztek.protocolos.model.Prueba;
import com.induztek.protocolos.model.ResultadoPrueba;
import com.induztek.protocolos.repository.HistorialEstadoRepository;
import com.induztek.protocolos.repository.PruebaRepository;
import com.induztek.protocolos.util.CalculadorResultados;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ProtocoloMapper {

    private static final int DIAS_HABILES_PARA_FORMALIZAR = 2;

    private final PruebaRepository pruebaRepository;
    private final HistorialEstadoRepository historialRepository;
    private final Clock reloj;

    public ProtocoloResponse aProtocolo(Protocolo protocolo) {
        List<Prueba> pruebas = pruebaRepository.findByProtocoloIdOrderByIdAsc(protocolo.getId());
        List<HistorialEstado> historial = historialRepository.findByProtocoloIdOrderByIdAsc(protocolo.getId());
        ResultadoPrueba global = CalculadorResultados.peor(pruebas.stream()
                .filter(Prueba::isRealizada)
                .map(Prueba::getResultado)
                .filter(Objects::nonNull)
                .toList());
        Protocolo reemplazado = protocolo.getProtocoloReemplazado();
        return new ProtocoloResponse(
                protocolo.getId(),
                protocolo.getNumero(),
                protocolo.getVisita().getId(),
                protocolo.getVisita().getNumeroOt(),
                protocolo.getEquipoId(),
                protocolo.getUsuarioRegistraId(),
                protocolo.getFechaHoraPrueba(),
                protocolo.getTemperaturaC(),
                protocolo.getHumedadPct(),
                protocolo.getDescripcionMantenimiento(),
                protocolo.getObservaciones(),
                protocolo.getEstado(),
                reemplazado == null ? null : reemplazado.getId(),
                global,
                estaAtrasado(protocolo, historial),
                pruebas.stream().map(this::aPrueba).toList(),
                historial.stream().map(this::aHistorial).toList()
        );
    }

    public PruebaResponse aPrueba(Prueba prueba) {
        return new PruebaResponse(
                prueba.getId(),
                prueba.getTipoPrueba().getId(),
                prueba.getTipoPrueba().getCodigo(),
                prueba.getTipoPrueba().getNombre(),
                prueba.getElemento(),
                prueba.getMomento(),
                prueba.isRealizada(),
                prueba.getMotivoNoRealizada(),
                prueba.getInstrumento() == null ? null : prueba.getInstrumento().getId(),
                prueba.getResultado(),
                prueba.getValorCalculado(),
                prueba.getComentario(),
                prueba.getMediciones().stream()
                        .map(m -> new MedicionResponse(m.getParametro().getCodigo(), m.getParametro().getNombre(),
                                m.getParametro().getUnidad(), m.getValor()))
                        .toList()
        );
    }

    private HistorialResponse aHistorial(HistorialEstado h) {
        return new HistorialResponse(h.getId(), h.getEstadoAnterior(), h.getEstadoNuevo(), h.getUsuarioId(),
                h.getFechaHora(), h.getMotivo());
    }

    private boolean estaAtrasado(Protocolo protocolo, List<HistorialEstado> historial) {
        if (protocolo.getEstado() == EstadoProtocolo.ANULADO) {
            return false;
        }
        LocalDate limite = CalculadorResultados.sumarDiasHabiles(
                protocolo.getFechaHoraPrueba().toLocalDate(), DIAS_HABILES_PARA_FORMALIZAR);
        if (protocolo.getEstado() == EstadoProtocolo.FORMALIZADO) {
            return historial.stream()
                    .filter(h -> h.getEstadoNuevo() == EstadoProtocolo.FORMALIZADO)
                    .anyMatch(h -> h.getFechaHora().toLocalDate().isAfter(limite));
        }
        return LocalDate.now(reloj).isAfter(limite);
    }
}
