package com.induztek.reportes.service;

import com.induztek.reportes.client.EquiposCliente;
import com.induztek.reportes.client.ProtocolosCliente;
import com.induztek.reportes.dto.ActaResponse;
import com.induztek.reportes.dto.EquipoExterno;
import com.induztek.reportes.dto.HistorialEquipoResponse;
import com.induztek.reportes.dto.InstalacionExterno;
import com.induztek.reportes.dto.ProtocoloExterno;
import com.induztek.reportes.dto.ResumenEquipoResponse;
import com.induztek.reportes.dto.UsuarioExterno;
import com.induztek.reportes.dto.VisitaExterno;
import com.induztek.reportes.util.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final String FORMALIZADO = "FORMALIZADO";
    private static final String ANULADO = "ANULADO";

    private final ActorService actorService;
    private final EquiposCliente equiposCliente;
    private final ProtocolosCliente protocolosCliente;

    public HistorialEquipoResponse historialEquipo(Long actorId, Long equipoId) {
        UsuarioExterno actor = actorService.exigirUsuarioActivo(actorId);
        boolean cliente = esCliente(actor);
        EquipoExterno equipo = equiposCliente.obtener(equipoId, cliente ? actor.id() : null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipo no encontrado"));
        List<ProtocoloExterno> protocolos = ordenar(protocolosCliente.protocolos(
                equipo.id(), null, estadoVisible(cliente), null));
        Map<String, Long> porResultado = protocolos.stream()
                .filter(p -> p.resultadoGlobal() != null)
                .collect(Collectors.groupingBy(ProtocoloExterno::resultadoGlobal, TreeMap::new, Collectors.counting()));
        return new HistorialEquipoResponse(equipo, protocolos.size(), porResultado, protocolos);
    }

    public List<ResumenEquipoResponse> equipos(Long actorId, Long clienteId) {
        UsuarioExterno actor = actorService.exigirUsuarioActivo(actorId);
        boolean cliente = esCliente(actor);
        return equiposCliente.listar(cliente ? actor.id() : clienteId).stream()
                .map(equipo -> resumir(equipo, cliente))
                .toList();
    }

    public List<ActaResponse> actas(Long actorId, Long instalacionId) {
        UsuarioExterno actor = actorService.exigirUsuarioActivo(actorId);
        boolean cliente = esCliente(actor);
        List<InstalacionExterno> instalaciones = equiposCliente.instalaciones(cliente ? actor.id() : null).stream()
                .filter(i -> instalacionId == null || i.id().equals(instalacionId))
                .toList();
        if (instalacionId != null && instalaciones.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Instalación no encontrada");
        }
        List<ActaResponse> actas = new ArrayList<>();
        for (InstalacionExterno instalacion : instalaciones) {
            for (VisitaExterno visita : protocolosCliente.visitas(instalacion.id(), true)) {
                actas.add(new ActaResponse(visita, instalacion.nombre(),
                        ordenar(protocolosCliente.protocolos(null, visita.id(), estadoVisible(cliente), null))));
            }
        }
        actas.sort(Comparator.comparing((ActaResponse a) -> a.visita().fecha(),
                Comparator.nullsLast(Comparator.reverseOrder())));
        return actas;
    }

    public List<ProtocoloExterno> atrasados(Long actorId) {
        actorService.exigirPersonalActivo(actorId);
        return ordenar(protocolosCliente.protocolos(null, null, null, true));
    }

    private ResumenEquipoResponse resumir(EquipoExterno equipo, boolean cliente) {
        List<ProtocoloExterno> vigentes = ordenar(protocolosCliente.protocolos(
                equipo.id(), null, estadoVisible(cliente), null)).stream()
                .filter(p -> !ANULADO.equals(p.estado()))
                .toList();
        if (vigentes.isEmpty()) {
            return new ResumenEquipoResponse(equipo, 0, null, null, null);
        }
        ProtocoloExterno ultimo = vigentes.get(0);
        return new ResumenEquipoResponse(equipo, vigentes.size(), ultimo.numero(), ultimo.fechaHoraPrueba(),
                ultimo.resultadoGlobal());
    }

    private List<ProtocoloExterno> ordenar(List<ProtocoloExterno> protocolos) {
        return protocolos.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ProtocoloExterno::fechaHoraPrueba,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private String estadoVisible(boolean cliente) {
        return cliente ? FORMALIZADO : null;
    }

    private boolean esCliente(UsuarioExterno actor) {
        return Roles.CLIENTE.equals(actor.rolCodigo());
    }
}
