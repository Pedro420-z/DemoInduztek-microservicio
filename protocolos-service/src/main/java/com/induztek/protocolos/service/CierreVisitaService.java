package com.induztek.protocolos.service;

import com.induztek.protocolos.dto.VisitaResponse;
import com.induztek.protocolos.model.EstadoProtocolo;
import com.induztek.protocolos.model.Protocolo;
import com.induztek.protocolos.model.Visita;
import com.induztek.protocolos.repository.ProtocoloRepository;
import com.induztek.protocolos.repository.VisitaRepository;
import com.induztek.protocolos.util.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CierreVisitaService {

    private final VisitaRepository visitaRepository;
    private final ProtocoloRepository protocoloRepository;
    private final VisitaService visitaService;
    private final ActorService actorService;
    private final Clock reloj;

    public VisitaResponse cerrar(Long actorId, Long visitaId) {
        actorService.exigirRol(actorId, "Solo el Técnico o el Ingeniero puede cerrar la visita",
                Roles.TECNICO, Roles.INGENIERO);
        Visita visita = visitaRepository.findById(visitaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visita no encontrada"));
        if (visita.getFechaHoraCierre() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La visita ya está cerrada");
        }
        List<Protocolo> protocolos = protocoloRepository.findByVisitaIdOrderByIdAsc(visitaId);
        List<String> pendientes = protocolos.stream()
                .filter(p -> p.getEstado() == EstadoProtocolo.BORRADOR || p.getEstado() == EstadoProtocolo.OBSERVADO)
                .map(Protocolo::getNumero)
                .toList();
        if (!pendientes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede cerrar la visita: faltan por registrar en terreno " + String.join(", ", pendientes));
        }
        if (protocolos.stream().allMatch(p -> p.getEstado() == EstadoProtocolo.ANULADO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La visita no tiene protocolos registrados en terreno");
        }
        visita.setFechaHoraCierre(LocalDateTime.now(reloj));
        visitaRepository.save(visita);
        return visitaService.obtener(visitaId);
    }
}
