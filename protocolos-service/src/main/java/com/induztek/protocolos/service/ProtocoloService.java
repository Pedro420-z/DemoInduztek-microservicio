package com.induztek.protocolos.service;

import com.induztek.protocolos.client.EquiposCliente;
import com.induztek.protocolos.dto.EquipoExterno;
import com.induztek.protocolos.dto.MotivoRequest;
import com.induztek.protocolos.dto.ProtocoloActualizacionRequest;
import com.induztek.protocolos.dto.ProtocoloRequest;
import com.induztek.protocolos.dto.ProtocoloResponse;
import com.induztek.protocolos.dto.UsuarioExterno;
import com.induztek.protocolos.model.EstadoProtocolo;
import com.induztek.protocolos.model.HistorialEstado;
import com.induztek.protocolos.model.Protocolo;
import com.induztek.protocolos.model.Prueba;
import com.induztek.protocolos.model.TipoEquipoPrueba;
import com.induztek.protocolos.model.TipoVisita;
import com.induztek.protocolos.model.Visita;
import com.induztek.protocolos.repository.HistorialEstadoRepository;
import com.induztek.protocolos.repository.ProtocoloRepository;
import com.induztek.protocolos.repository.PruebaRepository;
import com.induztek.protocolos.repository.TipoEquipoPruebaRepository;
import com.induztek.protocolos.repository.VisitaRepository;
import com.induztek.protocolos.util.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProtocoloService {

    private static final String SOLO_REVISORES = "Solo el Ingeniero o el Jefe técnico puede realizar esta acción";
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final ProtocoloRepository protocoloRepository;
    private final PruebaRepository pruebaRepository;
    private final HistorialEstadoRepository historialRepository;
    private final VisitaRepository visitaRepository;
    private final TipoEquipoPruebaRepository tipoEquipoPruebaRepository;
    private final ActorService actorService;
    private final EquiposCliente equiposCliente;
    private final ProtocoloPermisos permisos;
    private final ProtocoloMapper mapper;
    private final Clock reloj;

    public List<ProtocoloResponse> listar(String buscar, Long visitaId, Long equipoId,
                                          EstadoProtocolo estado, Boolean soloAtrasados) {
        String texto = buscar == null ? "" : buscar.trim();
        List<Protocolo> base = texto.isEmpty()
                ? protocoloRepository.findAllByOrderByIdDesc()
                : protocoloRepository.findByNumeroContainingIgnoreCaseOrderByIdDesc(texto);
        return base.stream()
                .filter(p -> visitaId == null || p.getVisita().getId().equals(visitaId))
                .filter(p -> equipoId == null || p.getEquipoId().equals(equipoId))
                .filter(p -> estado == null || p.getEstado() == estado)
                .map(mapper::aProtocolo)
                .filter(r -> !Boolean.TRUE.equals(soloAtrasados) || r.atrasado())
                .toList();
    }

    public ProtocoloResponse obtener(Long id) {
        return mapper.aProtocolo(buscar(id));
    }

    public ProtocoloResponse crear(Long actorId, ProtocoloRequest datos) {
        UsuarioExterno actor = actorService.exigirPersonalActivo(actorId);
        if (datos.visitaId() == null) {
            throw error(HttpStatus.BAD_REQUEST, "Debes indicar la visita");
        }
        if (datos.equipoId() == null) {
            throw error(HttpStatus.BAD_REQUEST, "Debes indicar el equipo");
        }
        Visita visita = visitaRepository.findById(datos.visitaId())
                .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "La visita indicada no existe"));
        if (visita.getFechaHoraCierre() != null) {
            throw error(HttpStatus.CONFLICT, "La visita ya está cerrada");
        }
        EquipoExterno equipo = equiposCliente.buscar(datos.equipoId())
                .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "El equipo indicado no existe"));
        if (!equipo.instalacionId().equals(visita.getInstalacionId())) {
            throw error(HttpStatus.BAD_REQUEST, "El equipo no pertenece a la instalación de la visita");
        }
        if (protocoloRepository.existsByVisitaIdAndEquipoId(visita.getId(), equipo.id())) {
            throw error(HttpStatus.CONFLICT, "Ya existe un protocolo de ese equipo en la visita");
        }
        validarAmbiente(datos.humedadPct());
        String mantenimiento = validarMantenimiento(visita, datos.descripcionMantenimiento());
        Protocolo reemplazado = buscarReemplazado(datos.protocoloReemplazadoId(), equipo.id());

        LocalDateTime fechaHora = datos.fechaHoraPrueba() != null ? datos.fechaHoraPrueba() : LocalDateTime.now(reloj);
        Protocolo nuevo = Protocolo.builder()
                .numero(generarNumero(fechaHora.getYear()))
                .visita(visita)
                .equipoId(equipo.id())
                .usuarioRegistraId(actor.id())
                .fechaHoraPrueba(fechaHora)
                .temperaturaC(datos.temperaturaC())
                .humedadPct(datos.humedadPct())
                .descripcionMantenimiento(mantenimiento)
                .observaciones(limpiar(datos.observaciones()))
                .estado(EstadoProtocolo.BORRADOR)
                .protocoloReemplazado(reemplazado)
                .build();
        Protocolo guardado = protocoloRepository.save(nuevo);
        registrarHistorial(guardado, null, EstadoProtocolo.BORRADOR, actor.id(), null);
        return mapper.aProtocolo(guardado);
    }

    public ProtocoloResponse actualizar(Long actorId, Long id, ProtocoloActualizacionRequest datos) {
        UsuarioExterno actor = actorService.exigirPersonalActivo(actorId);
        Protocolo protocolo = buscar(id);
        permisos.exigirEditable(actor, protocolo);
        validarAmbiente(datos.humedadPct());
        protocolo.setDescripcionMantenimiento(validarMantenimiento(protocolo.getVisita(), datos.descripcionMantenimiento()));
        if (datos.fechaHoraPrueba() != null) {
            protocolo.setFechaHoraPrueba(datos.fechaHoraPrueba());
        }
        protocolo.setTemperaturaC(datos.temperaturaC());
        protocolo.setHumedadPct(datos.humedadPct());
        protocolo.setObservaciones(limpiar(datos.observaciones()));
        return mapper.aProtocolo(protocoloRepository.save(protocolo));
    }

    public ProtocoloResponse registrarEnTerreno(Long actorId, Long id) {
        UsuarioExterno actor = actorService.exigirPersonalActivo(actorId);
        Protocolo protocolo = buscar(id);
        permisos.exigirEditable(actor, protocolo);
        if (protocolo.getTemperaturaC() == null || protocolo.getHumedadPct() == null) {
            throw error(HttpStatus.BAD_REQUEST, "Para registrar el protocolo debes indicar temperatura y humedad");
        }
        EquipoExterno equipo = equiposCliente.buscar(protocolo.getEquipoId())
                .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "El equipo del protocolo ya no existe"));
        Set<Long> registradas = pruebaRepository.findByProtocoloIdOrderByIdAsc(id).stream()
                .map(Prueba::getTipoPrueba)
                .map(tipo -> tipo.getId())
                .collect(Collectors.toSet());
        List<String> faltantes = tipoEquipoPruebaRepository.findByTipoEquipoId(equipo.tipoEquipoId()).stream()
                .map(TipoEquipoPrueba::getTipoPrueba)
                .filter(tipo -> !registradas.contains(tipo.getId()))
                .map(tipo -> tipo.getCodigo())
                .toList();
        if (!faltantes.isEmpty()) {
            throw error(HttpStatus.CONFLICT,
                    "Faltan pruebas por registrar o marcar como no realizadas: " + String.join(", ", faltantes));
        }
        return cambiarEstado(actor, protocolo, EstadoProtocolo.REGISTRADO_TERRENO, null);
    }

    public ProtocoloResponse observar(Long actorId, Long id, MotivoRequest datos) {
        UsuarioExterno actor = actorService.exigirRol(actorId, SOLO_REVISORES, Roles.INGENIERO, Roles.JEFE_TECNICO);
        Protocolo protocolo = buscar(id);
        exigirEstado(protocolo, EstadoProtocolo.REGISTRADO_TERRENO);
        return cambiarEstado(actor, protocolo, EstadoProtocolo.OBSERVADO, exigirMotivo(datos));
    }

    public ProtocoloResponse formalizar(Long actorId, Long id) {
        UsuarioExterno actor = actorService.exigirRol(actorId, SOLO_REVISORES, Roles.INGENIERO, Roles.JEFE_TECNICO);
        Protocolo protocolo = buscar(id);
        exigirEstado(protocolo, EstadoProtocolo.REGISTRADO_TERRENO);
        return cambiarEstado(actor, protocolo, EstadoProtocolo.FORMALIZADO, null);
    }

    public ProtocoloResponse anular(Long actorId, Long id, MotivoRequest datos) {
        UsuarioExterno actor = actorService.exigirRol(actorId, SOLO_REVISORES, Roles.INGENIERO, Roles.JEFE_TECNICO);
        Protocolo protocolo = buscar(id);
        if (protocolo.getEstado() == EstadoProtocolo.ANULADO) {
            throw error(HttpStatus.CONFLICT, "El protocolo ya está anulado");
        }
        return cambiarEstado(actor, protocolo, EstadoProtocolo.ANULADO, exigirMotivo(datos));
    }

    private ProtocoloResponse cambiarEstado(UsuarioExterno actor, Protocolo protocolo,
                                            EstadoProtocolo nuevo, String motivo) {
        EstadoProtocolo anterior = protocolo.getEstado();
        protocolo.setEstado(nuevo);
        Protocolo guardado = protocoloRepository.save(protocolo);
        registrarHistorial(guardado, anterior, nuevo, actor.id(), motivo);
        return mapper.aProtocolo(guardado);
    }

    private void registrarHistorial(Protocolo protocolo, EstadoProtocolo anterior, EstadoProtocolo nuevo,
                                    Long usuarioId, String motivo) {
        historialRepository.save(HistorialEstado.builder()
                .protocolo(protocolo)
                .estadoAnterior(anterior)
                .estadoNuevo(nuevo)
                .usuarioId(usuarioId)
                .fechaHora(LocalDateTime.now(reloj))
                .motivo(motivo)
                .build());
    }

    private void exigirEstado(Protocolo protocolo, EstadoProtocolo esperado) {
        if (protocolo.getEstado() != esperado) {
            throw error(HttpStatus.CONFLICT,
                    "El protocolo debe estar " + esperado + " y está " + protocolo.getEstado());
        }
    }

    private String exigirMotivo(MotivoRequest datos) {
        if (datos == null || datos.motivo() == null || datos.motivo().isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "El motivo es obligatorio");
        }
        return datos.motivo().trim();
    }

    private void validarAmbiente(BigDecimal humedad) {
        if (humedad != null && (humedad.signum() < 0 || humedad.compareTo(CIEN) > 0)) {
            throw error(HttpStatus.BAD_REQUEST, "La humedad relativa debe estar entre 0 y 100 %");
        }
    }

    private String validarMantenimiento(Visita visita, String descripcion) {
        String limpia = limpiar(descripcion);
        if (limpia != null && visita.getTipoVisita() == TipoVisita.SOLO_PRUEBAS) {
            throw error(HttpStatus.BAD_REQUEST,
                    "Solo las visitas con mantenimiento llevan descripción de mantenimiento");
        }
        return limpia;
    }

    private Protocolo buscarReemplazado(Long id, Long equipoId) {
        if (id == null) {
            return null;
        }
        Protocolo reemplazado = protocoloRepository.findById(id)
                .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "El protocolo a reemplazar no existe"));
        if (reemplazado.getEstado() != EstadoProtocolo.ANULADO) {
            throw error(HttpStatus.BAD_REQUEST, "Solo se puede reemplazar un protocolo anulado");
        }
        if (!reemplazado.getEquipoId().equals(equipoId)) {
            throw error(HttpStatus.BAD_REQUEST, "El protocolo a reemplazar es de otro equipo");
        }
        return reemplazado;
    }

    private String generarNumero(int anio) {
        String prefijo = "PT-" + anio + "-";
        return prefijo + String.format("%04d", protocoloRepository.countByNumeroStartingWith(prefijo) + 1);
    }

    private Protocolo buscar(Long id) {
        return protocoloRepository.findById(id)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Protocolo no encontrado"));
    }

    private String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }
}
