package com.induztek.protocolos.service;

import com.induztek.protocolos.client.UsuariosCliente;
import com.induztek.protocolos.dto.RecepcionRequest;
import com.induztek.protocolos.dto.RecepcionResponse;
import com.induztek.protocolos.dto.UsuarioExterno;
import com.induztek.protocolos.dto.VisitaRequest;
import com.induztek.protocolos.dto.VisitaResponse;
import com.induztek.protocolos.model.RecepcionActa;
import com.induztek.protocolos.model.Visita;
import com.induztek.protocolos.repository.RecepcionActaRepository;
import com.induztek.protocolos.repository.VisitaRepository;
import com.induztek.protocolos.util.Roles;
import com.induztek.protocolos.util.RutValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VisitaService {

    private final VisitaRepository visitaRepository;
    private final RecepcionActaRepository recepcionActaRepository;
    private final ActorService actorService;
    private final UsuariosCliente usuariosCliente;
    private final Clock reloj;

    public List<VisitaResponse> listar(String buscar, Long instalacionId, Long ingenieroId, Boolean cerrada) {
        String texto = buscar == null ? "" : buscar.trim();
        List<Visita> base = texto.isEmpty()
                ? visitaRepository.findAllByOrderByFechaDescIdDesc()
                : visitaRepository.findByNumeroOtContainingIgnoreCaseOrderByFechaDescIdDesc(texto);
        return base.stream()
                .filter(visita -> instalacionId == null || visita.getInstalacionId().equals(instalacionId))
                .filter(visita -> ingenieroId == null || visita.getIngenieroId().equals(ingenieroId))
                .filter(visita -> cerrada == null || cerrada == (visita.getFechaHoraCierre() != null))
                .map(this::aRespuesta)
                .toList();
    }

    public VisitaResponse obtener(Long id) {
        return aRespuesta(buscar(id));
    }

    public VisitaResponse crear(Long actorId, VisitaRequest datos) {
        actorService.exigirPersonalActivo(actorId);
        if (datos.numeroOt() == null || datos.numeroOt().isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "El número de OT es obligatorio");
        }
        if (datos.instalacionId() == null) {
            throw error(HttpStatus.BAD_REQUEST, "Debes indicar la instalación");
        }
        if (datos.fecha() == null) {
            throw error(HttpStatus.BAD_REQUEST, "Debes indicar la fecha de la visita");
        }
        if (datos.tipoVisita() == null) {
            throw error(HttpStatus.BAD_REQUEST, "Debes indicar el tipo de visita");
        }
        validarIngeniero(datos.ingenieroId());
        Set<Long> participantes = validarParticipantes(datos.participantesIds());

        String numeroOt = datos.numeroOt().trim().toUpperCase();
        if (visitaRepository.existsByNumeroOt(numeroOt)) {
            throw error(HttpStatus.CONFLICT, "Ya existe una visita con ese número de OT");
        }
        Visita nueva = Visita.builder()
                .numeroOt(numeroOt)
                .instalacionId(datos.instalacionId())
                .fecha(datos.fecha())
                .tipoVisita(datos.tipoVisita())
                .ingenieroId(datos.ingenieroId())
                .participantesIds(participantes)
                .build();
        return aRespuesta(visitaRepository.save(nueva));
    }

    public VisitaResponse registrarRecepcion(Long id, RecepcionRequest datos) {
        Visita visita = buscar(id);
        if (visita.getFechaHoraCierre() == null) {
            throw error(HttpStatus.CONFLICT, "La visita aún no está cerrada");
        }
        if (recepcionActaRepository.existsById(id)) {
            throw error(HttpStatus.CONFLICT, "El acta de esta visita ya fue recibida");
        }
        if (datos.receptorNombre() == null || datos.receptorNombre().isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "El nombre de quien recibe es obligatorio");
        }
        if (datos.receptorCargo() == null || datos.receptorCargo().isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "El cargo de quien recibe es obligatorio");
        }
        if (!RutValidator.esValido(datos.receptorRut())) {
            throw error(HttpStatus.BAD_REQUEST, "RUT inválido (revisa el dígito verificador)");
        }
        recepcionActaRepository.save(RecepcionActa.builder()
                .visitaId(id)
                .receptorNombre(datos.receptorNombre().trim())
                .receptorRut(RutValidator.normalizar(datos.receptorRut()))
                .receptorCargo(datos.receptorCargo().trim())
                .fechaHoraRecepcion(LocalDateTime.now(reloj))
                .build());
        return aRespuesta(visita);
    }

    private void validarIngeniero(Long ingenieroId) {
        if (ingenieroId == null) {
            throw error(HttpStatus.BAD_REQUEST, "Toda visita debe tener un ingeniero a cargo");
        }
        UsuarioExterno ingeniero = usuariosCliente.buscar(ingenieroId)
                .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "El ingeniero indicado no existe"));
        if (!ingeniero.activo() || !Roles.INGENIERO.equals(ingeniero.rolCodigo())) {
            throw error(HttpStatus.BAD_REQUEST, "El responsable de la visita debe ser un Ingeniero activo");
        }
    }

    private Set<Long> validarParticipantes(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw error(HttpStatus.BAD_REQUEST, "La visita necesita al menos un técnico participante");
        }
        Set<Long> participantes = new LinkedHashSet<>(ids);
        for (Long id : participantes) {
            UsuarioExterno tecnico = usuariosCliente.buscar(id)
                    .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "El participante " + id + " no existe"));
            boolean personalTerreno = Roles.TECNICO.equals(tecnico.rolCodigo()) || Roles.INGENIERO.equals(tecnico.rolCodigo());
            if (!tecnico.activo() || !personalTerreno) {
                throw error(HttpStatus.BAD_REQUEST, "Los participantes deben ser Técnicos o Ingenieros activos");
            }
        }
        return participantes;
    }

    private Visita buscar(Long id) {
        return visitaRepository.findById(id)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Visita no encontrada"));
    }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }

    private VisitaResponse aRespuesta(Visita visita) {
        RecepcionResponse recepcion = recepcionActaRepository.findById(visita.getId())
                .map(r -> new RecepcionResponse(r.getReceptorNombre(), r.getReceptorRut(),
                        r.getReceptorCargo(), r.getFechaHoraRecepcion()))
                .orElse(null);
        return new VisitaResponse(
                visita.getId(),
                visita.getNumeroOt(),
                visita.getInstalacionId(),
                visita.getFecha(),
                visita.getTipoVisita(),
                visita.getIngenieroId(),
                visita.getParticipantesIds().stream().sorted().toList(),
                visita.getFechaHoraCierre(),
                visita.getFechaHoraCierre() != null,
                recepcion
        );
    }
}
