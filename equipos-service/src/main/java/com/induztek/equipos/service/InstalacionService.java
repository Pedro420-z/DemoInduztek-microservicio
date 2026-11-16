package com.induztek.equipos.service;

import com.induztek.equipos.dto.InstalacionRequest;
import com.induztek.equipos.dto.InstalacionResponse;
import com.induztek.equipos.model.Comuna;
import com.induztek.equipos.model.Instalacion;
import com.induztek.equipos.repository.ComunaRepository;
import com.induztek.equipos.repository.InstalacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InstalacionService {

    private final InstalacionRepository instalacionRepository;
    private final ComunaRepository comunaRepository;

    public List<InstalacionResponse> listar(Long clienteId) {
        List<Instalacion> instalaciones = clienteId == null
                ? instalacionRepository.findAllByOrderByNombreAsc()
                : instalacionRepository.findByClienteIdOrderByNombreAsc(clienteId);
        return instalaciones.stream().map(this::aRespuesta).toList();
    }

    public InstalacionResponse obtener(Long id, Long clienteId) {
        return aRespuesta(buscar(id, clienteId));
    }

    public InstalacionResponse crear(InstalacionRequest datos) {
        if (datos.clienteId() == null) {
            throw error(HttpStatus.BAD_REQUEST, "Debes indicar el cliente dueño de la instalación");
        }
        validarNombre(datos.nombre());
        Comuna comuna = buscarComuna(datos.comunaId());
        Instalacion nueva = Instalacion.builder()
                .clienteId(datos.clienteId())
                .nombre(datos.nombre().trim())
                .ubicacion(limpiar(datos.ubicacion()))
                .comuna(comuna)
                .observacionesAcceso(limpiar(datos.observacionesAcceso()))
                .build();
        return aRespuesta(instalacionRepository.save(nueva));
    }

    public InstalacionResponse actualizar(Long id, InstalacionRequest datos) {
        Instalacion instalacion = buscar(id, null);
        validarNombre(datos.nombre());
        instalacion.setNombre(datos.nombre().trim());
        instalacion.setUbicacion(limpiar(datos.ubicacion()));
        instalacion.setComuna(buscarComuna(datos.comunaId()));
        instalacion.setObservacionesAcceso(limpiar(datos.observacionesAcceso()));
        return aRespuesta(instalacionRepository.save(instalacion));
    }

    private Instalacion buscar(Long id, Long clienteId) {
        return instalacionRepository.findById(id)
                .filter(instalacion -> clienteId == null || clienteId.equals(instalacion.getClienteId()))
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Instalación no encontrada"));
    }

    private Comuna buscarComuna(Long comunaId) {
        if (comunaId == null) {
            throw error(HttpStatus.BAD_REQUEST, "Debes indicar la comuna");
        }
        return comunaRepository.findById(comunaId)
                .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "La comuna indicada no existe"));
    }

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "El nombre de la instalación es obligatorio");
        }
    }

    private String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }

    private InstalacionResponse aRespuesta(Instalacion instalacion) {
        Comuna comuna = instalacion.getComuna();
        return new InstalacionResponse(
                instalacion.getId(),
                instalacion.getClienteId(),
                instalacion.getNombre(),
                instalacion.getUbicacion(),
                comuna.getId(),
                comuna.getNombre(),
                comuna.getRegion().getId(),
                comuna.getRegion().getNombre(),
                instalacion.getObservacionesAcceso()
        );
    }
}
