package com.induztek.protocolos.service;

import com.induztek.protocolos.dto.CalibracionRequest;
import com.induztek.protocolos.dto.CalibracionResponse;
import com.induztek.protocolos.dto.InstrumentoRequest;
import com.induztek.protocolos.dto.InstrumentoResponse;
import com.induztek.protocolos.model.Calibracion;
import com.induztek.protocolos.model.Instrumento;
import com.induztek.protocolos.repository.CalibracionRepository;
import com.induztek.protocolos.repository.InstrumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InstrumentoService {

    private final InstrumentoRepository instrumentoRepository;
    private final CalibracionRepository calibracionRepository;
    private final ActorService actorService;
    private final Clock reloj;

    public List<InstrumentoResponse> listar(Boolean soloVigentes) {
        LocalDate hoy = LocalDate.now(reloj);
        return instrumentoRepository.findAllByOrderByNombreAsc().stream()
                .map(instrumento -> aRespuesta(instrumento, hoy))
                .filter(respuesta -> !Boolean.TRUE.equals(soloVigentes) || respuesta.vigente())
                .toList();
    }

    public InstrumentoResponse obtener(Long id) {
        return aRespuesta(buscar(id), LocalDate.now(reloj));
    }

    public InstrumentoResponse crear(Long actorId, InstrumentoRequest datos) {
        actorService.exigirJefeTecnico(actorId);
        validar(datos);
        String serie = datos.numeroSerie().trim();
        if (instrumentoRepository.existsByNumeroSerie(serie)) {
            throw error(HttpStatus.CONFLICT, "Ya existe un instrumento con ese número de serie");
        }
        Instrumento nuevo = Instrumento.builder().numeroSerie(serie).build();
        copiarDatos(nuevo, datos);
        return aRespuesta(instrumentoRepository.save(nuevo), LocalDate.now(reloj));
    }

    public InstrumentoResponse actualizar(Long actorId, Long id, InstrumentoRequest datos) {
        actorService.exigirJefeTecnico(actorId);
        Instrumento instrumento = buscar(id);
        validar(datos);
        String serie = datos.numeroSerie().trim();
        if (instrumentoRepository.existsByNumeroSerieAndIdNot(serie, id)) {
            throw error(HttpStatus.CONFLICT, "Ya existe un instrumento con ese número de serie");
        }
        instrumento.setNumeroSerie(serie);
        copiarDatos(instrumento, datos);
        return aRespuesta(instrumentoRepository.save(instrumento), LocalDate.now(reloj));
    }

    public List<CalibracionResponse> historial(Long id) {
        buscar(id);
        return calibracionRepository.findByInstrumentoIdOrderByFechaVencimientoDesc(id).stream()
                .map(c -> new CalibracionResponse(c.getId(), c.getFechaCalibracion(), c.getFechaVencimiento()))
                .toList();
    }

    public InstrumentoResponse registrarCalibracion(Long actorId, Long id, CalibracionRequest datos) {
        actorService.exigirJefeTecnico(actorId);
        Instrumento instrumento = buscar(id);
        if (datos.fechaCalibracion() == null || datos.fechaVencimiento() == null) {
            throw error(HttpStatus.BAD_REQUEST, "Debes indicar la fecha de calibración y la de vencimiento");
        }
        if (!datos.fechaVencimiento().isAfter(datos.fechaCalibracion())) {
            throw error(HttpStatus.BAD_REQUEST, "El vencimiento debe ser posterior a la fecha de calibración");
        }
        calibracionRepository.save(Calibracion.builder()
                .instrumento(instrumento)
                .fechaCalibracion(datos.fechaCalibracion())
                .fechaVencimiento(datos.fechaVencimiento())
                .build());
        return aRespuesta(instrumento, LocalDate.now(reloj));
    }

    public boolean estaVigente(Long instrumentoId, LocalDate fecha) {
        return calibracionRepository
                .existsByInstrumentoIdAndFechaCalibracionLessThanEqualAndFechaVencimientoGreaterThanEqual(
                        instrumentoId, fecha, fecha);
    }

    private Instrumento buscar(Long id) {
        return instrumentoRepository.findById(id)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Instrumento no encontrado"));
    }

    private void validar(InstrumentoRequest datos) {
        if (datos.nombre() == null || datos.nombre().isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "El nombre del instrumento es obligatorio");
        }
        if (datos.numeroSerie() == null || datos.numeroSerie().isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "El número de serie es obligatorio");
        }
    }

    private void copiarDatos(Instrumento instrumento, InstrumentoRequest datos) {
        instrumento.setNombre(datos.nombre().trim());
        instrumento.setMarca(limpiar(datos.marca()));
        instrumento.setModelo(limpiar(datos.modelo()));
    }

    private String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }

    private InstrumentoResponse aRespuesta(Instrumento instrumento, LocalDate hoy) {
        Optional<Calibracion> ultima = calibracionRepository
                .findFirstByInstrumentoIdOrderByFechaVencimientoDesc(instrumento.getId());
        return new InstrumentoResponse(
                instrumento.getId(),
                instrumento.getNombre(),
                instrumento.getMarca(),
                instrumento.getModelo(),
                instrumento.getNumeroSerie(),
                ultima.map(Calibracion::getFechaCalibracion).orElse(null),
                ultima.map(Calibracion::getFechaVencimiento).orElse(null),
                ultima.isPresent() && !hoy.isAfter(ultima.get().getFechaVencimiento())
        );
    }
}
