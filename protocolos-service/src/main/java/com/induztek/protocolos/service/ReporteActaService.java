package com.induztek.protocolos.service;

import com.induztek.protocolos.client.EquiposCliente;
import com.induztek.protocolos.dto.ReporteActaRequest;
import com.induztek.protocolos.dto.ReporteActaResponse;
import com.induztek.protocolos.dto.ResponderReporteRequest;
import com.induztek.protocolos.dto.UsuarioExterno;
import com.induztek.protocolos.model.EstadoProtocolo;
import com.induztek.protocolos.model.EstadoReporte;
import com.induztek.protocolos.model.HistorialEstado;
import com.induztek.protocolos.model.Protocolo;
import com.induztek.protocolos.model.ReporteActa;
import com.induztek.protocolos.model.Visita;
import com.induztek.protocolos.repository.HistorialEstadoRepository;
import com.induztek.protocolos.repository.ProtocoloRepository;
import com.induztek.protocolos.repository.ReporteActaRepository;
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
public class ReporteActaService {

    private static final String SOLO_REVISORES = "Solo el Ingeniero o el Jefe técnico puede responder un reporte";

    private final ReporteActaRepository reporteRepository;
    private final VisitaRepository visitaRepository;
    private final ProtocoloRepository protocoloRepository;
    private final HistorialEstadoRepository historialRepository;
    private final ActorService actorService;
    private final EquiposCliente equiposCliente;
    private final Clock reloj;

    public List<ReporteActaResponse> listar(Long actorId, Long visitaId, EstadoReporte estado) {
        UsuarioExterno actor = actorService.exigirUsuarioActivo(actorId);
        List<ReporteActa> base = Roles.CLIENTE.equals(actor.rolCodigo())
                ? reporteRepository.findByClienteIdOrderByIdDesc(actor.id())
                : reporteRepository.findAllByOrderByIdDesc();
        return base.stream()
                .filter(r -> visitaId == null || r.getVisita().getId().equals(visitaId))
                .filter(r -> estado == null || r.getEstado() == estado)
                .map(this::aRespuesta)
                .toList();
    }

    public ReporteActaResponse obtener(Long actorId, Long id) {
        UsuarioExterno actor = actorService.exigirUsuarioActivo(actorId);
        ReporteActa reporte = buscar(id);
        if (Roles.CLIENTE.equals(actor.rolCodigo()) && !reporte.getClienteId().equals(actor.id())) {
            throw error(HttpStatus.NOT_FOUND, "Reporte no encontrado");
        }
        return aRespuesta(reporte);
    }

    public ReporteActaResponse crear(Long actorId, Long visitaId, ReporteActaRequest datos) {
        UsuarioExterno actor = actorService.exigirUsuarioActivo(actorId);
        if (!Roles.CLIENTE.equals(actor.rolCodigo())) {
            throw error(HttpStatus.FORBIDDEN, "Solo un cliente puede reportar un acta");
        }
        Visita visita = visitaRepository.findById(visitaId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Acta no encontrada"));
        equiposCliente.buscarInstalacion(visita.getInstalacionId())
                .filter(instalacion -> actor.id().equals(instalacion.clienteId()))
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Acta no encontrada"));
        if (visita.getFechaHoraCierre() == null) {
            throw error(HttpStatus.CONFLICT, "El acta aún no está cerrada");
        }
        if (datos == null || datos.texto() == null || datos.texto().isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "Debes describir el reporte");
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        String texto = datos.texto().trim();
        Protocolo protocolo = null;
        if (datos.protocoloId() != null) {
            protocolo = protocoloRepository.findById(datos.protocoloId())
                    .filter(p -> p.getVisita().getId().equals(visitaId))
                    .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "El protocolo no pertenece a esa acta"));
            if (protocolo.getEstado() == EstadoProtocolo.ANULADO) {
                throw error(HttpStatus.CONFLICT, "El protocolo está anulado y no se puede observar");
            }
            if (protocolo.getEstado() == EstadoProtocolo.FORMALIZADO) {
                reabrir(protocolo, actor.id(), texto, ahora);
            }
        }
        ReporteActa nuevo = ReporteActa.builder()
                .visita(visita)
                .protocolo(protocolo)
                .clienteId(actor.id())
                .texto(texto)
                .estado(EstadoReporte.ABIERTO)
                .fechaHoraCreacion(ahora)
                .fechaHoraActualizacion(ahora)
                .build();
        return aRespuesta(reporteRepository.save(nuevo));
    }

    public ReporteActaResponse responder(Long actorId, Long id, ResponderReporteRequest datos) {
        UsuarioExterno actor = actorService.exigirRol(actorId, SOLO_REVISORES, Roles.INGENIERO, Roles.JEFE_TECNICO);
        ReporteActa reporte = buscar(id);
        if (reporte.getEstado() == EstadoReporte.RESUELTO || reporte.getEstado() == EstadoReporte.RECHAZADO) {
            throw error(HttpStatus.CONFLICT, "El reporte ya fue cerrado como " + reporte.getEstado());
        }
        if (datos == null || datos.estado() == null || datos.estado() == EstadoReporte.ABIERTO) {
            throw error(HttpStatus.BAD_REQUEST, "El nuevo estado debe ser EN_REVISION, RESUELTO o RECHAZADO");
        }
        String respuesta = datos.respuesta() == null || datos.respuesta().isBlank() ? null : datos.respuesta().trim();
        if (datos.estado() != EstadoReporte.EN_REVISION && respuesta == null) {
            throw error(HttpStatus.BAD_REQUEST, "Para cerrar el reporte debes escribir una respuesta");
        }
        reporte.setEstado(datos.estado());
        if (respuesta != null) {
            reporte.setRespuesta(respuesta);
        }
        reporte.setRespondidoPorId(actor.id());
        reporte.setFechaHoraActualizacion(LocalDateTime.now(reloj));
        return aRespuesta(reporteRepository.save(reporte));
    }

    private void reabrir(Protocolo protocolo, Long clienteId, String texto, LocalDateTime ahora) {
        protocolo.setEstado(EstadoProtocolo.OBSERVADO);
        protocoloRepository.save(protocolo);
        historialRepository.save(HistorialEstado.builder()
                .protocolo(protocolo)
                .estadoAnterior(EstadoProtocolo.FORMALIZADO)
                .estadoNuevo(EstadoProtocolo.OBSERVADO)
                .usuarioId(clienteId)
                .fechaHora(ahora)
                .motivo("Reporte del cliente: " + texto)
                .build());
    }

    private ReporteActa buscar(Long id) {
        return reporteRepository.findById(id)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Reporte no encontrado"));
    }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }

    private ReporteActaResponse aRespuesta(ReporteActa r) {
        Protocolo protocolo = r.getProtocolo();
        return new ReporteActaResponse(
                r.getId(),
                r.getVisita().getId(),
                r.getVisita().getNumeroOt(),
                protocolo == null ? null : protocolo.getId(),
                protocolo == null ? null : protocolo.getNumero(),
                r.getClienteId(),
                r.getTexto(),
                r.getEstado(),
                r.getRespuesta(),
                r.getRespondidoPorId(),
                r.getFechaHoraCreacion(),
                r.getFechaHoraActualizacion());
    }
}
